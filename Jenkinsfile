stage('all') {
    node('windock') {
        def settingsXml = "${pwd tmp: true}/settings-azure.xml"
        def ok = infra.retrieveMavenSettingsFile(settingsXml)
        assert ok
        checkout scm
        withEnv(["MVN=${tool('mvn')}", "MAVEN_SETTINGS=$settingsXml"]) {
            bat($/
                docker run --rm -v %MVN%:c:\mvn -v %CD%:c:\ws -v m2repo:C:\Users\ContainerAdministrator\.m2\repository openjdk:8-windowsservercore-1809 cmd /c \mvn\bin\mvn -B -s %MAVEN_SETTINGS% help:evaluate -Dexpression=settings.localRepository
                rem docker run --rm -v %MVN%:c:\mvn -v %CD%:c:\ws openjdk:8-windowsservercore-1809 cmd /c \mvn\bin\mvn -B -f \ws clean verify
            /$)
        }
    }
}
