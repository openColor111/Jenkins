pipeline {
  agent any
  stages {
    stage('build') {
      parallel {
        stage('build') {
          steps {
            echo 'mvn build'
          }
        }

        stage('build2') {
          steps {
            sh 'echo 111'
          }
        }

      }
    }

  }
}