!!! How to include this plugin with your jspwiki deployment (using maven)

If you're building out your infrastructure, maven can help.

You probably have a project that includes the jspwiki war file as a dependency.

All that's needed to add a dependency for this library.

	<dependency>
		<groupId>org.apache.jspwiki</groupId>
		<artifactId>jspwiki-plugins-chartist</artifactId>
		<version>LATEST</version>
	</dependency>

Then build as normal.

!!! How to include this plugin with your jspwiki deployment (without maven)

Download the jspwiki chartist-plugin jar file.
Add it to ./webapps/jspwiki/WEB-INF/lib, replace "jspwiki" with your context path.
Restart the server.

!!! Removing the plugin from your jspwiki deployment.

Just delete the file from ./WEB-INF/lib/jspwiki-plugins-chartist-VERSION.jar 
and restart the server.

!!! Upgrading

New version? just replace the old jar file with the new one, then restart the server.

!!! Usage

Edit a wiki page. Then include the chartist plugin. Only one instance is required per wiki page.

	[{org.apache.jspwiki.plugins.chartist.ChartistPlugin}]

Then insert the chart anywhere you need it. Copy inbetween example-start and example-end.
Note: when copy and pasting, the plain text editor will auto wrap the text in {{{ and }}}. 
You'll need to remove those. In addition, the plugin reference may get an extra '['. Remove that too.

example-start

%%chartist-line {
  high: 15,
  low: -5
}
|| Monday || Tuesday || Wednesday || Thursday || Friday
| 12| 9 | 7  | 8 |  5
| 2 | 1 | 3.5| 7 |  3
| 1 | 3 | 4  | 5 |  6 
/%

example-end

Then save the page. Hopefully a chart will render. If it does not, check the browser console for potential clues.

!!! Maintenance notes

Currently, this is at chartist v1.5.0.
sourced Dec 2025 by pulling the webjar, then extracting
the umd.js and css files and placing in the src/main/resources/META-INF/chartist-plugin

The webjar, at this point in time, has a directory with a "." as a path, which is strange.
This caused the java compiled to to complain, otherwise the webjar is the way to go.


