stage('all') {
    node('windock') {
        checkout scm
        withEnv(["MVN=${tool('mvn')}"]) {
            bat($/
                docker run --rm -v %MVN%:c:\mvn -v %CD%:c:\ws -v m2repo:C:\Users\ContainerAdministrator\.m2\repository openjdk:8-windowsservercore-1809 cmd /c \mvn\bin\mvn -B help:evaluate -Dexpression=settings.localRepository
                rem docker run --rm -v %MVN%:c:\mvn -v %CD%:c:\ws openjdk:8-windowsservercore-1809 cmd /c \mvn\bin\mvn -B -f \ws clean verify
            /$)
        }
    }
}
