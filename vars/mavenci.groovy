def execute() {
    def branchName = validate.getBranchName()
    //boolean allStagesPassed = true;

    println 'run maven ci'

    stage('compile') {
        try{
            env.JENKINS_STAGE = env.STAGE_NAME
            echo env.JENKINS_STAGE
            sh './mvnwkk clean compile -e'
        }catch (Exception e){
            executeError(e)
        }

    }
    stage('unitTest') {
        try{
            env.JENKINS_STAGE = env.STAGE_NAME
            echo env.JENKINS_STAGE
            sh './mvnw clean test -e'
        }catch (Exception e){
            executeError(e)
        }
    }
    stage('jar') {
        try{
            env.JENKINS_STAGE = env.STAGE_NAME
            echo env.JENKINS_STAGE
            sh './mvnw clean package -e'
        }catch (Exception e){
            executeError(e)
        }

    }
    stage('sonar') {
        try{
            env.JENKINS_STAGE = env.STAGE_NAME
            echo env.JENKINS_STAGE
            withSonarQubeEnv(installationName: 'sonar-server') {
                sh './mvnw org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar'
            }
        }catch (Exception e){
            executeError(e)
        }
    }

    stage("Quality Gate"){
        try{
            sleep(10)
            timeout(time: 15, unit: 'MINUTES') { // Just in case something goes wrong, pipeline will be killed after a timeout
                def qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
                echo "Status: ${qg.status}"
                if (qg.status != 'OK') {
                    env.ERROR_MESSAGE = "Pipeline aborted due to quality gate failure: ${qg.status}"
                    error env.ERROR_MESSAGE
                }
            }
        }catch (Exception e){
            executeError(e)
        }
    }


    stage('nexusUpload') {
        try{
            env.JENKINS_STAGE = env.STAGE_NAME
            echo env.JENKINS_STAGE
            //imagino que las versiones de esto deben ser progresivas?
            //se crea en nexus repositorio mavenci-repo
            nexusPublisher nexusInstanceId: 'nexus', nexusRepositoryId: 'mavenci-repo', 
            packages: [[$class: 'MavenPackage', mavenAssetList: [[classifier: '', extension: 'jar', filePath: 'build/DevOpsUsach2020-0.0.1.jar']], 
            mavenCoordinate: [artifactId: 'DevOpsUsach2020', groupId: 'com.devopsusach2020', packaging: 'jar', version: '0.0.1']]]
        }catch (Exception e){
            executeError(e)
        }
    }

    if (branchName == 'develop') {
        stage('gitCreateRelease') {
            try {
                env.JENKINS_STAGE = env.STAGE_NAME
                echo env.JENKINS_STAGE
            }catch (Exception e){
                executeError(e)
            }
        }
    }

}

def executeError(e) {
    //Error para slack
    def message = env.ERROR_MESSAGE == '' || env.ERROR_MESSAGE == null ? "[Stage ${env.JENKINS_STAGE}] Pipeline aborted due stage failure" : env.ERROR_MESSAGE 
    error message
    //error para output del pipeline mas detallado
    //throw new Exception("${env.ERROR_MESSAGE} ${e.toString()}");
    echo e.toString()
}

return this