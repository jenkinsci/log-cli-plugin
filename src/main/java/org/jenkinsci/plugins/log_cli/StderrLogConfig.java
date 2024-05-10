/*
 * The MIT License
 *
 * Copyright 2023 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.log_cli;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.remoting.VirtualChannel;
import hudson.slaves.ComputerListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.lib.support_log_formatter.SupportLogFormatter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Configures loggers to print to stderr.
 */
@Symbol("stderrLog")
@Extension public final class StderrLogConfig extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger(StderrLogConfig.class.getName());

    public static final class Target extends AbstractDescribableImpl<Target> implements Serializable {

        public final String name;
        public final String level;

        @DataBoundConstructor public Target(String name, String level) {
            this.name = name;
            this.level = level;
            parsedLevel();
        }

        Level parsedLevel() {
            return Level.parse(level);
        }

        @Override public String toString() {
            return name + "@" + level;
        }

        @Extension public static final class DescriptorImpl extends Descriptor<Target> {

            public FormValidation doCheckName(@QueryParameter String value) {
                if (value.isEmpty()) {
                    return FormValidation.warning("Name is mandatory");
                } else {
                    return FormValidation.ok();
                }
            }

            public ListBoxModel doFillLevelItems() {
                return new ListBoxModel(new ListBoxModel.Option("CONFIG"), new ListBoxModel.Option("FINE"), new ListBoxModel.Option("FINER"), new ListBoxModel.Option("FINEST"));
            }

            // Adapted from LogRecorder (restricted):

            public AutoCompletionCandidates doAutoCompleteName(@QueryParameter String value) {
                if (value == null) {
                    return new AutoCompletionCandidates();
                }
                var candidateNames = new LinkedHashSet<>(getAutoCompletionCandidates(Collections.list(LogManager.getLogManager().getLoggerNames())));
                for (String part : value.split("[ ]+")) {
                    var partCandidates = new HashSet<String>();
                    String lowercaseValue = part.toLowerCase(Locale.ENGLISH);
                    for (String loggerName : candidateNames) {
                        if (loggerName.toLowerCase(Locale.ENGLISH).contains(lowercaseValue)) {
                            partCandidates.add(loggerName);
                        }
                    }
                    candidateNames.retainAll(partCandidates);
                }
                var candidates = new AutoCompletionCandidates();
                candidates.add(candidateNames.toArray(String[]::new));
                return candidates;
            }

            private static Set<String> getAutoCompletionCandidates(List<String> loggerNamesList) {
                var loggerNames = new HashSet<>(loggerNamesList);
                var seenPrefixes = new HashMap<String, Integer>();
                var relevantPrefixes = new TreeSet<String>();
                for (String loggerName : loggerNames) {
                    String[] loggerNameParts = loggerName.split("[.]");
                    String longerPrefix = null;
                    for (int i = loggerNameParts.length; i > 0; i--) {
                        String loggerNamePrefix = String.join(".", Arrays.copyOf(loggerNameParts, i));
                        seenPrefixes.put(loggerNamePrefix, seenPrefixes.getOrDefault(loggerNamePrefix, 0) + 1);
                        if (longerPrefix == null) {
                            relevantPrefixes.add(loggerNamePrefix);
                            longerPrefix = loggerNamePrefix;
                            continue;
                        }
                        if (seenPrefixes.get(loggerNamePrefix) > seenPrefixes.get(longerPrefix)) {
                            relevantPrefixes.add(loggerNamePrefix);
                        }
                        longerPrefix = loggerNamePrefix;
                    }
                }
                return relevantPrefixes;
            }

        }

    }

    private List<Target> targets;

    public StderrLogConfig() {
        load();
    }

    @Override public void load() {
        super.load();
        apply();
    }

    public List<Target> getTargets() {
        return targets;
    }

    @DataBoundSetter public void setTargets(List<Target> targets) {
        this.targets = targets;
        save();
        apply();
    }

    @Override public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        // make sure we can clear all targets
        setTargets(null);
        return super.configure(req, json);
    }

    private void apply() {
        new Register(targets).call(); // inside controller
        for (Computer c : Jenkins.get().getComputers()) {
            VirtualChannel channel = c.getChannel();
            if (channel instanceof Channel) { // i.e., not MasterComputer
                LOGGER.fine(() -> "Registering " + targets + " on existing agent " + c.getName());
                try {
                    channel.call(new Register(targets));
                } catch (IOException | InterruptedException x) {
                    LOGGER.log(Level.WARNING, null, x);
                }
            }
        }
    }

    @Extension(ordinal = 100) public static final class Listener extends ComputerListener {

        @Override public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener) throws IOException, InterruptedException {
            List<Target> targets = ExtensionList.lookupSingleton(StderrLogConfig.class).targets;
            if (targets != null && !targets.isEmpty()) {
                LOGGER.fine(() -> "Registering " + targets + " on new agent " + c.getName());
                channel.call(new Register(targets));
            }
        }

    }

    private static class Register extends MasterToSlaveCallable<Void, RuntimeException> {

        private static Handler handler;
        private static Map<Logger, Level> oldLevels;
        private final List<Target> targets;

        Register(List<Target> targets) {
            this.targets = targets != null ? targets : Collections.emptyList();
        }

        @SuppressFBWarnings(value = "LI_LAZY_INIT_UPDATE_STATIC", justification = "really meant to manage static state this way")
        @Override public Void call() {
            if (handler == null) {
                handler = new ConsoleHandler();
                handler.setFormatter(new SupportLogFormatter());
            }
            if (oldLevels == null) {
                oldLevels = new HashMap<>();
            } else {
                for (var entry : oldLevels.entrySet()) {
                    var logger = entry.getKey();
                    logger.fine(() -> "Deregistered logger on " + logger.getName());
                    logger.removeHandler(handler);
                    logger.setLevel(entry.getValue());
                }
                oldLevels.clear();
            }
            Level finest = Level.INFO;
            for (Target target : targets) {
                Level level = target.parsedLevel();
                if (level.intValue() < finest.intValue()) {
                    finest = level;
                }
                var logger = Logger.getLogger(target.name);
                oldLevels.put(logger, logger.getLevel());
                logger.setLevel(level);
                logger.addHandler(handler);
                logger.fine(() -> "Registered logger on " + target.name + "@" + level);
            }
            handler.setLevel(finest);
            return null;
        }

    }

}
