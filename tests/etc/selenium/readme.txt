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

This file is tests/etc/selenium/tests/readme.txt

You can run Selenium web unit tests in one of two ways:

1) Automatically, using Ant

2) Manually, using Selenium IDE (a Firefox plugin)

RUNNING SELENIUM TESTS AUTOMATICALLY
------------------------------------

Here's a quick start to selenium web unit tests

    * Type 'ant webtests' from the command line
    
    * You may find it useful to restart Tomcat before running webtests

RUNNING SELENIUM TESTS IN FIREFOX
---------------------------------

Start Tomcat and deploy the test-custom WAR.

Start the Selenium server...

java -jar tests/lib/selenium-server-1.0b2.jar -interactive

Open Firefox.
Set the browser proxy to localhost:4444

Open URL:
chrome://selenium-ide/content/selenium/TestRunner.html?test=file:///Users/arj/workspace/JSPWiki-SVN/tests/build/selenium/test-custom/JSPWikiTestSuite.html&baseURL=http://localhost:8080

CREATING TESTS
--------------
Selenium-IDE is the preferred way to create test files. The master
test suite file is tests/etc/selenium/tests/TestSuite.html.

You can add a new test file (such as one recorded in Selenium-IDE) to
the suite file by appending a table row like this:

        <tr><td><a href="./JSPWikiTestAnonymousView.html">JSPWikiTestAnonymousView</a></td></tr>

Add the new test file (in this case, JSPWikiTestAnonymousView.html) to the test
directory (tests/etc/selenium/tests).

However, simply adding the file to the suite is NOT enough. To make it run
for all of the five sample webapp contexts we test (test-custom, test-container, etc),
you MUST add a short preamble to each test file so that the Selenium-RC
test runner starts at the correct URL. Here is that preamble:

    <tr><td>store</td>	<td>/@selenium.context@</td>	<td>baseUrl</td></tr>
    <tr><td>open</td>	<td>$${baseUrl}/Wiki.jsp?page=Main</td>	<td>&nbsp;</td></tr>

The first line is important: at build time (that is, when the Ant webtests
target executes), the @selenium.context@ variable will be replaced with the
correct test context (e.g., test-custom). You MUST ensure that the first
Selenese 'open' command (such as the one shown in the second line, above)
opens to the correct context. Setting, then using, the ${baseUrl} property
is the recommended way.

To put it simply: just make sure you add somethign similar to the two lines shown above
to the top of your test, and you should be good to go.

You need to keep in mind two other issues:
1) Creating unique resources for each test
2) Restoring the correct session state at the end of your test

Web unit tests should NOT make any assumptions about what wiki pages, users and groups
are present in the test context, other than these:
Users: janne, admin, user
Groups: Admin
Pages: Main

If you need to run tests that create, delete  or rename users, groups or pages,
you should always write tests that use unique names for those resources.
The best way to do this is to store a unique value in a variable, then
use the value of that variable in your script. Generating unique values is easy:

	<tr><td>store</td>	<td>javascript{'Test-group-'+new Date().getTime()}</td>	<td>group</td></tr>

(from JSPWikiTestCreateGroupWikiName.html)

Second, you should make sure that a particular unit test doesn't hose the next one, and 
restore the user session to an anonymous state. Make sure your unit test includes
the following Selenese at the end of the test script:

    <!-- Log out -->
    <tr><td>clickAndWait</td>	<td>link=Log out</td> 	<td>&nbsp;</td></tr>


BUILD NOTES
-----------
