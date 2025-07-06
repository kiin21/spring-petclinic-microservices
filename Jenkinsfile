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

def MONITORING_SERVICES = [
    'prometheus',
    'grafana',
    'loki'
]

def COVERAGE_CHECK_SERVICES = [
    'spring-petclinic-customers-service',
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
        MANIFEST_REPO = 'github.com/kiin21/petclinic-manifests.git'
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
                    def affectedMonitoring = []

                    // Check for tag build first
                    if (env.TAG_NAME) { // If a tag is present, we assume all services are affected

                        echo "Found tag ${env.TAG_NAME}"
   
                        affectedServices = VALID_SERVICES
                        affectedMonitoring = MONITORING_SERVICES

                        AFFECTED_SERVICES = (affectedServices + affectedMonitoring).join(' ')
                        
                        echo "Changed services: ${AFFECTED_SERVICES}"
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

                        // Check for monitoring service changes in docker folder
                        MONITORING_SERVICES.each { service ->
                            if (file.startsWith("docker/${service}/") && !affectedMonitoring.contains(service)) {
                                affectedMonitoring.add(service)
                                echo "Detected changes in monitoring service ${service}"
                            }
                        }

                        // Also check for general docker folder changes that affect monitoring
                        if (file.startsWith("docker/docker-compose") || file.startsWith("docker/prometheus") || 
                            file.startsWith("docker/grafana") || file.startsWith("docker/loki")) {
                            MONITORING_SERVICES.each { service ->
                                if (!affectedMonitoring.contains(service)) {
                                    affectedMonitoring.add(service)
                                }
                            }
                        }
                    }

                    def allAffected = affectedServices + affectedMonitoring
                    if (allAffected.isEmpty()) {
                        echo "No valid service changes detected. Skipping pipeline"
                        AFFECTED_SERVICES = ''
                        return
                    }

                    AFFECTED_SERVICES = allAffected.join(' ')
                    echo "ENV_AFFECTED_SERVICES: [${AFFECTED_SERVICES}]"
                    echo "Changed services: ${AFFECTED_SERVICES}"
                }
            }
        }

        // stage('Test') {
        //     when {
        //         expression { return AFFECTED_SERVICES != '' }
        //     }
        //     steps {
        //         script {
        //             def services = AFFECTED_SERVICES.split(' ')
        //             for (service in services) {
        //                 // Only test the 3 main business services
        //                 if (COVERAGE_CHECK_SERVICES.contains(service)) {
        //                     echo "Running tests for ${service}"
        //                     sh """
        //                         if [ -d "${service}" ]; then
        //                             cd ${service}
        //                             mvn clean verify -P springboot -Dspring.profiles.active=test
        //                         else
        //                             echo "Directory ${service} does not exist!"
        //                         fi
        //                     """
        //                 } else if (VALID_SERVICES.contains(service)) {
        //                     echo "Skipping tests for ${service} (not in test coverage list)"
        //                 } else {
        //                     echo "Skipping tests for monitoring service: ${service}"
        //                 }
        //             }
        //         }
        //     }
        //     post {
        //         always {
        //             script {
        //                 AFFECTED_SERVICES.split(' ').each { service ->
        //                     // Only collect test results from the 3 main services
        //                     if (COVERAGE_CHECK_SERVICES.contains(service) && fileExists("${service}/target/surefire-reports")) {
        //                         dir(service) {
        //                             // Store JUnit test results
        //                             junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

        //                             // Store JaCoCo coverage results
        //                             jacoco(
        //                                 execPattern: '**/target/jacoco.exec',
        //                                 classPattern: '**/target/classes',
        //                                 sourcePattern: '**/src/main/java',
        //                                 exclusionPattern: '**/config/**, **/dto/**, **/entity/**, **/exception/**, **/utils/**, **/generated/**'
        //                             )
        //                         }
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // }

        // stage('Check Coverage') {
        //     when {
        //         expression { return AFFECTED_SERVICES != '' }
        //     }
        //     steps {
        //         script {
        //             def services = AFFECTED_SERVICES.split(' ')
        //             def coveragePass = true

        //             for (service in services) {
        //                 // Only check coverage for specific main services
        //                 if (COVERAGE_CHECK_SERVICES.contains(service)) {
        //                     echo "Checking coverage for ${service}..."

        //                     // Debug: List all files in target directory
        //                     sh """
        //                         echo "=== Debug: Files in ${service}/target ==="
        //                         ls -la ${service}/target/ || echo "No target directory"
        //                         echo "=== Debug: Looking for JaCoCo files ==="
        //                         find ${service}/target -name "*jacoco*" -type f || echo "No JaCoCo files found"
        //                         echo "=== Debug: Looking for site directory ==="
        //                         ls -la ${service}/target/site/ || echo "No site directory"
        //                     """

        //                     def jacocoFile = "${service}/target/site/jacoco/jacoco.xml"
        //                     if (fileExists(jacocoFile)) {
        //                         def coverage = sh(script: """
        //                             grep -m 1 -A 1 '<counter type="INSTRUCTION"' ${jacocoFile} |
        //                             grep 'missed' |
        //                             sed -E 's/.*missed="([0-9]+)".*covered="([0-9]+)".*/\\1 \\2/' |
        //                             awk '{ print (\$2/(\$1+\$2))*100 }'
        //                         """, returnStdout: true).trim()

        //                         def coverageFloat = coverage as Float
        //                         echo "Code Coverage for ${service}: ${coverageFloat}%"

        //                         if (coverageFloat < 70) {
        //                             echo "Coverage for ${service} is below 70%. Build failed!"
        //                             coveragePass = false
        //                         }
        //                     } else {
        //                         echo "No coverage file found at ${jacocoFile}. Checking alternative locations..."
                                
        //                         // Check alternative locations
        //                         sh """
        //                             echo "=== Checking alternative JaCoCo locations ==="
        //                             find ${service} -name "jacoco.xml" -type f || echo "No jacoco.xml found anywhere"
        //                             find ${service} -name "*.exec" -type f || echo "No .exec files found"
        //                         """
        //                     }
        //                 } else if (VALID_SERVICES.contains(service)) {
        //                     echo "Skipping coverage check for ${service} (not in coverage check list)"
        //                 } else {
        //                     echo "Skipping coverage check for monitoring service: ${service}"
        //                 }
        //             }

        //             if (!coveragePass) {
        //                 error "Test coverage is below 70% for one or more main services. Build failed!"
        //             }
        //         }
        //     }
        // }

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
                    def CONTAINER_TAG = ""

                    if (env.TAG_NAME != null) {
                        CONTAINER_TAG = env.TAG_NAME
                    } else {
                        CONTAINER_TAG = env.GIT_COMMIT.take(7)
                    }

                    echo "Using tag: ${CONTAINER_TAG}"
                    echo "Building images for services: ${AFFECTED_SERVICES}"
                   
                    // Split the string into an array
                    def services = AFFECTED_SERVICES.split(' ')
                    for (service in services) {
                        // Handle microservices
                        if (VALID_SERVICES.contains(service)) {
                            echo "Building and pushing Docker image for microservice ${service}"
                            sh """
                                cd ${service}
                                mvn clean install -P buildDocker -Dmaven.test.skip=true \\
                                    -Ddocker.image.prefix=${env.DOCKER_REGISTRY} \\
                                    -Ddocker.image.tag=${CONTAINER_TAG}
                                docker push ${env.DOCKER_REGISTRY}/${service}:${CONTAINER_TAG}
                                cd ..
                            """
                        }
                        
                        // Handle monitoring services (independent check)
                        if (MONITORING_SERVICES.contains(service)) {
                            echo "Building and pushing Docker image for monitoring service ${service}"
                            sh """
                                cd docker
                                docker build -t ${env.DOCKER_REGISTRY}/${service}:${CONTAINER_TAG} -f ${service}/Dockerfile .
                                docker push ${env.DOCKER_REGISTRY}/${service}:${CONTAINER_TAG}
                                cd ..
                            """
                        }
                        
                        // Handle unknown services (safety check)
                        if (!VALID_SERVICES.contains(service) && !MONITORING_SERVICES.contains(service)) {
                            echo "Warning: Unknown service '${service}' detected. Skipping build."
                        }
                    }
                }
            }
        }

        stage('Deploy k8s') {
            when { expression { return AFFECTED_SERVICES != '' } }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: GITHUB_CREDENTIALS_ID, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                        sh """
                            git clone https://\$GIT_USERNAME:\$GIT_PASSWORD@${env.MANIFEST_REPO} k8s
                            cd k8s
                            git config user.name "Jenkins"
                            git config user.email "jenkins@example.com"
                        """
                    }
                    sh '''
                        cd k8s
                        # Extract old version using grep + cut
                        old_version=$(grep '^version:' Chart.yaml | cut -d' ' -f2)
                        echo "Old version: $old_version"
                        major=$(echo "$old_version" | cut -d. -f1)
                        minor=$(echo "$old_version" | cut -d. -f2)
                        patch=$(echo "$old_version" | cut -d. -f3)
                        new_patch=$((patch + 1))
                        new_version="$major.$minor.$new_patch"
                        echo "New version: $new_version"
                        # Update version using sed
                        sed -i "s/^version: .*/version: $new_version/" Chart.yaml
                    '''

                    def actualBranch = ""
                    def COMMIT_MSG = ""
                    def shouldDeploy = false
                    
                    if (env.TAG_NAME != null && env.TAG_NAME != '') { // check for tag
                        actualBranch = sh(
                            script: "git branch -r --contains ${env.TAG_NAME} | grep -v HEAD | head -1 | sed 's/.*origin\\///g' | xargs",
                            returnStdout: true
                        ).trim()
                        echo "Detected tag ${env.TAG_NAME} on branch ${actualBranch}"

                        if (actualBranch == 'main') {
                            echo "Deploying to production for tag ${env.TAG_NAME}"
                            COMMIT_MSG = "Deploy to production for tag: ${env.TAG_NAME}"
                            
                            def services = AFFECTED_SERVICES.split(' ')
                            for (service in services) {
                                if (VALID_SERVICES.contains(service)) {
                                    def shortName = service.replaceFirst('spring-petclinic-', '')
                                    echo "Updating ${shortName} for production deployment"
                                    sh """
                                        cd k8s
                                        sed -i '/${shortName}:/{n;n;s/tag:.*/tag: ${env.TAG_NAME}/}' environments/prod-values.yaml
                                    """
                                }
                                
                                if (MONITORING_SERVICES.contains(service)) {
                                    echo "Updating ${service} for production deployment"
                                    sh """
                                        cd k8s
                                        sed -i '/${service}:/{n;n;s/tag:.*/tag: ${env.TAG_NAME}/}' environments/prod-values.yaml
                                    """
                                }
                            }
                            echo "Deploying all services to production at tag ${env.TAG_NAME}"

                        } else {
                            echo "Deploying to staging for tag ${env.TAG_NAME}"
                            COMMIT_MSG = "Deploy to staging for tag: ${env.TAG_NAME}"
                            def services = AFFECTED_SERVICES.split(' ')
                            for (service in services) {
                                if (VALID_SERVICES.contains(service)) {
                                    def shortName = service.replaceFirst('spring-petclinic-', '')
                                    echo "Updating ${shortName} for staging deployment"
                                    sh """
                                        cd k8s
                                        digest=\$(docker inspect --format='{{index .RepoDigests 0}}' ${env.DOCKER_REGISTRY}/${service}:${env.TAG_NAME} | cut -d'@' -f2)
                                        echo "Digest for ${shortName}: \$digest"
                                        sed -i "s/^imageTag: .*/imageTag: \\&tag ${env.TAG_NAME}/" environments/staging-values.yaml
                                        sed -i "/${shortName}:/,/digest:/ s/digest: .*/digest: \$digest/" environments/staging-values.yaml
                                    """
                                }
                                
                                if (MONITORING_SERVICES.contains(service)) {
                                    echo "Updating ${service} for staging deployment"
                                    sh """
                                        cd k8s
                                        digest=\$(docker inspect --format='{{index .RepoDigests 0}}' ${env.DOCKER_REGISTRY}/${service}:${env.TAG_NAME} | cut -d'@' -f2)
                                        echo "Digest for ${service}: \$digest"
                                        sed -i "s/^imageTag: .*/imageTag: \\&tag ${env.TAG_NAME}/" environments/staging-values.yaml
                                        sed -i "/${service}:/,/digest:/ s/digest: .*/digest: \$digest/" environments/staging-values.yaml
                                    """
                                }
                            }
                            echo "Deploying all services to staging at tag ${env.TAG_NAME}"
                        }
                        shouldDeploy = true
                        
                    } else if (env.BRANCH_NAME && env.BRANCH_NAME.startsWith('develop')) {
                        echo "Deploying to dev from branch ${env.BRANCH_NAME}"
                        
                        actualBranch = env.BRANCH_NAME
                        AFFECTED_SERVICES.split(' ').each { fullName ->
                            def shortCommit = env.GIT_COMMIT.take(7)
                            if (VALID_SERVICES.contains(fullName)) {
                                def shortName = fullName.replaceFirst('spring-petclinic-', '')
                                sh """
                                    cd k8s
                                    sed -i '/${shortName}:/{n;n;s/tag:.*/tag: ${shortCommit}/}' environments/dev-values.yaml
                                """
                                echo "Updated tag for ${shortName} to ${shortCommit}"
                            }
                            
                            if (MONITORING_SERVICES.contains(fullName)) {
                                sh """
                                    cd k8s
                                    sed -i '/${fullName}:/{n;n;s/tag:.*/tag: ${shortCommit}/}' environments/dev-values.yaml
                                """
                                echo "Updated tag for ${fullName} to ${shortCommit}"
                            }
                        }
                        
                        COMMIT_MSG = "Deploy to dev for branch ${env.BRANCH_NAME} with commit ${env.GIT_COMMIT.take(7)}"
                        shouldDeploy = true
                        
                    } else {
                        actualBranch = env.BRANCH_NAME ?: 'unknown'
                        echo "Push from branch ${actualBranch}, manual deploy required"
                        shouldDeploy = false
                    }

                    if (shouldDeploy) {
                        sh """
                            cd k8s
                            git add .
                            git commit -m "${COMMIT_MSG}"
                            git push origin main
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
            echo "Workspace cleaned"
            
            sh "docker system prune -af"
            sh "docker logout"
            echo "Docker cleanup and logout completed"
        }
    }
}