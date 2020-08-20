pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'Building..'
                sh 'sh gradlew assembleDebug'
            }
        }	
        stage('SCA') {
            steps {
                 sh 'curl -sSL https://download.sourceclear.com/ci.sh | sh'
                }
         }
        stage('Upload and Scan') {
            steps {
                    echo 'test'
                }
            }
        stage('Deploy') {
            steps {
                echo 'Deploying....'
            }
        }
    }
}
