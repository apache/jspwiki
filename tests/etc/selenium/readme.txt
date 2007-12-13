This file is tests/etc/selenium/tests/readme.txt

Here's a quick start to selenium web unit tests

    * Type 'ant webtests' from the command line
    
    * You may find it useful to restart Tomcat before running webtests

	
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
