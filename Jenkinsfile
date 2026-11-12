// ShopSphere CI (P1-T10): build and test backend and frontend, build the
// backend Docker image, publish test results. CI only: nothing is pushed or deployed.
//
// Requires an agent with Maven, Node.js 20+ and the Docker CLI (the project's
// Jenkins image in infrastructure/jenkins has all three). Backend tests need
// PostgreSQL and Redis; the pipeline starts throwaway containers for them on
// CI_NETWORK and removes them at the end.

pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds(abortPrevious: true)
    }

    triggers {
        // Used by plain Pipeline jobs; Multibranch jobs are triggered by the GitHub webhook instead.
        githubPush()
    }

    environment {
        BACKEND_DIR  = 'backend/shopsphere-api'
        FRONTEND_DIR = 'frontend/shopsphere-web'
        IMAGE_NAME   = 'shopsphere-api'
        // Docker network the test containers join. Must be reachable from this agent
        // (the project's Jenkins container is attached to it).
        CI_NETWORK   = "${env.CI_NETWORK ?: 'shopsphere-network'}"
        MAVEN_OPTS   = '-Dmaven.repo.local=/var/jenkins_home/.m2/repository'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = sh(returnStdout: true, script: 'git rev-parse --short=7 HEAD').trim()
                    env.IMAGE_TAG = env.GIT_SHORT
                }
                sh 'git log -1 --pretty=format:"Commit: %h%nAuthor: %an%nDate:   %ad%nSubject: %s%n"'
                echo "Branch: ${env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'n/a'}  Change: ${env.CHANGE_ID ? 'PR #' + env.CHANGE_ID : 'none'}"
            }
        }

        stage('Backend Build') {
            steps {
                dir(env.BACKEND_DIR) {
                    sh 'mvn -B -ntp -DskipTests package'
                }
            }
        }

        stage('Backend Tests') {
            steps {
                script {
                    // Unique, filesystem/DNS-safe id for this build's test containers.
                    env.CI_ID = sh(returnStdout: true,
                        script: 'printf "%s" "ci-$BUILD_TAG" | tr -c "A-Za-z0-9_.-" "-" | cut -c1-48').trim()
                    env.CI_DB_PASSWORD = sh(returnStdout: true, script: 'cat /proc/sys/kernel/random/uuid').trim()
                }
                sh '''
                    docker run -d --name "$CI_ID-pg" --network "$CI_NETWORK" \
                        -e POSTGRES_DB=shopsphere -e POSTGRES_USER=shopsphere -e POSTGRES_PASSWORD="$CI_DB_PASSWORD" \
                        postgres:16-alpine
                    docker run -d --name "$CI_ID-redis" --network "$CI_NETWORK" redis:7-alpine
                    for i in $(seq 1 30); do
                        docker exec "$CI_ID-pg" pg_isready -U shopsphere -d shopsphere >/dev/null 2>&1 \
                          && [ "$(docker exec "$CI_ID-redis" redis-cli ping 2>/dev/null)" = "PONG" ] && exit 0
                        sleep 2
                    done
                    echo "Test services did not become ready" >&2
                    exit 1
                '''
                withEnv([
                    "DB_HOST=${env.CI_ID}-pg",
                    'DB_PORT=5432',
                    'DB_NAME=shopsphere',
                    'DB_USERNAME=shopsphere',
                    "DB_PASSWORD=${env.CI_DB_PASSWORD}",
                    "SPRING_DATA_REDIS_HOST=${env.CI_ID}-redis",
                ]) {
                    dir(env.BACKEND_DIR) {
                        sh 'mvn -B -ntp test'
                    }
                }
            }
            post {
                always {
                    junit testResults: "${env.BACKEND_DIR}/target/surefire-reports/*.xml", allowEmptyResults: true
                }
            }
        }

        stage('Frontend Build') {
            steps {
                dir(env.FRONTEND_DIR) {
                    sh '''
                        if [ -f package-lock.json ]; then npm ci; else npm install; fi
                        npm run build
                    '''
                }
            }
        }

        stage('Frontend Tests') {
            steps {
                dir(env.FRONTEND_DIR) {
                    // No test framework is configured yet; run tests only if a "test" script exists.
                    sh '''
                        if node -e "process.exit(require('./package.json').scripts?.test ? 0 : 1)"; then
                            npm test
                        else
                            echo "No frontend tests configured (no \\"test\\" script in package.json); skipping."
                        fi
                    '''
                }
            }
        }

        stage('Docker Build') {
            steps {
                // Backend only: the frontend has no Dockerfile in this repository yet.
                sh '''
                    docker build -t "$IMAGE_NAME:$IMAGE_TAG" -t "$IMAGE_NAME:ci-$BUILD_NUMBER" "$BACKEND_DIR"
                    docker image ls "$IMAGE_NAME"
                '''
            }
        }

        stage('Pipeline Summary') {
            steps {
                echo "Build #${env.BUILD_NUMBER} OK: ${env.IMAGE_NAME}:${env.IMAGE_TAG} built (not pushed). " +
                     "Test results: 'Test Result' on the build page."
            }
        }
    }

    post {
        failure {
            echo "Build #${env.BUILD_NUMBER} FAILED. See the failing stage above; test reports: 'Test Result' on the build page."
        }
        always {
            // "always" runs before "success"/"failure", so archive here, before the workspace is cleaned.
            script {
                if (currentBuild.currentResult == 'SUCCESS') {
                    archiveArtifacts artifacts: "${env.BACKEND_DIR}/target/*.jar", fingerprint: true, allowEmptyArchive: true
                }
            }
            // Remove this build's throwaway test containers (no-op if they were never started).
            sh '''
                if [ -n "${CI_ID:-}" ]; then
                    docker rm -f "$CI_ID-pg" "$CI_ID-redis" >/dev/null 2>&1 || true
                fi
            '''
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}
