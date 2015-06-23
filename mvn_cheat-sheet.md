

# 1. IDE Specific

| Maven Command                                                     | Description                                                                                                               |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| mvn eclipse:eclipse                                               | generates Eclipse project files (alternatively, you could use m2e)                                                        |
| mvn idea:idea                                                     | generates IDEA IntelliJ project files                                                                                     |


# 2. Build Specific

| Maven Command                                                     | Description                                                                                                               |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| mvn clean install                                                 | performs a build                                                                                                          |
| mvn clean install -Dmaven.test.skip                               | performs a build, skipping the tests (not recommended)                                                                    |
| mvn clean test                                                    | compiles the source and executes the tests                                                                                |
| mvn test -Dtest=JSPWikiMarkupParserTest                           | run just a single test class                                                                                              |
| mvn test -Dtest=JSPWikiMarkupParserTest#testHeadingHyperlinks3    | run just a single test within a test class                                                                                |
| mvn test -Dtest=TestClassName#methodName -Dmaven.surefire.debug   | debug a test in Eclipse or IDEA to see why it's failing (see http://www.jroller.com/gmazza/entry/jpa_and_junit#debugging) |
| mvn tomcat7:run-war                                               | (from a war module) starts JSPWiki on a Tomcat7 instance at http://localhost:8080/JSPWiki                                 |
| mvnDebug -DskipTests tomcat7:run-war                              | (from a war module) starts JSPWiki with attached debugger on a Tomcat7 instance at http://localhost:8080/JSPWiki          |
| mvn clean deploy -Papache-release -Dgpg.passphrase=<passphrase>   | deploys generated artifact to a repository. If -Dgpg.passphrase is not given, expects a gpg-agent running                 |
| mvn clean install -Pintegration-tests                             | performs a build, enabling Selenium tests execution (best run from the jspwiki-it-tests folder)                           |
| mvn wro4j:run -Dminimize=true                                     | merge & compress js & css files                                                                                           |
| mvn wro4j:run -Dminimize=false                                    | only merge the js & css files  (no compression)                                                                           |
| mvn clean install -Dmaven.test.skip -Dminimize=false              | performs a build, skipping the tests and skip compression                                                                 |


# 3. Reports Specific

| Maven Command                                                     | Description                                                                                                               |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| mvn apache-rat:check                                              | creates an Apache RAT report. See: http://creadur.apache.org/rat/apache-rat-plugin/plugin-info.html                       |
| mvn cobertura:cobertura                                           | generates a cobertura maven report. See: http://mojo.codehaus.org/cobertura-maven-plugin/usage.html                       |
| mvn javadoc:javadoc                                               | creates javadocs; if graphviz binaries (www.graphviz.org) are found on $PATH, the javadocs will display some UML class/package level diagrams |
| mvn sonar:sonar                                                   | generates a Sonar report. Expects a Sonar server running at http://localhost:9000/                                        | 
| cd jspwiki-site; mvn test -Dtest=SiteGeneratorTest                | checks all language resource files for missing or unused translations                                                     |
