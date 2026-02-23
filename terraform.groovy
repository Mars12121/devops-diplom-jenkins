pipeline {
    agent any

    environment {
        REPO_URL = 'https://github.com/Mars12121/devops-diplom-terraform.git'
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
                    string(credentialsId: 'authorized_key', variable: 'authorized_key'),
                    string(credentialsId: 'id_ed25519', variable: 'id_ed25519')
                ]) {
                    script {                   
                        sh """
                        echo  $access_key > infra/access_key
                        echo  $secret_key > infra/secret_key
                        echo  $cloud_id > infra/cloud_id
                        echo  $folder_id > infra/folder_id
                        echo  $authorized_key > infra/authorized_key.json
                        echo  $id_ed25519 > infra/id_ed25519.pub
                        """
                    }
                }
            }
        }

        stage('Tofu Init') {
            steps {
                dir('infra') {
                    sh 'tofu init'
                }
            }
        }

        stage('Tofu Plan') {
            steps {
                dir('infra') {

                    sh 'tofu plan  -out=tfplan'
                }
            }
        }

        // stage('Tofu Apply') {
        //     steps {
        //         dir('infra') {
        //             sh 'tofu apply -auto-approve tfplan'
        //         }
        //     }
        // }
    }

}