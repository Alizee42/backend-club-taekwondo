pipeline {
  agent any
  options { timestamps() }
  stages {
    stage('Checkout'){ steps{ checkout scm } }
    stage('Build & Test'){
      agent {
        docker {
          image 'maven:3.9.8-eclipse-temurin-21'
          args '-v /var/jenkins_home/.m2:/root/.m2'
        }
      }
      steps { sh 'mvn -B -U clean verify' }
      post { always { junit '**/target/surefire-reports/*.xml' } }
    }
  }
}