pipeline {
    agent {
        node {
            label 'maven'
        }
    }

    parameters {
        // TAG_NAME：对应git仓库项目中的标签（一般记录着版本号）
        string(name:'TAG_NAME', defaultValue: '', description:'')
    }

    environment {
        // docker仓库，以用户名、密码方式可访问
        //在kubesphere界面->devops工程->凭证->创建docker->添加用户名、密码
        DOCKER_CREDENTIAL_ID = 'dockerhub-id'
        // gitee仓库，从仓库下载所需
        // 在kubesphere界面->devops工程->凭证->创建gitee
        GIT_CREDENTIALS_ID = 'gitee-id'
        // 在kubesphere界面->devops工程->凭证->创建kubeconfig->类型选择kubeconfig即可
        KUBECONFIG_CREDENTIAL_ID = 'kubeconfig-demo'
        // docker镜像仓库地址
        // 真正配置的是maven仓库私服->ip（域名）:端口
        REGISTRY = 'node1:9990'
        DOCKERHUB_NAMESPACE = 'docker'
        GITREPO_ACCOUNT = 'liangzhicheng3'
        GITREPO_EMAIL = 'yichengc3@163.com'
        APP_NAME = 'devops-demo'
        // 1.在sonarqube界面右上角->my account->security->generate tokens
        // 2.在kubesphere界面->devops工程->凭证->创建sonar-token，类型选择秘密文本，将第1步骤生成的token粘贴到密钥中
        // 3.在sonarqube界面->configuration->webhooks->创建与jenkins访问地址进行关联
        // 4.在kubesphere使用jenkins，需要在jenkins->系统管理->系统设置->配置sonarqube servers（sonarqube访问地址，sonarqube的token）
        // 配置完以上，代码审查工具才能正常使用
        SONAR_CREDENTIAL_ID = 'sonar-token'
        GIT_REPO_URL = 'git@gitee.com:liangzhicheng3/devops-demo.git'
    }

    // 操作步骤
    stages {
        // 1.下载代码
        stage ('checkout scm') {
            steps {
                checkout(scm)
            }
        }

        // 2.单元测试
        stage ('unit test') {
            steps {
                // 基于maven容器
                container ('maven') {
                    // -gs：全局指定本地项目根目录中的configuration->settings.xml
                    sh 'mvn clean -gs `pwd`/configuration/settings.xml test'
                }
            }
        }

        // 3.代码分析
        stage('sonarqube analysis') {
            steps {
                // 基于maven容器
                container ('maven') {
                    withCredentials([string(credentialsId: "$SONAR_CREDENTIAL_ID", variable: 'SONAR_TOKEN')]) {
                        withSonarQubeEnv('sonar') {
                            // sonar:sonar使用了sonar插件（sonar-maven-plugin），$SONAR_TOKEN->variable的SONAR_TOKEN对应映射到credentialsId的SONAR_CREDENTIAL_ID
                            sh "mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install -Dmaven.test.failure.ignore=true sonar:sonar -o -gs `pwd`/configuration/settings.xml -Dsonar.login=$SONAR_TOKEN"
                        }
                    }
                    // 超时机制
                    timeout(time: 1, unit: 'HOURS') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }

        // 4.构建并推送
        stage ('build & push') {
            steps {
                // 基于maven容器
                container ('maven') {
                    sh 'mvn -Dmaven.test.skip=true -gs `pwd`/configuration/settings.xml clean package'
                    // docker构建，-f指定项目根目录中的Dockerfile，-t指定target
                    // $REGISTRY->仓库地址  $DOCKERHUB_NAMESPACE->命名空间  $APP_NAME:SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER .->应用名称:版本号-分支名称-部署定义的数字  .->当前根目录中找Dockerfile
                    sh 'docker build -f Dockerfile -t $REGISTRY/$DOCKERHUB_NAMESPACE/$APP_NAME:SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER .'
                    withCredentials([usernamePassword(passwordVariable : 'DOCKER_PASSWORD' ,usernameVariable : 'DOCKER_USERNAME' ,credentialsId : "$DOCKER_CREDENTIAL_ID" ,)]) {
                        // 登录到docker私有仓库
                        sh 'echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u "$DOCKER_USERNAME" --password-stdin'
                        // 将docker镜像推送到私有仓库
                        sh 'docker push  $REGISTRY/$DOCKERHUB_NAMESPACE/$APP_NAME:SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER'
                    }
                }
            }
        }

        // 5.推送最新版本tag
        stage('push latest') {
            when{
                branch 'master'
            }
            steps {
                // 基于maven容器
                container ('maven') {
                    sh 'docker tag  $REGISTRY/$DOCKERHUB_NAMESPACE/$APP_NAME:SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER $REGISTRY/$DOCKERHUB_NAMESPACE/$APP_NAME:latest '
                    sh 'docker push  $REGISTRY/$DOCKERHUB_NAMESPACE/$APP_NAME:latest '
                }
            }
        }

        // 6.部署到开发环境
        stage('deploy to dev') {
            when{
                // 条件：分支必须是master
                branch 'master'
            }
            steps {
                // 弹出小窗口提示是否需要进行部署
                input(id: 'deploy-to-dev', message: 'deploy to dev?')
                // 期望值enableConfigSubstitution为true才是真正进行kubernetes部署
                kubernetesDeploy(configs: 'deploy/dev/**', enableConfigSubstitution: true, kubeconfigId: "$KUBECONFIG_CREDENTIAL_ID")
            }
        }

        // 7.推送对应的tag
        stage('push with tag') {
            when{
                expression{
                    // 条件：参数中必须存在TAG_NAME，TAG_NAME的值必须等于正则表达式（以v开头，*.*）
                    return params.TAG_NAME =~ /v*.*/
                }
            }
            // 表达式通过才会将tag推送到git仓库
            steps {
                // 基于maven容器
                container ('maven') {
                    input(id: 'release-image-with-tag', message: 'release image with tag?')
                    // Pull code
                    git branch: "$BRANCH_NAME", credentialsId: "$GIT_CREDENTIALS_ID", url: "$GIT_REPO_URL"
                    // Git tag and push tag
        		    // Requires to install User Build Vars plugin
        		    // https://wiki.jenkins.io/display/JENKINS/Build+User+Vars+Plugin
                    wrap([$class: 'BuildUser']) {
        		        // Requires to install SSH Agent plugin
        		  	    // http://wiki.jenkins-ci.org/display/JENKINS/SSH+Agent+Plugin
        		  	    sshagent(["$GIT_CREDENTIALS_ID"]) {
        		  		    sh """
        		  			    git config user.email "$GITREPO_EMAIL"
        		  			    git config user.name "$GITREPO_ACCOUNT"
        		  			    git tag -a $TAG_NAME -m "$TAG_NAME"
        		  			    mkdir -p ~/.ssh
        		  			    echo 'Host * \n    StrictHostKeyChecking no' > ~/.ssh/config
        		  			    git push origin $BRANCH_NAME --tags
        		  		    """
        		  	    }
                    }
                }
            }
        }

        // 8.部署到生产环境
        stage('deploy to production') {
            when {
                expression{
                    // 条件：参数中必须存在TAG_NAME，TAG_NAME的值必须等于正则表达式（以v开头，*.*）
                    return params.TAG_NAME =~ /v*.*/
                }
            }
            steps {
                // 弹出小窗口提示是否需要进行部署
                input(id: 'deploy-to-production', message: 'deploy to production?')
                // 期望值enableConfigSubstitution为true才是真正进行kubernetes部署
                kubernetesDeploy(configs: 'deploy/prod/**', enableConfigSubstitution: true, kubeconfigId: "$KUBECONFIG_CREDENTIAL_ID")
            }
        }
    }

}
