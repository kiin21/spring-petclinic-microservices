def VALID_SERVICES = [
    'spring-petclinic-admin-server',
    'spring-petclinic-api-gateway',
    'spring-petclinic-config-server',
    'spring-petclinic-genai-service',
    'spring-petclinic-customers-service',
    'spring-petclinic-discovery-server',
    'spring-petclinic-vets-service',
    'spring-petclinic-visits-service',
]
def AFFECTED_SERVICES = ''

pipeline {
    agent any

    tools {
        maven 'Maven-3.9.4'
        jdk 'OpenJDK-17'
    }

    environment {
        DOCKER_REGISTRY = 'anhkhoa217'
        GITHUB_CREDENTIALS_ID = 'github_pat'
        GKE_CREDENTIALS_ID = 'gke_credentials'
        DOCKER_HUB_CREDENTIALS_ID = 'dockerhub_credentials'
        GITHUB_REPO_URL = 'https://github.com/kiin21/spring-petclinic-microservices.git'
    }

    stages {
        stage('Clone Code') {
            steps {
                script {
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '**']],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [
                            [$class: 'CloneOption', noTags: false, depth: 0, shallow: false],
                            [$class: 'PruneStaleBranch']
                        ],
                        userRemoteConfigs: [[
                            url: env.GITHUB_REPO_URL,
                            credentialsId: env.GITHUB_CREDENTIALS_ID
                        ]]
                    ])
                    
                    // Explicitly set GIT_COMMIT if it's not already set
                    if (!env.GIT_COMMIT) {
                        env.GIT_COMMIT = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                    }
                    
                    echo "Current commit: ${env.GIT_COMMIT}"
                }
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    def affectedServices = []

                    // Check for tag build first
                    if (env.TAG_NAME) { // If a tag is present, we assume all services are affected
                        echo "A new release found with tag ${env.TAG_NAME}"
                        affectedServices = VALID_SERVICES
                        AFFECTED_SERVICES = affectedServices.join(' ')
                        echo "Changed services (tag): ${AFFECTED_SERVICES}"
                        return
                    }

                    // Regular build with change detection
                    def changedFiles = sh(
                        script: """
                            # Get the last successful commit hash or use HEAD~1
                            LAST_COMMIT=\$(git rev-parse HEAD~1 2>/dev/null || git rev-parse origin/main)

                            # Get the changed files
                            git diff --name-only \$LAST_COMMIT HEAD
                        """,
                        returnStdout: true
                    ).trim()

                    if (changedFiles == null || changedFiles.trim().isEmpty()) {
                        echo "No changes detected."
                        AFFECTED_SERVICES = ''
                        return
                    }

                    changedFiles.split("\n").each { file ->
                        VALID_SERVICES.each { service ->
                            if (file.startsWith("${service}/") && !affectedServices.contains(service)) {
                                affectedServices.add(service)
                                echo "Detected changes in ${service}"
                            }
                        }
                    }

                    if (affectedServices.isEmpty()) {
                        echo "No valid service changes detected. Skipping pipeline."
                        AFFECTED_SERVICES = ''
                        return
                    }

                    def servicesString = affectedServices.join(' ')
                    AFFECTED_SERVICES = servicesString
                    echo "ENV_AFFECTED_SERVICES: [${AFFECTED_SERVICES}]"
                    echo "Changed services: ${servicesString}"
                }
            }
        }

        stage('Login to DockerHub') {
            when {
                expression { return AFFECTED_SERVICES != '' || env.TAG_NAME != null }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKER_HUB_CREDENTIALS_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
                }
            }
        }

        stage('Build and Push Docker Images') {
            when {
                expression { return AFFECTED_SERVICES != '' || env.TAG_NAME != null }
            }
            steps {
                script {
                    def CONTAINER_TAG = env.TAG_NAME ? env.TAG_NAME : env.GIT_COMMIT.take(7)
                    echo "Using tag: ${CONTAINER_TAG}"

                    echo "Building images for services: ${AFFECTED_SERVICES}"
                    
                    // Split the string into an array
                    def services = AFFECTED_SERVICES.split(' ')
                                      
                    for (service in services) {
                        echo "Building and pushing Docker image for ${service}"
                        sh """
                            cd ${service}
                            mvn clean install -P buildDocker -Dmaven.test.skip=true \
                                -Ddocker.image.prefix=${env.DOCKER_REGISTRY} \
                                -Ddocker.image.tag=${CONTAINER_TAG}
                            docker push ${env.DOCKER_REGISTRY}/${service}:${CONTAINER_TAG}
                            cd ..
                        """
                    }
                }
            }
        }

        stage('Clean Up') {
            when {
                expression { return AFFECTED_SERVICES != '' || env.TAG_NAME != null }
            }
            steps {
                sh "docker system prune -af"
                sh "docker logout"
                echo "Docker cleanup and logout completed"
            }
        }
    }

    post {
        always {
            cleanWs()
            echo "Workspace cleaned"
        }
        success {
            step([
                $class: 'GitHubCommitStatusSetter',
                statusResultSource: [
                    $class: 'DefaultStatusResultSource'
                ]
            ])
        }
        failure {
            step([
                $class: 'GitHubCommitStatusSetter',
                statusResultSource: [
                    $class: 'DefaultStatusResultSource'
                ]
            ])
        }
    }
}