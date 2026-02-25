pipeline {
    agent any
    
    environment {
        REGISTRY = "cr.yandex/crplg5rlmq59dfl3s7if"
        IMAGE_NAME = "devops-app"
        CHART_NAME = "devops-diplom-app"
        YANDEX_CREDS = "docker_token"
    }



    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                checkout([$class: 'GitSCM', 
                         branches: [[name: '*/main']], 
                         doGenerateSubmoduleConfigurations: false, 
                         extensions: [], 
                         submoduleCfg: [], 
                         userRemoteConfigs: [[
                        credentialsId: 'github', 
                        url: 'https://github.com/Mars12121/devops-diplom-app'
            ]]
        ])
            }
        }

        stage('Bump Version') {
            steps {
                script {
                    sh "pybump bump --file ${CHART_NAME}/Chart.yaml --level patch"
                  
                    env.NEW_VERSION = sh(script: "pybump get --file ${CHART_NAME}/Chart.yaml", returnStdout: true).trim()
                    echo "New version: ${env.NEW_VERSION}"
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    def fullImageName = "${REGISTRY}/${IMAGE_NAME}:${env.NEW_VERSION}"
                    
                withCredentials([
                    string(credentialsId: YANDEX_CREDS, variable: 'docker_token')                    
                ]) {
                        sh "echo -n $docker_token | docker login --username oauth --password-stdin cr.yandex"
                        sh "docker build -t ${fullImageName} ."
                        sh "docker push ${fullImageName}"
                    }
                }
            }
        }

        // stage('Helm Deploy') {
        //     steps {
        //         sh """
        //         helm upgrade --install ${CHART_NAME} $ \
        //             --set image.tag=${env.NEW_VERSION} \
        //             --namespace 
        //         """
        //     }
        // }
        
        stage('Git Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: "github", passwordVariable: 'GIT_PASS', usernameVariable: 'GIT_USER')]) {
                    sh """
                        git checkout -b "main"
                        git config user.name "Морозов Александр"
                        git config user.email "sanchez12121@mail.ru"
                        git config credential.helper '!f() { echo "username=${GIT_USER}\\npassword=${GIT_PASS}"; }; f'
                        git add .
                        git commit -m "Version up ${env.NEW_VERSION}"
                        git push HEAD:main
                    """
                }
            }
        }
    }
}