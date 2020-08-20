pipeline {
    agent {
      docker {
          image 'androidsdk/android-26'
        // image 'uber/android-build-environment'
      }
    }
    stages {
        stage('Build') {
            steps {
                echo 'Building..'
                // sh 'echo y | /usr/local/android-sdk/tools/android'
                sh 'sh gradlew assembleDebug'
            }
        }	
        stage('Security Assessment') {
            parallel {
                stage('SCA') {
                    steps {
                        sh 'curl -sSL https://download.sourceclear.com/ci.sh | sh'
                    }
                }
                stage('Upload and Scan') {
                    steps {
                        sh 'curl -sS "https://search.maven.org/solrsearch/select?q=g:%22com.veracode.vosp.api.wrappers%22&rows=20&wt=json" | jq -r \'.["response"]["docs"][0].latestVersion\' > wrapper-version'
                        sh 'VERACODE_WRAPPER_VERSION=$(cat wrapper-version); curl -sS "https://repo1.maven.org/maven2/com/veracode/vosp/api/wrappers/vosp-api-wrappers-java/${VERACODE_WRAPPER_VERSION}/vosp-api-wrappers-java-${VERACODE_WRAPPER_VERSION}.jar" > veracode-wrapper'
                        echo 'test'
                    }
                }
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying....'
            }
        }
    }
}
