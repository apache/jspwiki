/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

try {
    def buildRepo = 'https://github.com/apache/jspwiki'
    def siteRepo = 'https://gitbox.apache.org/repos/asf/jspwiki-site.git'
    def creds = '9b041bd0-aea9-4498-a576-9eeb771411dd'

    def asfsite = 'asf-site'
    def build = 'build'
    def jbake = 'jbake'

    node( 'ubuntu' ) {
        def JAVA_JDK_8=tool name: 'JDK 1.8 (latest)', type: 'hudson.model.JDK'
        echo "Will use Java $JAVA_JDK_8"

        def MAVEN_3_LATEST=tool name: 'Maven 3 (latest)', type: 'hudson.tasks.Maven$MavenInstallation'
        echo "Will use Maven $MAVEN_3_LATEST"

        stage( 'clean ws' ) {
            cleanWs()
        }

        stage( 'build source' ) {
            dir( build ) {
                git url: buildRepo, poll: true
                withEnv( [ "Path+JDK=$JAVA_JDK_8/bin", "Path+MAVEN=$MAVEN_3_LATEST/bin", "JAVA_HOME=$JAVA_JDK_8" ] ) {
                    withSonarQubeEnv( 'ASF Sonar Analysis' ) {
                        echo "Will use SonarQube instance at $SONAR_HOST_URL"
                        sh "mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install -Pattach-additional-artifacts $SONAR_MAVEN_GOAL"
                    }
                }
            }
        }

        stage( 'build website' ) {
            withEnv( [ "Path+JDK=$JAVA_JDK_8/bin", "Path+MAVEN=$MAVEN_3_LATEST/bin", "JAVA_HOME=$JAVA_JDK_8" ] ) {
                dir( jbake ) {
                    git branch: jbake, url: siteRepo, credentialsId: creds, poll: false
                    sh 'mvn clean process-resources'
                }
                stash name: 'jbake-website'
            }
        }
        
    }

    node( 'git-websites' ) {
        stage( 'publish website' ) {
            cleanWs()
            unstash 'jbake-website'
            dir( asfsite ) {
                git branch: asfsite, url: siteRepo, credentialsId: creds, poll: false
                sh "cp -rf ../$jbake/target/content/* ./"
                timeout( 15 ) { // 15 minutes
                    sh 'git add .'
                    sh 'git commit -m "Automatic Site Publish by Buildbot"'
                    echo "pushing to $siteRepo"
                    sh "git push origin asf-site"
                }
            }
        }
    }

    currentBuild.result = 'SUCCESS'

} catch( Exception err ) {
    currentBuild.result = 'FAILURE'
    echo err.message
} finally {
    node( 'ubuntu' ) {
        if( currentBuild.result == null ) {
            currentBuild.result = 'ABORTED'
        }
        emailext body: "See ${env.BUILD_URL}",
                 replyTo: 'dev@jspwiki.apache.org', 
                 to: 'commits@jspwiki.apache.org',
                 subject: "[${env.JOB_NAME}] build ${env.BUILD_DISPLAY_NAME} - ${currentBuild.result}"
    }
}