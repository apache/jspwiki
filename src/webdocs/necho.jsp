<?xml version="1.0" encoding="UTF-8"?>
<!--
    THIS FEED IS ENTIRELY EXPERIMENTAL.  DO NOT USE FOR ANYTHING REAL YET!

    This feed is based on the 01-Jul-2003 snapshot of the (n)echo
    development, which is probably badly outdated.

    See http://intertwingly.net/stories/2003/07/01/example.necho    
-->
<%@ page import="java.util.*,com.ecyrd.jspwiki.*" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="java.text.*" %>
<%@ page import="com.ecyrd.jspwiki.rss.*" %>
<%!
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    Category log = Category.getInstance("JSPWiki");
    WikiEngine wiki;
%>

<%
    WikiContext wikiContext = wiki.createContext( request, "rss" );
    WikiPage    wikipage    = wikiContext.getPage();
    
    NDC.push( wiki.getApplicationName()+":"+wikipage.getName() );    

    response.setContentType("text/xml; charset=UTF-8" );

    StringBuffer result = new StringBuffer();
    SimpleDateFormat iso8601fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    Properties properties = wiki.getWikiProperties();
    String channelDescription = wiki.getRequiredProperty( properties, RSSGenerator.PROP_CHANNEL_DESCRIPTION );
    String channelLanguage    = wiki.getRequiredProperty( properties, RSSGenerator.PROP_CHANNEL_LANGUAGE );
%>
<feed xmlns="http://example.com/newformat#" version="0.1" 
  xml:lang="<%=channelLanguage%>" xml:base="http://example.com"> 

  <title><%=wiki.getApplicationName()%></title>
  <subtitle>FIXME: We support no subtitles here</subtitle>
  <link><%=wiki.getBaseURL()%></link>
<%
    //
    //  Now, list items.
    //

    com.ecyrd.jspwiki.plugin.WeblogPlugin plug = new com.ecyrd.jspwiki.plugin.WeblogPlugin();
    List changed = plug.findBlogEntries(wiki.getPageManager(), 
                                        wikipage.getName(),
                                        new Date(0L),
                                        new Date());

    Collections.sort( changed, new PageTimeComparator() );
%>

<%
        int items = 0;
        for( Iterator i = changed.iterator(); i.hasNext() && items < 15; items++ )
        {
            WikiPage p = (WikiPage) i.next();

            String encodedName = wiki.encodeName(p.getName());

            String url = wiki.getViewURL(p.getName());

            out.println(" <entry>");

            out.println("  <title>");

            String pageText = wiki.getText(p.getName());
            String title = "";
            int firstLine = pageText.indexOf('\n');

            if( firstLine > 0 )
            {
                title = pageText.substring( 0, firstLine );
            }
            
            if( title.trim().length() == 0 ) title = p.getName();

            // Remove wiki formatting
            while( title.startsWith("!") ) title = title.substring(1);

            out.println( RSSGenerator.format(title) );
            out.println("</title>");

            out.println("<link>");
            out.println( url );
            out.println("</link>");

            out.println("<content type=\"text/html\" mode=\"xml\">");
            out.print("<![CDATA[");

            if( firstLine > 0 )
            {
                int maxlen = pageText.length();
                if( maxlen > 1000 ) maxlen = 1000; // Assume 112 bytes of growth.

                if( maxlen > 0 )
                {
                    pageText = wiki.textToHTML( wikiContext, 
                                                pageText.substring( firstLine+1,
                                                                    maxlen ).trim() );
                    out.print( pageText );
                    if( maxlen == 1000 ) out.print( "..." );
                }
                else
                {
                    out.print( RSSGenerator.format(title) );
                }
            }
            else
            {
                out.print( RSSGenerator.format(title) );
            }

            out.print("]]>");
            out.println("</content>");

            //
            //  Modification date.
            //
            out.println("<created>");
            Calendar cal = Calendar.getInstance();
            cal.setTime( p.getLastModified() );
            cal.add( Calendar.MILLISECOND, 
                     - (cal.get( Calendar.ZONE_OFFSET ) + 
                        (cal.getTimeZone().inDaylightTime( p.getLastModified() ) ? cal.get( Calendar.DST_OFFSET ) : 0 )) );
            out.println( iso8601fmt.format( cal.getTime() ) );
            out.println("</created>\n");

            String author = p.getAuthor();
            if( author == null ) author = "unknown";

            //
            //  Author.
            //
            out.println("  <author>");
            out.println("   <name>"+author+"</name>");
            if( wiki.pageExists(author) )
            {
                out.println("<homepage>"+wiki.getViewURL(author)+"</homepage>");
            }
            out.println("  </author>\n");

            //
            //  Unique id.  FIXME: is not really a GUID.
            //
            out.println("<id>"+url+"</id>");

            out.println(" </entry>\n");
        }
%>

</feed>

<%
    NDC.pop();
    NDC.remove();
%>
