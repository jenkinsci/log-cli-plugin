node('windock') {
    bat 'docker run --help'
    withEnv(["MVN=${tool('mvn').replace('\\', '/').replace('C:/', '//c/')}"]) {
        bat 'docker run --rm -v %MVN%:\/mvn openjdk:8-windowsservercore-1809 /mvn/bin/mvn -version'
    }
}
