/*
 * The MIT License
 *
 * Copyright 2017 CloudBees, Inc.
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
import hudson.cli.CLICommand;
import io.jenkins.lib.support_log_formatter.SupportLogFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import jenkins.model.Jenkins;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

@Extension public class TailLogCommand extends CLICommand {

    @Argument(metaVar="NAME", required=true, usage="Logger name(s) to record; for example: hudson.model", multiValued=true) public List<String> names;

    @Option(name="-l", usage="Level such as FINE.") public String level = "ALL";

    @Override public String getShortDescription() {
        return "Tail a Jenkins system log.";
    }

    @Override protected int run() throws Exception {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        Map<Logger, Level> oldLevels = new HashMap<>();
        Handler handler = new StreamHandler(stdout, new SupportLogFormatter()) {
            @Override public synchronized void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        };
        Level logLevel = Level.parse(level);
        handler.setLevel(logLevel);
        for (String name : names) {
            Logger logger = Logger.getLogger(name);
            oldLevels.put(logger, logger.getLevel());
            logger.setLevel(logLevel);
            logger.addHandler(handler);
        }
        try {
            stderr.println("Waiting for messages or interruption");
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException x) {
            stderr.println("Stopped.");
        } finally {
            for (Map.Entry<Logger, Level> entry : oldLevels.entrySet()) {
                entry.getKey().removeHandler(handler);
                entry.getKey().setLevel(entry.getValue());
            }
        }
        return 0;
    }

}
