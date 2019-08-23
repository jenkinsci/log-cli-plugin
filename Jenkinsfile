node('windock') {
    withEnv(["MVN=${tool('mvn')}"]) {
        bat(/docker run --rm -v %MVN%:c:\mvn openjdk:8-windowsservercore-1809 cmd \/c dir \/sb \mvn /)
    }
}
