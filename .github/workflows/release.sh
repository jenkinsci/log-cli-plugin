#!/bin/bash
set -euxo pipefail
# see https://github.com/jenkinsci/incrementals-tools/#superseding-maven-releases
export MAVEN_OPTS=-Djansi.force=true
mvn -B -V -s .github/workflows/settings.xml -ntp -Dstyle.color=always -Dset.changelist -DaltDeploymentRepository=maven.jenkins-ci.org::default::https://repo.jenkins-ci.org/releases/ clean deploy
# TODO https://help.github.com/en/articles/virtual-environments-for-github-actions#github_token-secret use GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }} to define a release based on this version, after configuring Release Drafter as a step
