stage('all') {
    node('windock') {
        def tmp = pwd tmp: true
        def ok = infra.retrieveMavenSettingsFile("$tmp/settings-azure.xml")
        assert ok
        checkout scm
        withEnv(["WSTMP=$tmp"]) {
            powershell($/
                docker run --rm -v $${env:CD}:c:\ws -v $${env:WSTMP}:c:\wstmp -v m2repo:C:\Users\ContainerAdministrator\.m2\repository -e MAVEN_TERMINATE_CMD=on jenkins4eval/maven-windows-jdk-8 mvn -B -s c:\wstmp\settings-azure.xml -ntp -f \ws --show-version clean verify
            /$)
        }
    }
}
