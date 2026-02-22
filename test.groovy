pipeline {
    agent any
    triggers {
        githubPush() 
    }
    stages {
        stage('Hello') {
            steps {
                echo 'Hello world from GitHub Jenkinsfile!'
            }
        }
    }
}