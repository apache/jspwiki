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

java -jar tests/lib/selenium-server-0.9.2-patched.jar -interactive

Open Firefox.
Set the browser proxy to localhost:4444

Open URL:
chrome://selenium-ide/content/selenium/TestRunner.html?test=file:///Users/arj/workspace/JSPWiki-SVN/tests/build/selenium/test-custom/JSPWikiTestSuite.html&baseURL=http://localhost:8080

CREATING TESTS
--------------
Selenium-IDE is the preferred way to create test files. The master
test suite file is tests/etc/selenium/tests/JSPWikiTestSuite.html.

You can add a new test file (such as one recorded in Selenium-IDE) to
the suite file by appending a table row like this:

        <tr><td><a href="./JSPWikiTestAnonymousView.html">JSPWikiTestAnonymousView</a></td></tr>

Add the new test file (in this case, JSPWikiTestAnonymousView.html) to the test
directory (tests/etc/selenium/tests).

However, simply adding the file to the suited is NOT enough. To make it run
for all of the five sample webapp contexts we test, you MUST add a short
preamble to each test file so that the Selenium-RC test runner starts at
the correct URL. Here is that preamble:

    <tr><td>store</td><td>/@selenium.context@/</td>	<td>baseUrl</td></tr>
    <tr><td>open</td>	<td>$${baseUrl}/Wiki.jsp?page=Main</td>	<td>&nbsp;</td></tr>

The first line is important: at build time (that is, when the Ant webtests
target executes), the @selenium.context@ variable will be replaced with the
correct test context. You MUST ensure that the first Selenese 'open' command
(such as the one shown in the second line, above) opens to the correct context.
Setting, then using, the ${baseUrl} property is the recommended way.


BUILD NOTES
-----------
	
ARJ notes 10-Dec-2007:	
Note: To get Selenium to work, Dirk patched the following two Selenium-core files, which were re-injected into lib/selenium-server.jar:

core/scripts/htmlutils.js

OLD:
function triggerEvent(element, eventType, canBubble, controlKeyDown, altKeyDown, shiftKeyDown, metaKeyDown) {
    canBubble = (typeof(canBubble) == undefined) ? true : canBubble;
    if (element.fireEvent) {

function triggerKeyEvent(element, eventType, keySequence, canBubble, controlKeyDown, altKeyDown, shiftKeyDown, metaKeyDown) {
    var keycode = getKeyCodeFromKeySequence(keySequence);
    canBubble = (typeof(canBubble) == undefined) ? true : canBubble;
    if (element.fireEvent) {
 
NEW:
function triggerEvent(element, eventType, canBubble, controlKeyDown, altKeyDown, shiftKeyDown, metaKeyDown) {
    canBubble = (typeof(canBubble) == undefined) ? true : canBubble;

    //if (element.fireEvent) {  
    /* support for mootools patch Dirk Frederickx Dec 07*/
	/* see http://forum.mootools.net/viewtopic.php?id=1639 */
    if (element.ownerDocument.createEventObject) {

function triggerKeyEvent(element, eventType, keySequence, canBubble, controlKeyDown, altKeyDown, shiftKeyDown, metaKeyDown) {
    var keycode = getKeyCodeFromKeySequence(keySequence);
    canBubble = (typeof(canBubble) == undefined) ? true : canBubble;

    //if (element.fireEvent) {  
    /* support for mootools patch Dirk Frederickx Dec 07*/
	/* see http://forum.mootools.net/viewtopic.php?id=1639 */
    if (element.ownerDocument.createEventObject) {

(the last line, "if (element.fireEvent" is what's replaced)


core/scripts/selenium-browserbot.js

OLD:
BrowserBot.prototype.triggerMouseEvent = function(element, eventType, canBubble, clientX, clientY) {
    clientX = clientX ? clientX : 0;
    clientY = clientY ? clientY : 0;

    LOG.debug("triggerMouseEvent assumes setting screenX and screenY to 0 is ok");
    var screenX = 0;
    var screenY = 0;

    canBubble = (typeof(canBubble) == undefined) ? true : canBubble;
    if (element.fireEvent) {

NEW:
BrowserBot.prototype.triggerMouseEvent = function(element, eventType, canBubble, clientX, clientY) {
    clientX = clientX ? clientX : 0;
    clientY = clientY ? clientY : 0;

    LOG.debug("triggerMouseEvent assumes setting screenX and screenY to 0 is ok");
    var screenX = 0;
    var screenY = 0;

    canBubble = (typeof(canBubble) == undefined) ? true : canBubble;
    //if (element.fireEvent) {  
    /* support for mootools patch Dirk Frederickx Dec 07*/
	/* see http://forum.mootools.net/viewtopic.php?id=1639 */
    if (element.ownerDocument.createEventObject) {

(the last line, "if (element.fireEvent" is what's replaced)


Commands to re-inject these files:
cd tests/etc/selenium/selenium-core-0.8.3-patches
jar -uvf ../../../../lib/selenium-server-0.9.2.jar core/scripts/selenium-browserbot.js core/scripts/htmlutils.js 
