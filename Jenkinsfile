pipeline {
    agent any
    tools {
        maven 'Maven 3'
        jdk 'Java 8'
    }
    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '15'))
    }
    stages {
        stage ('Build') {
            steps {
                sh 'mvn clean package'
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

        stage ('Release') {
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
            discordSend(webhookURL: "https://discordapp.com/api/webhooks/772560103869644850/1eOgPJakLJcFjqFNnC0ngK152FU_MqfiyR7LgON6hKSeXgb69o1vIIaDg7JRMGfEov2p", description: "**Build:** ${env.BUILD_NUMBER}\n**Status:** Success\n\n**Changes:**\n${env.BUILD_URL}", footer: "Waterdog Jenkins", link: "${env.BUILD_URL}", successful: true, title: "Build Success: WaterdogPE", unstable: false, result: "SUCCESS")
        }
            failure {
            discordSend(webhookURL: "https://discordapp.com/api/webhooks/772560103869644850/1eOgPJakLJcFjqFNnC0ngK152FU_MqfiyR7LgON6hKSeXgb69o1vIIaDg7JRMGfEov2p", description: "**Build:** ${env.BUILD_NUMBER}\n**Status:** Failure\n\n**Changes:**\n${env.BUILD_URL}", footer: "Waterdog Jenkins", link: "${env.BUILD_URL}", successful: true, title: "Build Failed: WaterdogPE", unstable: false, result: "FAILURE")
        }

}