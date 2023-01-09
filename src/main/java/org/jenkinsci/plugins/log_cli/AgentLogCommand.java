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

import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.cli.CLICommand;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.remoting.VirtualChannel;
import hudson.slaves.ComputerListener;
import io.jenkins.lib.support_log_formatter.SupportLogFormatter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

@Extension public final class AgentLogCommand extends CLICommand {

    private static final Logger LOGGER = Logger.getLogger(AgentLogCommand.class.getName());

    @Argument(metaVar="NAME", required=true, usage="Logger name(s) to record; for example: hudson.remoting.PingThread", multiValued=true) public List<String> names;

    @Option(name="-l", usage="Level such as FINE.") public String level = "ALL";

    @Override public String getShortDescription() {
        return "Request that any newly launched agents send specified log messages to standard output.";
    }

    @Override protected int run() throws Exception {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        ExtensionList.lookupSingleton(Listener.class).configure(names, Level.parse(level));
        return 0;
    }

    @Extension(ordinal = 100) public static final class Listener extends ComputerListener {

        private List<String> names;
        private Level level;

        void configure(List<String> names, Level level) throws IOException, InterruptedException {
            LOGGER.info(() -> "Configuring agent logs " + names + "@" + level);
            this.names = names;
            this.level = level;
            for (Computer c : Jenkins.get().getComputers()) {
                VirtualChannel channel = c.getChannel();
                if (channel instanceof Channel) { // i.e., not MasterComputer
                    LOGGER.info(() -> "Registering logs on existing agent " + c.getName());
                    channel.call(new Register(names, level));
                }
            }
        }

        @Override public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener) throws IOException, InterruptedException {
            if (names != null) {
                LOGGER.info(() -> "Registering logs on new agent " + c.getName());
                channel.call(new Register(names, level));
            }
        }
        
    }
    private static class Register extends MasterToSlaveCallable<Void, RuntimeException> {
        private static Handler handler;
        private static Map<Logger, Level> oldLevels;
        private final List<String> names;
        private final Level level;
        Register(List<String> names, Level level) {
            this.names = names;
            this.level = level;
        }
        @Override public Void call() {
            if (handler == null) {
                handler = new StreamHandler(System.out, new SupportLogFormatter()) {
                    @Override public synchronized void publish(LogRecord record) {
                        super.publish(record);
                        flush();
                    }
                };
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
            handler.setLevel(level);
            for (var name : names) {
                var logger = Logger.getLogger(name);
                oldLevels.put(logger, logger.getLevel());
                logger.setLevel(level);
                logger.addHandler(handler);
                logger.fine(() -> "Registered logger on " + name + "@" + level);
            }
            return null;
        }
    }

}
