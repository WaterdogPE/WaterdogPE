pipeline {
    agent any
    tools {
        maven 'Maven 3'
        jdk 'Java 17'
    }
    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '15'))
    }
    stages {
        stage('Build') {
            steps {
                sh 'sed -i -e s/#build/"${BUILD_ID}"/g ./src/main/java/dev/waterdog/waterdogpe/VersionInfo.java'
                sh 'mvn clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Snapshot') {
            when {
                branch "master"
            }
            steps {
                sh 'mvn source:jar deploy -DskipTests'
            }
        }

        stage('Release') {
            when {
                branch "release"
            }
            steps {
                sh 'mvn javadoc:jar source:jar deploy -DskipTests'
            }
        }

    }
    post {
        always {
            deleteDir()
        }
        success {
            withCredentials([string(credentialsId: 'WDPE_Discord_Webhook', variable: 'TOKEN')]) {
                discordSend(webhookURL: "$TOKEN", description: "**Build:** ${env.BUILD_NUMBER}\n**Status:** Success\n\n**Changes:**\n${env.BUILD_URL}", footer: "Waterdog Jenkins", link: "${env.BUILD_URL}", successful: true, title: "Build Success: WaterdogPE", unstable: false, result: "SUCCESS")
            }
        }
        failure {
            withCredentials([string(credentialsId: 'WDPE_Discord_Webhook', variable: 'TOKEN')]) {
                discordSend(webhookURL: "$TOKEN", description: "**Build:** ${env.BUILD_NUMBER}\n**Status:** Failure\n\n**Changes:**\n${env.BUILD_URL}", footer: "Waterdog Jenkins", link: "${env.BUILD_URL}", successful: true, title: "Build Failed: WaterdogPE", unstable: false, result: "FAILURE")
            }
        }
    }
}