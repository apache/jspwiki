@ECHO OFF

REM  Licensed to the Apache Software Foundation (ASF) under one or more
REM  contributor license agreements.  See the NOTICE file distributed with
REM  this work for additional information regarding copyright ownership.
REM  The ASF licenses this file to You under the Apache License, Version 2.0
REM  (the "License"); you may not use this file except in compliance with
REM  the License.  You may obtain a copy of the License at
REM 
REM      http://www.apache.org/licenses/LICENSE-2.0
REM 
REM  Unless required by applicable law or agreed to in writing, software
REM  distributed under the License is distributed on an "AS IS" BASIS,
REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM  See the License for the specific language governing permissions and
REM  limitations under the License.

REM %~dp0 is expanded pathname of the current script under NT

SET JSPWIKI_HOME=%~dp0

SET CATALINA_HOME=%JSPWIKI_HOME%
SET CATALINA_BASE=%JSPWIKI_HOME%
SET CATALINA_OUT=%CATALINA_BASE%/logs/catalina.out
SET CATALINA_TMPDIR=%CATALINA_BASE%/temp
SET CATALINA_OPTS="-Xmx128m"

cd %JSPWIKI_HOME%
./bin/catalina.bat %1 %2 %3