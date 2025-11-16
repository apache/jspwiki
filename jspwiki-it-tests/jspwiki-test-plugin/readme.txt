This test plugin was created for testing the newer (v3.0.0) API
changes that provide plugin discovery and the ability to get 
an example available in the UI.

In case that's not clear, after installing this plugin, then 
starting the server and editing a page, ctrl+space will show 
a snippet (refered to as a "snip" in the code) that provides
a usage example in the browser. This helps to eliminate guess
work and provides a better user experience.

To deploy this plugin:
	mvn clean install
Then 
	copy target\*.jar $JSPWIKI_HOME
Where $JSPWIKI_HOME is something like
	tomcat/webapps/jspwiki/WEB-INF/lib
Then start up the tomcat server/container that you're running.

That should be it. Enjoy.

Remember this is just a test plugin, meaning it's only to 
demonstrate the proof of concept. It really doesn't do much.