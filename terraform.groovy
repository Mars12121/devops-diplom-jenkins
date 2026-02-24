pipeline {
    agent any

    triggers {
        GenericTrigger(
            token: 'diplom_terraform_token',
            causeString: 'Triggered by GitHub',
        )
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
                url: "https://github.com/Mars12121/devops-diplom-terraform.git"
            ]]
        ])
            }
        }

        stage('Prepare Tofu Credentials') {
            steps {
                withCredentials([
                    string(credentialsId: 'access_key', variable: 'access_key'),
                    string(credentialsId: 'secret_key', variable: 'secret_key'),
                    string(credentialsId: 'cloud_id', variable: 'cloud_id'),
                    string(credentialsId: 'folder_id', variable: 'folder_id'),
                    file(credentialsId: 'authorized_key', variable: 'authorized_key'),
                    file(credentialsId: 'id_ed25519', variable: 'id_ed25519')
                    
                ]) {
                    script {                   
                        sh """
                        echo -n "$access_key" > terraform/infra/access_key
                        echo -n "$secret_key" > terraform/infra/secret_key
                        echo -n "$cloud_id" > terraform/infra/cloud_id
                        echo -n "$folder_id" > terraform/infra/folder_id
                        cp $authorized_key terraform/infra/authorized_key.json
                        cp $id_ed25519 terraform/infra/id_ed25519.pub
                        """
                    }
                }
            }
        }

        stage('Tofu Init') {
            steps {
                dir('terraform/infra') {
                    sh 'tofu init'
                }
            }
        }

        stage('Tofu Plan') {
            steps {
                dir('terraform/infra') {

                    sh 'tofu plan'
                }
            }
        }
        stage('Approval') {
            steps {
                script {
                    input message: "Применить изменения в инфраструктуре?", ok: "Apply!"
                }
            }
        }

        stage('Tofu Apply') {
            steps {
                dir('terraform/infra') {
                    sh 'tofu apply -auto-approve'
                }
            }
        }
    }
    post {
        always {
              cleanWs()
            }
        }
}