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
import hudson.cli.CLICommand;
import hudson.util.StreamCopyThread;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import jenkins.model.Jenkins;

/**
 * Runs an interactive shell on the controller.
 */
@Extension public final class InteractiveShellCommand extends CLICommand {

    @Override public String getShortDescription() {
        return "Runs an interactive shell on the controller.";
    }

    @Override protected int run() throws Exception {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        var proc = new ProcessBuilder("bash", "-i").start(); // TODO allow shell to be customized?
        var prefix = "interactive-shell " + new Date();
        var stdoutT = new StreamCopyThread(prefix + " stdout", proc.getInputStream(), stdout, true);
        stdoutT.start();
        var stderrT = new StreamCopyThread(prefix + " stderr", proc.getErrorStream(), stderr, true);
        stderrT.start();
        var stdinT = new StreamCopyThread(prefix + " stdin", stdin, new FlushingOutputStream(proc.getOutputStream()), true);
        stdinT.start();
        try {
            int r = proc.waitFor();
            return r == 0 ? 0 : r + 16;
        } finally {
            stdout.close();
            stderr.close();
            stdin.close();
            stdoutT.interrupt();
            stderrT.interrupt();
            stdinT.interrupt();
        }
    }

    private static final class FlushingOutputStream extends FilterOutputStream {

        FlushingOutputStream(OutputStream out) {
            super(out);
        }

        @Override public void write(int b) throws IOException {
            super.write(b);
            flush();
        }

        @Override public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            flush();
        }

    }

}
