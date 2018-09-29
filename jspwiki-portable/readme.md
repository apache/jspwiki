```
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
```

# 1. Introduction

## 1.1 What Is Inside?

This project builds a ready-to-use JSP Wiki distribution for your personal note taking

* Based on embedded Tomcat servlet engine with minimal memory foot-print 
* Using HTTP port 9627 to avoid conflicts with existing servers running on port 80 and/or 8080
* Provides native launchers for Windows and Mac OS X (in addition to batch file and shell script)
* Can be run from an USB stick (`Wiki On A Stick`) or be installed on your PC

## 1.2 Pre-Configured Wikis

The installation provided two pre-configured JSPWikis

### 1.2.1 Personal Wiki

This configuration makes the following assumptions

* You are the one and only user of JSPWiki
* You don't need to authenticate for working with JSPWiki
* Using *BasicSearchProvider* reg-exp grepping
* No page versioning
* Registered users with admin rights
    * "user", "user" 
* Works nicely with Dropbox as document storage

### 1.2.2 Department Wiki

This configuration makes the following assumptions

* A group of mostly trust-worthy people are using the wiki
* Anonymous users have with read-only access
* Using *VersioningFileProvider*
* *LuceneSearchProvider* only picks up changes through JSPWiki
* Registered users with read/write access
    * "admin", "lEtMeIn"
    * "user", "user" 

# 2. Internals

## 2.1 Creating The Native Launchers

The native launchers are under version control and can be re-created manually. In other words there are not automatically build because

* Considering the complex setup I'm glad that it works on the JSPWiki committer boxes
* Downloading all the stuff is time-consuming and would slow the build for everyone
* There should be some manual testing before promoting the native launchers

### 2.1.1 Creating The Windows Launcher

Run the following commands

```
jspwiki-portable> mvn clean package

jspwiki-portable> ant woas:download-launch4j-for-mac woas:create-windows-app -Djspwiki.tomcat.version=X.Y.Z
Buildfile: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/build.xml

woas:download-launch4j-for-mac:
    [mkdir] Created dir: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/launch4j
      [get] Getting: http://netassist.dl.sourceforge.net/project/launch4j/launch4j-3/3.12/launch4j-3.12-macosx-x86.tgz
      [get] To: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/launch4j/launch4j-3.12-macosx-x86.tgz
    [untar] Expanding: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/launch4j/launch4j-3.12-macosx-x86.tgz into /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack
   [delete] Deleting: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/launch4j/launch4j-3.12-macosx-x86.tgz

woas:create-windows-app:
 [launch4j] Compiling resources
 [launch4j] Linking
 [launch4j] Wrapping
 [launch4j] Successfully created /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/woas/woas.exe
     [echo] Created /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/woas/woas.exe ...

BUILD SUCCESSFUL
```
The generated "woas.exe" can be copied manually to *jspwiki-portable/src/overlay/launchers/tomcat/woas.exe* after manual testing.

note: on windows platforms, use `ant woas:download-launch4j-for-win woas:create-windows-app -Djspwiki.tomcat.version=X.Y.Z` to create the launcher.

### 2.1.2 Creating The Mac OS X Launcher

Run the following commands

```
jspwiki-portable> mvn clean package

jspwiki-portable> ant woas:download-appbundler-for-mac woas:mac-app-oracle-jdk -Djspwiki.tomcat.version=X.Y.Z
Buildfile: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/build.xml

woas:download-appbundler-for-mac:
    [mkdir] Created dir: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/appbundler
      [get] Getting: http://search.maven.org/remotecontent?filepath=com/panayotis/appbundler/1.1.0/appbundler-1.1.0.jar
      [get] To: /Users/sgoeschl/work/asf/jspwiki/trunk/jspwiki/jspwiki-portable/target/unpack/appbundler/appbundler-1.1.0.jar

woas:mac-app-oracle-jdk:
     [echo] Building Mac OS X launcher for Oracle JDK
[bundleapp] Creating app bundle: woas

BUILD SUCCESSFUL
```
The generated "woas.app" can be copied manually to *jspwiki-portable/src/overlay/launchers/tomcat/woas.app* after manual testing.

note: on windows platforms, use `ant woas:download-appbundler-for-mac woas:download-launch4j-for-mac woas:create-mac-app -Djspwiki.tomcat.version=X.Y.Z`

## 2.2. Current State

### 2.2.1 Mac OS X

* Supporting a modern Oracle JDK is done using the [Oracle's AppBundler Task](http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/packagingAppsForMac.html)
* When executing `woas.app` we get a dock icon bouncing for some time so there is something slightly wrong with the MacOS X integration
* When executing `woas.app` JSPWiki does not pick up the installation directory but accesses your `~/temp` and `~.log`

### 2.2.2 Jetty vesus Tomcat

Over the time Jetty's memory foot-print become larger and larger so I moved back to an embedded Tomcat
