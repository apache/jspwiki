#!/bin/sh
#
#  A very simple installer script which restarts tomcat as well.
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

ant war

rm -rf $TOMCAT_HOME/webapps/JSPWiki
cp /tmp/jalkanen/JSPWiki/*.war $TOMCAT_HOME/webapps

cd $TOMCAT_HOME/webapps
mkdir JSPWiki
cd JSPWiki
jar xf ../JSPWiki.war

if [ $private -eq 1 ]
then
    cp -v /home/jalkanen/Projects/JSPWiki/jspwiki.properties $TOMCAT_HOME/webapps/JSPWiki/WEB-INF/
fi

$TOMCAT_HOME/bin/shutdown.sh
$TOMCAT_HOME/bin/startup.sh
