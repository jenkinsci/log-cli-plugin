node('windock') {
    withEnv(["MVN=${tool('mvn').replace('\\', '/')}"]) {
        bat(/docker run --rm -v %MVN%:\mvn openjdk:8-windowsservercore-1809 \mvn\bin\mvn -version/)
    }
}
