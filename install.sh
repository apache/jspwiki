#!/bin/sh

TOMCAT_HOME=$HOME/jakarta-tomcat-3.2.2

ant war

rm -rf $TOMCAT_HOME/webapps/JSPWiki
cp /tmp/jalkanen/JSPWiki/*.war $TOMCAT_HOME/webapps

$TOMCAT_HOME/bin/shutdown.sh
$TOMCAT_HOME/bin/startup.sh
