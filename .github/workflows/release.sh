#!/bin/bash
set -euxo pipefail
# see https://github.com/jenkinsci/incrementals-tools/#superseding-maven-releases
export MAVEN_OPTS=-Djansi.force=true
mvn -B -V -s .github/workflows/settings.xml -ntp -Dstyle.color=always -Dset.changelist -DaltDeploymentRepository=maven.jenkins-ci.org::default::https://repo.jenkins-ci.org/releases/ -Pquick-build clean deploy
version=$(mvn -B -ntp -Dset.changelist -Dexpression=project.version -q -DforceStdout help:evaluate)
# TODO would be more legible with jo:
curl -H "Authorization: Bearer $GITHUB_TOKEN" -s -d '{"ref":"refs/tags/'$version'","sha":"'$GITHUB_SHA'"}' $GITHUB_API_URL/repos/$GITHUB_REPOSITORY/git/refs
release=$(curl -H "Authorization: Bearer $GITHUB_TOKEN" -s $GITHUB_API_URL/repos/$GITHUB_REPOSITORY/releases | jq -e -r '.[] | select(.draft == true and .name == "next") | .id')
curl -H "Authorization: Bearer $GITHUB_TOKEN" -X PATCH -d '{"draft":"false","name":"'$version'","tag_name":"'$version'"}' $GITHUB_API_URL/repos/$GITHUB_REPOSITORY/releases/$release
