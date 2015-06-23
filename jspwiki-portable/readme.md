# 1. Introduction

This project builds a ready-to-use JSP Wiki distribution

* Based on Tomcat 7.0.52 as servlet engine
* Using HTTP port 9627 to avoid conflicts with existing servers running on port 80 and/or 8080

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

To secure the "public" wiki the following accounts were created

* "admin", "lEtMeIn"
* "user", "user" 




