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
                  veracode applicationName: "twidere-android", \
                  canFailJob: true, \
                  criticality: "VeryHigh",  \
                  scanName: "${BUILD_ID}", \
                  uploadIncludesPattern: "**/twidere/build/outputs/apk/fdroid/debug/twidere-fdroid-debug.apk", \
                  useIDkey: true, \
                  vid: "${VERACODE_API_ID}",  \
                  vkey: "${VERACODE_API_SECRET}"
                }
            }
        stage('Deploy') {
            steps {
                echo 'Deploying....'
            }
        }
    }
}
