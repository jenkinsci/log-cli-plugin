stage('all') {
    node('windock') {
        def tmp = pwd tmp: true
        def ok = infra.retrieveMavenSettingsFile("$tmp/settings-azure.xml")
        assert ok
        checkout scm
        withEnv(["MVN=${tool('mvn')}", "WSTMP=$tmp"]) {
            bat($/
                docker run --rm -v %CD%:c:\ws -v %WSTMP%:c:\wstmp -v %MVN%:c:\mvn -v m2repo:C:\Users\ContainerAdministrator\.m2\repository openjdk:8-windowsservercore-1809 cmd /c \mvn\bin\mvn -B -s c:\wstmp\settings-azure.xml -f \ws --show-version clean verify
            /$)
        }
    }
}
