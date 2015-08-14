<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->

# 1. Introduction

This project builds a ready-to-use JSP Wiki distribution

* Based on Tomcat 7.0.52 as servlet engine
* Using HTTP port 9627 to avoid conflicts with existing servers running on port 80 and/or 8080

# 2. Creating The Native Launchers

The native launchers are under version control and can be re-created manually. In other words there are not automatically build because

* Considering the complex setup I'm glad that it works on the JSPWiki committer boxes
* Downloading all the stuff is time-consuming and would slow the build for everyone
* There should be some manual testing before promoting the native launchers

## 2.1 Creating The Windows Launcher

Run the following commands

```
jspwiki-portable> mvn clean package

jspwiki-portable> ant woas:download-launch4j-for-mac woas:create-windows-app
Buildfile: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/build.xml

woas:download-launch4j-for-mac:
    [mkdir] Created dir: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/launch4j
      [get] Getting: http://netassist.dl.sourceforge.net/project/launch4j/launch4j-3/3.8/launch4j-3.8-macosx-x86.tgz
      [get] To: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/launch4j/launch4j-3.8-macosx-x86.tgz
    [untar] Expanding: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/launch4j/launch4j-3.8-macosx-x86.tgz into /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack
   [delete] Deleting: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/launch4j/launch4j-3.8-macosx-x86.tgz

woas:create-windows-app:
 [launch4j] Compiling resources
 [launch4j] Linking
 [launch4j] Wrapping
 [launch4j] Successfully created /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/woas/woas.exe
     [echo] Created /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/woas/woas.exe ...

BUILD SUCCESSFUL
```
The generated "woas.exe" can be copied manually to *jspwiki-portable/src/overlay/launchers/tomcat/woas.exe* after manual testing.

## 2.2 Creating The Mac OS X Launcher

Run the following commands

```
jspwiki-portable> mvn clean package

jspwiki-portable> ant woas:download-appbundler-for-mac woas:mac-app-oracle-jdk
Buildfile: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/build.xml

woas:download-appbundler-for-mac:
    [mkdir] Created dir: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/appbundler
      [get] Getting: https://java.net/downloads/appbundler/appbundler-1.0.jar
      [get] To: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/appbundler/appbundler-1.0.jar

woas:mac-app-oracle-jdk:
     [echo] Building Mac OS X launcher for Oracle JDK
[bundleapp] Creating app bundle: woas

BUILD SUCCESSFUL
```
The generated "woas.exe" can be copied manually to *jspwiki-portable/src/overlay/launchers/tomcat/woas.exe* after manual testing.

# 3. Current State

## 3.1 Mac OS X

* The Mac OS X [JarBundler](http://informagen.com/JarBundler/index.html) 2.2.0 is used to build a native Mac OS X app but it depends on having the Apple JDK installed
* Supporting a modern Oracle JDK is done using the [Oracle's AppBundler Task](http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/packagingAppsForMac.html)

## 3.2 Jetty vesus Tomcat

Over the time Jetty's memory foot-print become larger and larger so I moved back to an embedded Tomcat

# 2. Available Maven Commands

```
mvn clean package
```

# 3. The Public Wiki

To secure the "department" wiki the following accounts were created

* "admin", "lEtMeIn"
* "user", "user" 




