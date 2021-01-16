import pipeline.*
def call() {
    pipeline {
        agent any

        stages {
            stage('Pipeline') {
                steps {
                    script {
						def tech = new test.validateTech()
                        
                        figlet 'Tecnologia: ' + tech.get()
						
                        def branchName = validate.getBranchName()

                        figlet env.GIT_BRANCH
                        println 'branch detectado ' + branchName

                        

                        switch (branchName) {
                           case ['develop', 'feature']:
                                pipelineci.execute()
                                break
                           case 'release':
                                pipelinecd.execute()
                                break
                           default: 
                                //Quizás existe una mejor forma de hacer esto
                                env.ERROR_MESSAGE = 'Nombre de branch no cumple con las convenciones de gitflow'
                                error env.ERROR_MESSAGE
                                break
                        }
                    }
                }
            }
        }

        post {
            success {
                script {
                    notification.success();
                }
            }
            failure {
                script {
                    echo 'env.ERROR_MESSAGE ='+env.ERROR_MESSAGE
                    //mensaje de error por defecto
                    if (env.ERROR_MESSAGE == '' || env.ERROR_MESSAGE == null) {
                        notification.failure();
                    }else {
                        //cuando se agrega un mensaje "personalizado"
                        notification.failure(env.ERROR_MESSAGE);
                    }
                }
            }
        }
    }
}



return this
