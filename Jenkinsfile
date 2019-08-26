stage('all') {
    node('windock') {
        def tmp = pwd tmp: true
        def ok = infra.retrieveMavenSettingsFile("$tmp/settings-azure.xml")
        assert ok
        checkout scm
        withEnv(["WSTMP=$tmp"]) {
            bat($/
                docker run --rm -v %CD%:c:\ws -v %WSTMP%:c:\wstmp -v m2repo:C:\Users\ContainerAdministrator\.m2\repository jenkins4eval/maven-windows-jdk-8 cmd /c mvn -B -s c:\wstmp\settings-azure.xml -ntp -f \ws --show-version clean verify
            /$)
        }
    }
}
