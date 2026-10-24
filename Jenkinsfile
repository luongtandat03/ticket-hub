pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    environment { IMAGE = 'ticket-hub-backend' }

    stages {
        stage('Build & Test') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-25'
                    args '-v maven-repo:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                sh 'mvn -B clean verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Docker Image') {
            steps {
                sh 'docker build -t $IMAGE:$BUILD_NUMBER -t $IMAGE:latest ticket-hub-backend-start'
            }
        }
        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub',
                        usernameVariable: 'DH_USER', passwordVariable: 'DH_TOKEN')]) {
                    sh '''
                        echo "$DH_TOKEN" | docker login -u "$DH_USER" --password-stdin
                        docker tag $IMAGE:$BUILD_NUMBER $DH_USER/$IMAGE:$BUILD_NUMBER
                        docker tag $IMAGE:$BUILD_NUMBER $DH_USER/$IMAGE:latest
                        docker push $DH_USER/$IMAGE:$BUILD_NUMBER
                        docker push $DH_USER/$IMAGE:latest
                        docker logout
                    '''
                }
            }
        }
        stage('Deploy') {
            steps {
                withCredentials([file(credentialsId: 'ticket-hub-env', variable: 'ENV_FILE')]) {
                    sh '''
                        docker rm -f ticket-hub-backend || true
                        docker run -d --name ticket-hub-backend \
                          --network environment_pre-event-network \
                          --env-file "$ENV_FILE" \
                          -e BACKEND_IP=1122 \
                          -e SPRING_DATA_REDIS_HOST=pre-event-redis -e SPRING_DATA_REDIS_PORT=6379 \
                          -e SPRING_KAFKA_BOOTSTRAP_SERVERS=pre-event-kafka:9092 \
                          -p 1122:1122 \
                          $IMAGE:$BUILD_NUMBER
                        for i in $(seq 1 60); do
                          docker logs ticket-hub-backend 2>&1 | grep -q 'Started StartApplication' && { echo 'Backend is up'; exit 0; }
                          [ "$(docker inspect -f '{{.State.Running}}' ticket-hub-backend)" = true ] || break
                          sleep 2
                        done
                        docker logs --tail 100 ticket-hub-backend
                        exit 1
                    '''
                }
            }
        }
    }
}