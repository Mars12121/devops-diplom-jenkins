pipeline {
    agent any

    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'ref', value: '$.ref']
            ],
            token: 'devops-diplom-app',
            causeString: 'Triggered by GitHub',
            printPostContent: true
        )
    }
    
    environment {
        REGISTRY = "cr.yandex/crplg5rlmq59dfl3s7if"
        IMAGE_NAME = "devops-app"
        CHART_NAME = "devops-diplom-helm"
        APP_NAME = "devops-diplom-app"
        YANDEX_CREDS = "docker_token"
        SSH_CREDS_ID = "github_ssh"
    }

    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                dir("${APP_NAME}") {
                checkout([$class: 'GitSCM', 
                    branches: [[name: '*/main']], 
                    extensions: [[$class: 'LocalBranch', localBranch: 'main']], 
                    userRemoteConfigs: [[
                        credentialsId: env.SSH_CREDS_ID, 
                        url: "git@github.com:Mars12121/devops-diplom-app.git"
                    ]]
                ])
                }
                dir("${CHART_NAME}") {
                checkout([$class: 'GitSCM', 
                    branches: [[name: '*/main']], 
                    extensions: [[$class: 'LocalBranch', localBranch: 'main']], 
                    userRemoteConfigs: [[
                        credentialsId: env.SSH_CREDS_ID, 
                        url: "git@github.com:Mars12121/devops-diplom-helm.git"
                    ]]
                ])
                }
            }
        }

        stage('Bump Version') {
            steps {
                script {
                    dir("${CHART_NAME}") {
                        if (env.ref && env.ref.startsWith("refs/tags/")) {
                            env.NEW_VERSION = env.ref.replace("refs/tags/", "")
                            echo "Using version from TAG: ${env.NEW_VERSION}"
                        } else {
                            sh "pybump bump --file Chart.yaml --level patch"
                            env.NEW_VERSION = sh(script: "pybump get --file Chart.yaml", returnStdout: true).trim()
                            echo "New version: ${env.NEW_VERSION}"
                        }
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    dir("${APP_NAME}") {
                        def fullImageName = "${REGISTRY}/${IMAGE_NAME}:${env.NEW_VERSION}"
                        
                        withCredentials([
                            string(credentialsId: YANDEX_CREDS, variable: 'docker_token')                    
                        ]) {
                                sh "echo -n $docker_token | docker login --username oauth --password-stdin cr.yandex"
                                sh "docker build --no-cache -t ${fullImageName} ."
                                sh "docker push ${fullImageName}"
                            }
                    }
                }
            }
        }
        stage('Git Push') {
            when {
                expression { return !env.ref?.startsWith("refs/tags/") }
            }
            steps {
                dir("${CHART_NAME}") {
                    sshagent([env.SSH_CREDS_ID]) {
                        sh """
                                git config user.name "Морозов Александр"
                                git config user.email "sanchez12121@mail.ru"
                                
                                git add .
                                git commit -m "Version up ${env.NEW_VERSION}"
                                git push origin main
                            """
                    }
                }
            }
        }

        stage('Helm Deploy') {
            steps {
                dir("${CHART_NAME}") {
                    withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG_FILE')]) {
                    sh """
                        export KUBECONFIG=${KUBECONFIG_FILE}
                        helm upgrade --install ${CHART_NAME} . \
                            --set image.tag=${env.NEW_VERSION} \
                            --namespace app-web \
                    """
                }    
        }
            }
        }
    }
}