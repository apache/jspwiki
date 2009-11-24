#!/bin/sh
#
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
####################################################################################
#
#  This script imports a JSPWiki repository exported with the export.sh command
#  into the given WikiSpace.
#
#  Currently tested only on OSX, but should run well on Linux/BSD/Solaris. Windows
#  counterpart needed!
#

# FIXME: This should use some sort of an algorithm to figure out the home
JSPWIKI_HOME=${PWD}

# First one is for a source installation, the latter is for binary installation
# One of these should work, but not both.
LIBSRC=${JSPWIKI_HOME}/src/WebContent/
LIBBIN=${JSPWIKI_HOME}/
LIBCMP=${JSPWIKI_HOME}/build/classes/
CLASSESSRC=${JSPWIKI_HOME}/src/WebContent/WEB-INF/classes/
ETC=${JSPWIKI_HOME}/etc

CLASSPATH="${LIBCMP}:${LIBSRC}:${LIBBIN}:${LIBSRC}/WEB-INF/classes:${ETC}"

for i in ${LIBSRC}/WEB-INF/lib/*.jar
do
	CLASSPATH=${CLASSPATH}:$i
done

for i in ${LIBBIN}/WEB-INF/lib/*.jar
do
	CLASSPATH=${CLASSPATH}:$i
done

#echo $CLASSPATH

#
#  OK, now run JSPWiki.
#
java -classpath ${CLASSPATH} org.apache.wiki.util.Import $@

