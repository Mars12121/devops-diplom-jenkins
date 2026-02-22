pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                echo 'Hello world from GitHub Jenkinsfile!'
            }
        }
    }
}