# Streaming logs from the CLI

This plugin adds a CLI command to watch Jenkins system log messages in real time.
This is useful if you wish to monitor activity from a shell.
You can get detailed logs from a particular component.

For detailed usage information, go to `https://<jenkins>/cli/command/tail-log` after installation.

# Streaming detailed logs to standard error

You can also choose to send system logs to the Jenkins controller’s standard error stream.
While Jenkins sends `INFO` and above by default,
this plugin allows you to configure more detailed messages from particular components of interest.

From the GUI, check **Fine logging to standard error** in system configuration.
Using JCasC (`configuration-as-code`), use the following format:

```yaml
unclassified:
  stderrLog:
    targets:
    - name: io.jenkins.plugins.whatever
      level: FINE
```

As a bonus, this will stream messages in the selected components printed by agents as well,
which are otherwise very tricky to collect.

Viewing the resulting logs is a separate matter.
On Kubernetes, you can use for example `kubectl logs -f $podname`,
or try `kubectl krew install tail` or `kubectl krew install stern` for richer options.
