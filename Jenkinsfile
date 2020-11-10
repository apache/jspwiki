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
    def buildJdk = 'jdk_11_latest'
    def buildMvn = 'maven_3_latest'
    def jbake = 'jbake'
    def pom

    node( 'ubuntu' ) {
        stage( 'clean ws' ) {
            cleanWs()
        }

        stage( 'build source' ) {
            dir( build ) {
                git url: buildRepo, poll: true
                withMaven( jdk: buildJdk, maven: buildMvn, publisherStrategy: 'EXPLICIT', options: [ jacocoPublisher(), junitPublisher() ] ) {
                    withCredentials( [ string( credentialsId: 'sonarcloud-jspwiki', variable: 'SONAR_TOKEN' ) ] ) {
                        def sonarOptions = "-Dsonar.projectKey=jspwiki-builder -Dsonar.organization=apache -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$SONAR_TOKEN"
                        echo 'Will use SonarQube instance at https://sonarcloud.io'
                        sh "mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install org.jacoco:jacoco-maven-plugin:report -Pattach-additional-artifacts sonar:sonar -up $sonarOptions -Djdk.javadoc.doclet.version=2.0.12"
                    }
                    pom = readMavenPom file: 'pom.xml'
                    writeFile file: 'target/classes/apidocs.txt', text: 'file created in order to allow aggregated javadoc generation, target/classes is needed for all modules'
                    writeFile file: 'jspwiki-it-tests/target/classes/apidocs.txt', text: 'file created in order to allow aggregated javadoc generation, target/classes is needed for all modules'
                    sh 'mvn package javadoc:aggregate-no-fork -DskipTests -pl !jspwiki-portable -Djdk.javadoc.doclet.version=2.0.12'
                    sh 'java -cp jspwiki-main/target/classes org.apache.wiki.TranslationsCheck site'
                }
            }
        }

        stage( 'build website' ) {
            withMaven( jdk: 'jdk_1.8_latest', maven: buildMvn, publisherStrategy: 'EXPLICIT' ) {
                dir( jbake ) {
                    git branch: jbake, url: siteRepo, credentialsId: creds, poll: false
                    sh "cp ../$build/ChangeLog.md ./src/main/config/changelog.md"
                    sh "cp ../$build/i18n-table.txt ./src/main/config/i18n-table.md"
                    sh "cat ./src/main/config/changelog-header.txt ./src/main/config/changelog.md > ./src/main/jbake/content/development/changelog.md"
                    sh "cat ./src/main/config/i18n-header.txt ./src/main/config/i18n-table.md > ./src/main/jbake/content/development/i18n.md"
                    sh 'mvn clean process-resources -Dplugin.japicmp.jspwiki-new=' + pom.version
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
            }
            def apidocs = asfsite + '/apidocs/' + pom.version
            dir( apidocs ) {
                sh "cp -rf ../../../$build/target/site/apidocs/* ."
            }
            dir( asfsite ) {
                timeout( 15 ) { // 15 minutes
                    sh 'git add .'
                    sh 'git commit -m "Automatic Site Publish by Buildbot"'
                    echo "pushing to $siteRepo"
                    sh 'git push origin asf-site'
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
