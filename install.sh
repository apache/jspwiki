#!/bin/sh
#
#  A very simple installer script which restarts tomcat as well.
#

if [ "$1" = "public" ]
then
    echo "Installing in public"
    TOMCAT_HOME=/p/web/tomcat/current/
else
    echo "Installing in private"
    TOMCAT_HOME=$HOME/jakarta-tomcat-3.2.2
fi

ant war

rm -rf $TOMCAT_HOME/webapps/JSPWiki
cp /tmp/jalkanen/JSPWiki/*.war $TOMCAT_HOME/webapps

$TOMCAT_HOME/bin/shutdown.sh
$TOMCAT_HOME/bin/startup.sh
