# 
#    JSPWiki - a JSP-based WikiWiki clone.
#
#    Licensed to the Apache Software Foundation (ASF) under one
#    or more contributor license agreements.  See the NOTICE file
#    distributed with this work for additional information
#    regarding copyright ownership.  The ASF licenses this file
#    to you under the Apache License, Version 2.0 (the
#    "License"); you may not use this file except in compliance
#    with the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing,
#    software distributed under the License is distributed on an
#    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#    KIND, either express or implied.  See the License for the
#    specific language governing permissions and limitations
#    under the License.  
#
############################################################################

#!/bin/sh
#
#  A very simple installer script which restarts tomcat as well.
#
#  This is just a sample.
#

if [ "$1" = "public" ]
then
    echo "Installing in public"
    TOMCAT_HOME=/p/web/tomcat/current/
    private=0
else
    echo "Installing in private"
    TOMCAT_HOME=$HOME/jakarta-tomcat-3.2.2
    private=1
fi

$TOMCAT_HOME/bin/shutdown.sh

ant clean
ant war

rm -rf $TOMCAT_HOME/webapps/JSPWiki
USER=`whoami`
cp /tmp/$USER/JSPWiki/install/*.war $TOMCAT_HOME/webapps

cd $TOMCAT_HOME/webapps
rm -rf JSPWiki
mkdir JSPWiki
cd JSPWiki
jar xf ../JSPWiki.war

#
#  Copy private things.
#
if [ $private -eq 1 ]
then
    cp -v $HOME/Projects/JSPWiki/jspwiki.properties $TOMCAT_HOME/webapps/JSPWiki/WEB-INF/
fi

$TOMCAT_HOME/bin/startup.sh
