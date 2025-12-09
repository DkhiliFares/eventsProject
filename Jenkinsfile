pipeline {
    agent { 
        docker {
            image 'maven:3.9.6-eclipse-temurin-17'
            args '-v /root/.m2:/root/.m2'
        }
    }

    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        NEXUS_CREDENTIALS = credentials('nexus-credentials')
        SONAR_HOST_URL = 'http://192.168.20.139:9000'
        IMAGE_NAME = "fares2112/event-app"
        DOCKER_IMAGE = "${IMAGE_NAME}:${env.BUILD_NUMBER}"
    }

    stages {

        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }

                    stage('SonarQube Analysis') {
                steps {
                    withSonarQubeEnv('sonarqube') {
                        withCredentials([string(credentialsId: 'sonarqube', variable: 'SONAR_TOKEN')]) {
                            sh """
                                echo "Starting SonarQube analysis..."
                                mvn sonar:sonar \
                                    -Dsonar.projectKey=eventsproject \
                                    -Dsonar.host.url=${SONAR_HOST_URL} \
                                    -Dsonar.login=${SONAR_TOKEN} \
                                    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                                    -B
                            """
                        }
                    }
                }
            }

        stage('Wait for Analysis Completion') {
    steps {
        echo "Waiting 30 seconds for SonarQube analysis to complete..."
        sleep 30
    }
}

stage('Quality Gate') {
    steps {
        timeout(time: 15, unit: 'MINUTES') {
            script {
                echo "Checking Quality Gate..."
                def qg = waitForQualityGate abortPipeline: true
                echo "Quality Gate status: ${qg.status}"
            }
        }
    }
}

        stage('Artifact Upload to Nexus') {
            steps {
                sh "mvn deploy -DskipTests"
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${DOCKER_IMAGE} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', 
                                                 passwordVariable: 'DOCKER_PASSWORD', 
                                                 usernameVariable: 'DOCKER_USER')]) {
                    sh "docker login -u ${DOCKER_USER} -p ${DOCKER_PASSWORD}"
                    sh "docker push ${DOCKER_IMAGE}"
                    sh "docker push ${IMAGE_NAME}:latest"
                }
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                sh "docker-compose -f docker-compose.yml up -d"
            }
        }
    }

    post {
        failure {
            echo "Pipeline failed! Check the SonarQube Quality Gate and logs."
        }
        success {
            echo "Pipeline completed successfully."
        }
    }
}
