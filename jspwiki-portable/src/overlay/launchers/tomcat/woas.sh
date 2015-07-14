#!/bin/sh 
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

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
# export CATALINA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000 -Xmx128m"

# invoke the Tomcat start script from the JSPWIKI_HOME
# in order to use relative path names

cd ${JSPWIKI_HOME}
./bin/catalina.sh "$@"