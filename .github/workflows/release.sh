#!/bin/bash
set -euxo pipefail
# see https://github.com/jenkinsci/incrementals-tools/#superseding-maven-releases
export MAVEN_OPTS=-Djansi.force=true
mvn -B -V -s .github/workflows/settings.xml -ntp -Dstyle.color=always -Dset.changelist -DaltDeploymentRepository=maven.jenkins-ci.org::default::https://repo.jenkins-ci.org/releases/ -Pquick-build clean deploy
echo '::set-output name=version::'$(mvn -B -ntp -Dset.changelist -Dexpression=project.version -q -DforceStdout help:evaluate)
