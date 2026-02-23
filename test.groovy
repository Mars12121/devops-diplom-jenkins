pipeline {
    agent any
    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'COMMITTER', value: '$.pusher.name']
            ],
            token: 'my_secret_token',
            causeString: 'Triggered by GitHub commit from $COMMITTER'
        )
    }
    stages {
        stage('Hello') {
            steps {
                echo 'Hello world from GitHub Jenkinsfile!'
            }
        }
    }
}
