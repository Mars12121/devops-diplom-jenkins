pipeline {
    agent any
    triggers {
        GenericTrigger(
            genericVariables: [
                [
                    key: 'fullJson', 
                    value: '$'
                ]
            ],
            token: 'my_secret_token',
            causeString: 'Triggered by GitHub',
            printPostContent: true 
        )
    }
    stages {
        stage('Hello') {
            steps {
                echo "${fullJson}"
            }
        }
    }
}