#!/bin/bash
set -euxo pipefail
# see https://github.com/jenkinsci/incrementals-tools/#superseding-maven-releases
export MAVEN_OPTS=-Djansi.force=true
mvn -B -V -s .github/workflows/settings.xml -ntp -Dstyle.color=always -Dset.changelist -DaltDeploymentRepository=maven.jenkins-ci.org::default::https://repo.jenkins-ci.org/releases/ -Pquick-build clean deploy
version=$(mvn -B -ntp -Dset.changelist -Dexpression=project.version -q -DforceStdout help:evaluate)
echo "::set-output name=version::$version"
# TODO would be more legible with jo:
curl -H "Authorization: Bearer $GITHUB_TOKEN" -s -d '{"ref":"refs/tags/'$version'","sha":"'$GITHUB_SHA'"}' $GITHUB_API_URL/repos/$GITHUB_REPOSITORY/git/refs
