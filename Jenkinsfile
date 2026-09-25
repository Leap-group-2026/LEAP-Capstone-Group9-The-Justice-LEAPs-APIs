pipeline{
    agent any
    tools {
        maven 'Maven-3.9.16'  
    }
    stages{
        stage('Checkout'){
            steps{
                echo 'Checkout stage placeholder'
            }
        }
        stage('Build'){
            steps{
                sh 'mvn clean install'
            }
        }
        stage('Test'){
            steps{
                sh 'mvn test'
            }
        }
        stage('Start Application'){
            steps{
                sh 'mvn spring-boot:run &'
                sh 'sleep 15'
                echo 'Spring Boot application started successfully'
            }
        }
        stage('Stop Application'){
            steps{
                sh 'pkill -f "spring-boot" || true'
                echo 'Spring Boot application stopped'
            }
        }
        stage('Archive'){
            steps{
                echo 'Archive stage placeholder'
            }
        }
    }
}