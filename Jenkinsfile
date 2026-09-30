pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        // Android SDK location on the Jenkins VM
        ANDROID_HOME     = '/opt/android-sdk'
        ANDROID_SDK_ROOT = '/opt/android-sdk'
        // Keep Gradle output plain and avoid a long-lived daemon on CI
        GRADLE_OPTS      = '-Dorg.gradle.daemon=false'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare') {
            steps {
                sh 'chmod +x gradlew'
                sh 'java -version'
            }
        }

        stage('Build debug APK') {
            steps {
                sh './gradlew assembleDebug --no-daemon --stacktrace'
            }
        }

        stage('Unit tests') {
            steps {
                sh './gradlew testDebugUnitTest --no-daemon'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'app/build/test-results/testDebugUnitTest/*.xml'
                }
            }
        }

        stage('Archive APK') {
            steps {
                archiveArtifacts artifacts: 'app/build/outputs/apk/debug/*.apk', fingerprint: true
            }
        }

        stage('Distribute') {
            // Publish on branch builds; skip PR builds
            when { not { changeRequest() } }
            steps {
                withCredentials([file(credentialsId: 'firebase-sa', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                    sh './gradlew appDistributionUploadDebug --no-daemon'
                }
            }
        }
    }

    post {
        success { echo 'Build succeeded' }
        failure { echo 'Build failed' }
    }
}
