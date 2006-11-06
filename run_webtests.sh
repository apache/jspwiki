#!/bin/sh

# Couple of notes:
#
# Make sure your CATALINA_HOME is set before you run this, and that
# your tomcat is running
# Also, make sure that your junit.jar can be found in the classpath.
# The last resort is to drop it in your $JAVA_HOME/lib/ext, or on OSX
# /Library/Java/External/

export CATALINA_HOME=${HOME}/Java/tomcat/

ant -Dbuild.properties=build.properties -find build.xml webtests