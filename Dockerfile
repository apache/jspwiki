#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.

FROM maven:3.6-jdk-8 as package

WORKDIR /tmp

COPY . .

RUN set -x \
# fastest, minimum build
  && mvn clean package -pl jspwiki-war,jspwiki-wikipages/en -am -DskipTests

FROM tomcat:8.5

COPY --from=package /tmp/jspwiki-war/target/JSPWiki.war /tmp
COPY --from=package /tmp/jspwiki-wikipages/en/target/jspwiki-wikipages-en-*.zip /tmp
COPY docker-files/log4j.properties /tmp
COPY docker-files/tomcat-users.xml $CATALINA_HOME/conf/tomcat-users.xml

#
# set default environment entries to configure jspwiki
ENV CATALINA_OPTS -Djava.security.egd=file:/dev/./urandom
ENV LANG en_US.UTF-8
ENV jspwiki_basicAttachmentProvider_storageDir /var/jspwiki/pages
ENV jspwiki_fileSystemProvider_pageDir /var/jspwiki/pages
ENV jspwiki_jspwiki_frontPage Main
ENV jspwiki_pageProvider VersioningFileProvider
ENV jspwiki_use_external_logconfig true
ENV jspwiki_workDir /var/jspwiki/work
ENV jspwiki_xmlUserDatabaseFile /var/jspwiki/etc/userdatabase.xml
ENV jspwiki_xmlGroupDatabaseFile /var/jspwiki/etc/groupdatabase.xml

RUN set -x \
 && export DEBIAN_FRONTEND=noninteractive \
 && apt install --fix-missing --quiet --yes unzip

#
# install jspwiki
RUN set -x \
 && mkdir /var/jspwiki \
# remove default tomcat applications, we dont need them to run jspwiki
 && cd $CATALINA_HOME/webapps \
 && rm -rf examples host-manager manager docs ROOT \
# remove other stuff we don't need
 && rm -rf /usr/local/tomcat/bin/*.bat \
# create subdirectories where all jspwiki stuff will live
 && cd /var/jspwiki \
 && mkdir pages logs etc work \
# deploy jspwiki
 && mkdir $CATALINA_HOME/webapps/ROOT \
 && unzip -q -d $CATALINA_HOME/webapps/ROOT /tmp/JSPWiki.war \
 && rm /tmp/JSPWiki.war \
# deploy wiki pages
 && cd /tmp/ \
 && unzip -q jspwiki-wikipages-en-*.zip \
 && mv jspwiki-wikipages-en-*/* /var/jspwiki/pages/ \
 && rm -rf jspwiki-wikipages-en-* \
# move the userdatabase.xml and groupdatabase.xml to /var/jspwiki/etc
 && cd $CATALINA_HOME/webapps/ROOT/WEB-INF \
 && mv userdatabase.xml groupdatabase.xml /var/jspwiki/etc \
# arrange proper logging (jspwiki.use.external.logconfig = true needs to be set)
 && mv /tmp/log4j.properties $CATALINA_HOME/lib/log4j.properties

# make port visible in metadata
EXPOSE 8080

#
# by default we start the Tomcat container when the docker container is started.
CMD ["/usr/local/tomcat/bin/catalina.sh","run", ">/usr/local/tomcat/logs/catalina.out"]
