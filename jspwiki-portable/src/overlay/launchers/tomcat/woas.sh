#!/bin/sh 

#############################################################################
# Helper script to run JSPWiki
#############################################################################

# uncomment for remote debugging

# export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"

# get the directory where the script is located

export JSPWIKI_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# overwrite the various CATALINA properties to play safe with a developer box

export CATALINA_HOME=${JSPWIKI_HOME}
export CATALINA_BASE=${JSPWIKI_HOME}
export CATALINA_OUT=${CATALINA_BASE}/logs/catalina.out
export CATALINA_TMPDIR=${CATALINA_BASE}/temp
export CATALINA_OPTS="-Xmx128m"

# invoke the Tomcat start script from the JSPWIKI_HOME
# in order to use relative path names

cd ${JSPWIKI_HOME}
./bin/catalina.sh "$@"