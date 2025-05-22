pipeline {
     agent any

    tools {
        maven 'Maven-3.9.4'
        jdk 'OpenJDK-17'
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '**']],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [
                        [$class: 'CloneOption', noTags: false, depth: 0, shallow: false],
                        [$class: 'PruneStaleBranch']
                    ],
                    userRemoteConfigs: [[
                        url: 'https://github.com/kiin21/spring-petclinic-microservices.git',
                        credentialsId: 'github_pat'
                    ]]
                ])
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    // Define valid services
                    def VALID_SERVICES = [
                        'spring-petclinic-admin-server',
                        'spring-petclinic-api-gateway',
                        'spring-petclinic-config-server',
                        'spring-petclinic-customers-service',
                        'spring-petclinic-discovery-server',
                        'spring-petclinic-vets-service',
                        'spring-petclinic-visits-service',
                    ]

                    // Get the list of changed files between current commit and last successful build
                    def changedFiles = sh(
                        script: """
                            # Get the last successful commit hash or use origin/main if none
                            LAST_COMMIT=\$(git rev-parse HEAD~1 2>/dev/null || git rev-parse origin/main)

                            # Get the changed files
                            git diff --name-only \$LAST_COMMIT HEAD
                        """,
                        returnStdout: true
                    ).trim()

                    // Handle empty result
                    if (changedFiles.isEmpty()) {
                        echo "No changes detected."
                        env.AFFECTED_SERVICES = ''
                        return
                    }

                    // Determine which services were affected by the changes
                    def affectedServices = []
                    changedFiles.split("\n").each { file ->
                        VALID_SERVICES.each { service ->
                            if (file.startsWith("${service}/")) {
                                if (!affectedServices.contains(service)) {
                                    affectedServices.add(service)
                                }
                            }
                        }
                    }

                    env.AFFECTED_SERVICES = affectedServices.join(' ')

                    if (env.AFFECTED_SERVICES.isEmpty()) {
                        echo "No valid service changes detected. Skipping pipeline."
                        currentBuild.result = 'SUCCESS'
                    } else {
                        echo "Changed services: ${env.AFFECTED_SERVICES}"
                    }
                }
            }
        }

        stage('Test') {
            when {
                expression { env.AFFECTED_SERVICES != '' }
            }
            steps {
                script {
                    def services = env.AFFECTED_SERVICES.split(' ')
                    for (service in services) {
                        echo "Running tests for ${service}"
                        sh """
                            if [ -d "${service}" ]; then
                                cd ${service}
                                mvn clean verify -P springboot
                            else
                                echo "Directory ${service} does not exist!"
                            fi
                        """

                    }
                }
            }
            post {
                always {
                    script {
                        env.AFFECTED_SERVICES.split(' ').each { service ->
                            dir(service) {
                                // Store JUnit test results
                                junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

                                // Store JaCoCo coverage results
                                jacoco(
                                    execPattern: '**/target/jacoco.exec',
                                    classPattern: '**/target/classes',
                                    sourcePattern: '**/src/main/java',
                                )

                            }
                        }
                    }
                }
            }
        }

        stage('Check Coverage') {
            when {
                expression { env.AFFECTED_SERVICES != '' }
            }
            steps {
                script {
                    def services = env.AFFECTED_SERVICES.split(' ')
                    def coveragePass = true
                    // Define valid services
                    def SKIP_CHECK_COVERAGE_SERVICES = [
                        'spring-petclinic-admin-server',
                        'spring-petclinic-api-gateway',
                        'spring-petclinic-config-server',
                        'spring-petclinic-discovery-server',
                    ]
                    for (service in services) {\
                        if (service in SKIP_CHECK_COVERAGE_SERVICES) {
                            echo "Skipping coverage check for ${service}."
                            continue
                        }
                        echo "Checking coverage for ${service}..."

                        // Check if the Jacoco XML file exists before parsing
                        def jacocoFile = "${service}/target/site/jacoco/jacoco.xml"
                        if (fileExists(jacocoFile)) {
                            // Improved shell command to extract coverage properly
                            def coverage = sh(script: """
                                grep -m 1 -A 1 '<counter type="INSTRUCTION"' ${jacocoFile} |
                                grep 'missed' |
                                sed -E 's/.*missed="([0-9]+)".*covered="([0-9]+)".*/\\1 \\2/' |
                                awk '{ print (\$2/(\$1+\$2))*100 }'
                            """, returnStdout: true).trim().toFloat()

                            echo "Code Coverage for ${service}: ${coverage}%"

                            // If coverage is below 70%, mark as failed
                            if (coverage < 70) {
                                echo "Coverage for ${service} is below 70%. Build failed!"
                                coveragePass = false
                            }
                        } else {
                            echo "Jacoco report for ${service} not found. Skipping coverage check."
                            coveragePass = false
                        }
                    }

                    // Fail the build if any service's coverage is below 70%
                    if (!coveragePass) {
                        error "Test coverage is below 70% for one or more services. Build failed!"
                    }
                }
            }
        }

        stage('Build') {
            when {
                expression { AFFECTED_SERVICES != '' }
            }
            steps {
                script {
                    def services = AFFECTED_SERVICES.split(' ')
                    for (service in services) {
                        echo "Building ${service}"
                        sh """
                            cd ${service}
                            mvn clean package -DskipTests
                        """
                    }
                }
            }
        }
    }

    post {
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
