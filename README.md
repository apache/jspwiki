# Apache JSPWiki 2.11 - Documentation

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

The license file can be found in LICENSE.


## What is JSPWiki?

JSPWiki is a simple (well, not any more) WikiWiki clone, written in Java
and JSP.  A WikiWiki is a web site which allows anyone to participate
in its development.  JSPWiki supports all the traditional wiki features,
as well as very detailed access control and security integration using JAAS. 

* For more information see https://jspwiki-wiki.apache.org/

## Pre-requirements

Okay, so you wanna Wiki?  You'll need the following things:

REQUIRED:

* A JSP engine that supports Servlet API 3.1.  We recommend Tomcat from
  http://tomcat.apache.org/ for a really easy installation.
  Tomcat 8.x or later is supported.

* Some previous administration experience...  If you've ever installed
  Apache or any other web server, you should be pretty well off.

* And of course, a server to run the JSP engine on.

* JDK 8+


OPTIONAL:

* JavaMail package from java.sun.com, if you want to use log4j mailing
  capabilities.  You'll also need the Java Activation Framework.

## Really simple installation

This section is meant for you, if you just want to have a really quick
install without much worries.  If you want to have a more complicated
installation (with more power as to how to do things), 
check out the "Install" section below.

Since JSPWiki 2.1.153, JSPWiki comes with a really simple installation
engine.  Just do the following:

1) Install Tomcat from http://tomcat.apache.org/ (or any other servlet
   container)

2) Rename the JSPWiki.war file from the download and rename it based on
   your desired URL (if you want it different from /JSPWiki).  For example,
   if you want your URL to be http://.../wiki, rename it to wiki.war.
   This name will be referred to as <appname> below.
   Place this WAR in your `$TOMCAT_HOME/webapps` folder and then start Tomcat.

3) Point your browser at http://&lt;myhost>/&lt;appname>/Install.jsp

4) Answer a couple of simple questions

5) Restart your container

6) Point your browser to http://&lt;myhost>/&lt;appname>/

That's it!


## Advanced Installation

In the `$TOMCAT_HOME/lib` folder (or equivalent based on your servlet container),
place a `jspwiki-custom.properties` file, which can contain any overrides to the 
default `ini/jspwiki.properties` file in the JSPWiki JAR.  For any values not 
placed in `jspwiki-custom.properties` file JSPWiki will rely on the default file.
Review the default file to look for values you may wish to override in the custom
file.  Some common values to override in your custom file include 
`jspwiki.xmlUserDatabaseFile`, `jspwiki.xmlGroupDatabaseFile`, 
`jspwiki.fileSystemProvider.pageDir`, `jspwiki.basicAttachmentProvider.storageDir`, 
and `log4j.appender.FileLog.File`.  The comments in the default file will suggest 
appropriate values to override them with. 

The custom file can also be placed in the `WEB-INF/` folder of the WAR, but storing
this file in `$TOMCAT_HOME/lib` allows you to upgrade the JSPWiki WAR without needing
to re-insert your customizations.

Unzip the contents of `jspwiki-corepages.zip` into your newly created
directory.  You can find the rest of the documentation in the
`JSPWiki-doc.zip` file.

(Re)start tomcat.

Point your browser at http://&lt;where your Tomcat is installed>/MyWiki/.
You should see the Main Wiki page.  See the next section if you want
to edit the pages =).

The `WEB-INF/jspwiki.policy` file is used to change access permissions for 
the Wiki.

Check the Apache JSPWiki website and project documentation for additional
setup and configuration suggestions.

## Upgrading from previous versions

Please read ReleaseNotes and the UPGRADING document available with this
distribution.

## Contact

Questions can be asked to JSPWiki team members and fellow users via the jspwiki-users
mailing list: See http://jspwiki.apache.org/community/mailing_lists.html.
Please use the user mailing list instead of contacting team members directly, 
and as this is a public list stored in public archives, be sure to avoid including
any sensitive information (passwords, data, etc.) in your questions.

