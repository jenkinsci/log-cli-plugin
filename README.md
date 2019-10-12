[Wiki](https://wiki.jenkins.io/display/JENKINS/Log+CLI+Plugin)

# Releasing

As per [instructions](https://github.com/jenkinsci/incrementals-tools/#superseding-maven-releases):

```bash
mvn -Dset.changelist -DaltDeploymentRepository=maven.jenkins-ci.org::default::https://repo.jenkins-ci.org/releases/ clean deploy
```

then update [Releases](https://github.com/jenkinsci/log-cli-plugin/releases).
