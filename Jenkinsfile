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

buildRepo = 'https://github.com/apache/jspwiki'
siteRepo = 'https://gitbox.apache.org/repos/asf/jspwiki-site.git'
creds = '9b041bd0-aea9-4498-a576-9eeb771411dd'

asfsite = 'asf-site'
build = 'build'
buildJdk8 = 'jdk_1.8_latest'
buildJdk11 = 'jdk_11_latest'
buildJdk17 = 'jdk_17_latest'
buildMvn = 'maven_3_latest'
errMsg = ''
jbake = 'jbake'

try {
    def pom

    stage( "build source" ) {
        parallel jdk11Build: {
            node( 'ubuntu' ) {
                stage( buildJdk11 ) {
                    cleanWs()
                    dir( build ) {
                        git url: buildRepo, poll: true
                        if( env.BRANCH_NAME == 'master' ) {
                            buildJSPWiki( '-Pattach-additional-artifacts -Djdk.javadoc.doclet.version=2.0.15' )
                            pom = readMavenPom file: 'pom.xml'
                            writeFile file: 'target/classes/apidocs.txt', text: 'file created in order to allow aggregated javadoc generation, target/classes is needed for all modules'
                            writeFile file: 'jspwiki-it-tests/target/classes/apidocs.txt', text: 'file created in order to allow aggregated javadoc generation, target/classes is needed for all modules'
                            withMaven( jdk: buildJdk11, maven: buildMvn, publisherStrategy: 'EXPLICIT' ) {
                                sh 'mvn package javadoc:aggregate-no-fork -DskipTests -pl !jspwiki-portable -Djdk.javadoc.doclet.version=2.0.15'
                                sh 'java -cp jspwiki-main/target/classes org.apache.wiki.TranslationsCheck site'
                            }
                        } else {
                            buildJSPWiki()
                        }
                    }
                }

                stage( 'build website' ) {
                    if( env.BRANCH_NAME == 'master' ) {
                        withMaven( jdk: buildJdk8, maven: buildMvn, publisherStrategy: 'EXPLICIT' ) {
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
            }
        },
        jdk8Build: {
            reducedBuildWith( buildJdk8 )
        },
        jdk17Build: {
            reducedBuildWith( buildJdk17 )
        }
    }

    stage( 'publish website' ) {
        if( env.BRANCH_NAME == 'master' ) {
            node( 'git-websites' ) {
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
    }

    currentBuild.result = 'SUCCESS'

} catch( Exception err ) {
    currentBuild.result = 'FAILURE'
    echo err.message
    errMsg = '- ' + err.message
} finally {
    node( 'ubuntu' ) {
        if( currentBuild.result == null ) {
            currentBuild.result = 'ABORTED'
        }
        if( env.BRANCH_NAME == 'master' ) {
            emailext body: "See ${env.BUILD_URL} $errMsg",
                     replyTo: 'dev@jspwiki.apache.org',
                     to: 'commits@jspwiki.apache.org',
                     subject: "[${env.JOB_NAME}] build ${env.BUILD_DISPLAY_NAME} - ${currentBuild.result}"
        }
    }
}

def buildJSPWiki( buildOpts = '' ) {
    withMaven( jdk: buildJdk11, maven: buildMvn, publisherStrategy: 'EXPLICIT', options: [ jacocoPublisher(), junitPublisher() ] ) {
        withCredentials( [ string( credentialsId: 'sonarcloud-jspwiki', variable: 'SONAR_TOKEN' ) ] ) {
            def sonarOptions = "-Dsonar.projectKey=jspwiki-builder -Dsonar.organization=apache -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$SONAR_TOKEN"
            echo 'Will use SonarQube instance at https://sonarcloud.io'
            sh "mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent package org.jacoco:jacoco-maven-plugin:report sonar:sonar $sonarOptions $buildOpts -T 1C"
        }
    }
}

def reducedBuildWith( jdk ) {
    node( 'ubuntu' ) {
        stage( jdk ) {
            cleanWs()
            git url: buildRepo, poll: true
            withMaven( jdk: jdk, maven: buildMvn, publisherStrategy: 'EXPLICIT', options: [] ) {
                sh 'mvn clean package -T 1C'
            }
        }
    }
}