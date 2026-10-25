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
                        docker start pre-event-mysql pre-event-redis pre-event-kafka
                        for i in $(seq 1 30); do
                          docker exec pre-event-mysql mysqladmin ping --silent >/dev/null 2>&1 && break
                          sleep 2
                        done

                        docker rm -f ticket-hub-backend || true
                        for n in 1 2; do
                          docker rm -f ticket-hub-backend-$n || true
                          docker run -d --name ticket-hub-backend-$n \
                            --network environment_pre-event-network \
                            --env-file "$ENV_FILE" \
                            -e BACKEND_IP=1122 \
                            -e SPRING_DATA_REDIS_HOST=pre-event-redis -e SPRING_DATA_REDIS_PORT=6379 \
                            -e SPRING_KAFKA_BOOTSTRAP_SERVERS=pre-event-kafka:9092 \
                            -e SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=50 -e SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=10 \
                            -e SERVER_FORWARD_HEADERS_STRATEGY=native \
                            $IMAGE:$BUILD_NUMBER
                          up=false
                          for i in $(seq 1 60); do
                            code=$(curl -s -o /dev/null -w '%{http_code}' http://ticket-hub-backend-$n:1122/actuator/health || true)
                            [ "$code" != 000 ] && { up=true; break; }
                            [ "$(docker inspect -f '{{.State.Running}}' ticket-hub-backend-$n)" = true ] || break
                            sleep 2
                          done
                          [ "$up" = true ] || { docker logs --tail 100 ticket-hub-backend-$n; exit 1; }
                          echo "ticket-hub-backend-$n is up"
                          if docker ps --format '{{.Names}}' | grep -qx pre-event-nginx; then docker exec pre-event-nginx nginx -s reload; fi
                        done
                    '''
                }
            }
        }
    }
}