# Apache JSPWiki 2.12 - Documentation

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  

The license file can be found in LICENSE.


## What is JSPWiki?

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


JSPWiki is a simple (well, not anymore) WikiWiki clone, written in Java
and JSP.  A WikiWiki is a website which allows anyone to participate
in its development.  JSPWiki supports all the traditional wiki features,
as well as very detailed access control and security integration using JAAS. 

* For more information see https://jspwiki-wiki.apache.org/

## Pre-requirements

Okay, so you wanna Wiki?  You'll need the following things:

REQUIRED:

* A JSP engine that supports Servlet API 3.1.  We recommend [Apache Tomcat](https://tomcat.apache.org/)
  for a really easy installation. Tomcat 9.x or later is recommended, although Tomcat 8.x 
  is supported too; see [additional configuration](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Getting%20Started#section-Getting+Started-Tomcat8.x) 
  that must be set up in order to run JSPWiki on Tomcat 8.x.

* Some previous administration experience...  If you've ever installed
  Apache or any other web server, you should be pretty well off.

* And of course, a server to run the JSP engine on.

* JDK 11+


OPTIONAL:

* JavaMail package from java.sun.com, if you want to use log4j mailing
  capabilities.  You'll also need the Java Activation Framework.

## Really simple installation

This section is meant for you, if you just want to have a really quick
install without many worries.  If you want to have a more complicated
installation (with more power as to how to do things), 
check out the "Install" section below.

Since JSPWiki 2.1.153, JSPWiki comes with a really simple installation
engine.  Just do the following:

1) Install Tomcat from https://tomcat.apache.org/ (or any other servlet
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

## Using the Docker image

_**Docker images are not official ASF releases but provided for convenience. 
Recommended usage is always to build the source.**_

The Apache JSPWiki Docker image is available at [Docker Hub](https://registry.hub.docker.com/r/apache/jspwiki/).

### Get the Image
```
$ docker pull apache/jspwiki
```

### Running the Container
```
$ docker run -d -p 8080:8080 --name jspwiki apache/jspwiki
```

Then point your browser at http://localhost:8080/, that should give you a working 
wiki right away!

See https://jspwiki-wiki.apache.org/Wiki.jsp?page=Docker for customizations and 
advanced usage of the image.

## Upgrading from previous versions

Please read [ReleaseNotes](./ReleaseNotes) and the [UPGRADING](./UPGRADING) documents available with this
distribution.

## Contact

Questions can be asked to JSPWiki team members and fellow users via the jspwiki-users
mailing list: See https://jspwiki.apache.org/community/mailing_lists.html.
Please use the user mailing list instead of contacting team members directly, 
and as this is a public list stored in public archives, be sure to avoid including
any sensitive information (passwords, data, etc.) in your questions.
