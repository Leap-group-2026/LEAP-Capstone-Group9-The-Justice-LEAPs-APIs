pipeline {
  agent any
  tools {
    maven 'maven for java'
  }
  stages {
    stage('Checkout') {
      steps {
        echo 'Checkout stage placeholder'
      }
    }

    stage('Build') {
      steps {
        sh 'mvn clean install'
      }
    }

    stage('Test') {
      steps {
        sh 'mvn test'
      }
    }

    stage('SonarQube Analysis') {
      steps {
        withSonarQubeEnv('my sonar qube for project') {
          sh '''
            mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922:sonar \
              -Dsonar.projectKey=Justice-Leaps-API-scanner \
              -Dsonar.projectName="Justice LEAPs API" \
              -Dsonar.host.url=http://localhost:9000
          '''
        }
      }
    }

    stage('Quality Gate') {
      steps {
        timeout(time: 5, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
      }
    }

    stage('Start Application') {
      steps {
        sh 'mvn spring-boot:run &'
        sh 'sleep 15'
        echo 'Spring Boot application started successfully'
      }
    }

    stage('Stop Application') {
      steps {
        sh 'pkill -f "spring-boot" || true'
        echo 'Spring Boot application stopped'
      }
    }

    stage('Archive') {
      steps {
        echo 'Archive stage placeholder'
      }
    }
  }
}