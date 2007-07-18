#!/bin/sh
#
# This script runs the WebUnit tests for JSPWiki.  You should tweak
# it according to your own setup.  You can also look on all the things
# that the webtests expect to be running.
#
# Couple of notes:
#
# This script is destructive in the sense that it will restart your
# tomcat, and also remove configuration files.
#
# Make sure that your junit.jar can be found in the classpath.
# The last resort is to drop it in your $JAVA_HOME/lib/ext, or on OSX
# /Library/Java/External/
#
# This is tuned for Tomcat 5.5.  Any previous/later version may have
# unintended consequences.
#

export CATALINA_HOME=${HOME}/Java/tomcat-webtest

${CATALINA_HOME}/bin/shutdown.sh

sleep 5

rm -rf ${CATALINA_HOME}/webapps/test*
rm -rf ${CATALINA_HOME}/conf/Catalina/localhost/test*

${CATALINA_HOME}/bin/startup.sh

ant -Dbuild.properties=build.properties -find build.xml webtests