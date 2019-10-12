# Overview

Adds a CLI command to watch Jenkins system log messages in real time.

For detailed usage information, go to `http://jenkins/cli/command/tail-log` after installation.

# Releasing

As per [instructions](https://github.com/jenkinsci/incrementals-tools/#superseding-maven-releases):

```bash
mvn -Dset.changelist -DaltDeploymentRepository=maven.jenkins-ci.org::default::https://repo.jenkins-ci.org/releases/ clean deploy
```

then update [Releases](https://github.com/jenkinsci/log-cli-plugin/releases).
