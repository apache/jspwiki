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
buildJdk8 = 'jdk_1.8_latest'
buildJdk11 = 'jdk_11_latest'
buildJdk17 = 'jdk_17_latest'
buildMvn = 'maven_3_latest'
errMsg = ''

try {

    stage( "build source" ) {
        parallel jdk11Build: {
            buildAndSonarWith( buildJdk11 )
        },
        jdk8Build: {
            buildWith( buildJdk8 )
        },
        jdk17Build: {
            buildWith( buildJdk17 )
        }
    }

    if( env.BRANCH_NAME == 'master' ) {
        build wait: false, job: 'JSPWiki/site', parameters: [ text( name: 'version', value: 'master' ) ]
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

def buildAndSonarWith( jdk ) {
    node( 'ubuntu' ) {
        stage( jdk ) {
            cleanWs()
            git url: buildRepo, poll: true
            withMaven( jdk: jdk, maven: buildMvn, publisherStrategy: 'EXPLICIT', options: [ jacocoPublisher(), junitPublisher() ] ) {
                withCredentials( [ string( credentialsId: 'sonarcloud-jspwiki', variable: 'SONAR_TOKEN' ) ] ) {
                    def sonarOptions = "-Dsonar.projectKey=jspwiki-builder -Dsonar.organization=apache -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$SONAR_TOKEN"
                    echo 'Will use SonarQube instance at https://sonarcloud.io'
                    sh "mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent package org.jacoco:jacoco-maven-plugin:report sonar:sonar $sonarOptions -T 1C"
                }
            }
        }
    }
}

def buildWith( jdk ) {
    node( 'ubuntu' ) {
        stage( jdk ) {
            cleanWs()
            git url: buildRepo, poll: true
            withMaven( jdk: jdk, maven: buildMvn, publisherStrategy: 'EXPLICIT' ) {
                sh 'mvn clean package -T 1C'
            }
        }
    }
}