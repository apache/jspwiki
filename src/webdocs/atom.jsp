<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet href="<%=wiki.getBaseURL()%>atom.css" type="text/css"?>

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

    private String getFormattedDate( Date d )
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat iso8601fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        cal.setTime( d );
        cal.add( Calendar.MILLISECOND, 
                 - (cal.get( Calendar.ZONE_OFFSET ) + 
                    (cal.getTimeZone().inDaylightTime( d ) ? cal.get( Calendar.DST_OFFSET ) : 0 )) );
        return iso8601fmt.format( cal.getTime() );
    }
%>

<%
    WikiContext wikiContext = wiki.createContext( request, "rss" );
    WikiPage    wikipage    = wikiContext.getPage();
    
    if( wiki.getBaseURL().length() == 0 )
    {
        response.sendError( 500, "The jspwiki.baseURL property has not been defined for this wiki - cannot generate Atom feed" );
        return;
    }

    NDC.push( wiki.getApplicationName()+":"+wikipage.getName() );    

    response.setContentType("text/xml; charset=UTF-8" );

    StringBuffer result = new StringBuffer();
    SimpleDateFormat iso8601fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    Properties properties = wiki.getWikiProperties();
    String channelDescription = wiki.getRequiredProperty( properties, RSSGenerator.PROP_CHANNEL_DESCRIPTION );
    String channelLanguage    = wiki.getRequiredProperty( properties, RSSGenerator.PROP_CHANNEL_LANGUAGE );
%>

<feed version="0.3" xmlns="http://purl.org/atom/ns#" xml:lang="<%=wiki.getContentEncoding()%>">
  <title mode="escaped" type="text/html"><%=wiki.getApplicationName()%></title>
  <%--<tagline>FIXME: We support no subtitles here</tagline> --%>

  <link rel="alternate" href="<%=wiki.getBaseURL()%>" title="<%=wiki.getApplicationName()%>" type="text/html"/>
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

    Date    blogmodified = new Date();
    String  blogauthor   = "";

    if( changed.size() > 0 )
    { 
        blogmodified = ((WikiPage)changed.get(0)).getLastModified();
        blogauthor   = ((WikiPage)changed.get(0)).getAuthor();
    }
%>
  <modified><%=iso8601fmt.format(blogmodified)%></modified>
  <author>
     <name><%=blogauthor%></name>
  </author>

  <info mode="xml" type="text/html">
      <div xmlns="http://www.w3.org/1999/xhtml">This is an Atom formatted XML site feed. It is intended to be viewed in a Newsreader or syndicated to another site.</div>
  </info>
<%
        int items = 0;
        for( Iterator i = changed.iterator(); i.hasNext() && items < 15; items++ )
        {
            WikiPage p = (WikiPage) i.next();

            String encodedName = wiki.encodeName(p.getName());

            String url = wiki.getAbsoluteViewURL(p.getName());

            out.println(" <entry>");

            //
            //  Title
            //
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

            //
            //  Link element
            //

            out.println("<link rel=\"alternate\" type=\"text/html\" href=\""+url+"\"/>");

            //
            //  Description
            //
            out.println("<content type=\"text/html\" mode=\"escaped\" xml:base=\""+wiki.getBaseURL()+"\">");
            out.print("<![CDATA[");

            if( firstLine > 0 )
            {
                int maxlen = pageText.length();
                // if( maxlen > 1000 ) maxlen = 1000; // Assume 112 bytes of growth.

                if( maxlen > 0 )
                {
                    //
                    //  Force the TranslatorReader to output absolute URLs
                    //  regardless of the current settings.
                    //
                    wikiContext.setVariable( WikiEngine.PROP_REFSTYLE, "absolute" );

                    pageText = wiki.textToHTML( wikiContext, 
                                                pageText.substring( firstLine+1,
                                                                    maxlen ).trim() );
                    out.print( pageText );
                    // if( maxlen == 1000 ) out.print( "..." );
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
            //  Creation date.
            //
            out.print("<created>");
            WikiPage firstversion = wiki.getPage(p.getName(),1);

            out.print( getFormattedDate( firstversion.getLastModified() ) );
            out.print("</created>\n");


            //
            //  Issued date.  JSPWiki does not support drafts, so we essentially output
            //  the same date.
            //

            out.print("<issued>"+getFormattedDate(firstversion.getLastModified())+"</issued>\n");

            //
            //  Modification date.
            //
            out.print("<modified>");
            out.print( getFormattedDate(p.getLastModified()) );
            out.print("</modified>\n");

            //
            //  Author.
            //

            String author = p.getAuthor();
            if( author == null ) author = "unknown";

            out.println("  <author>");
            out.println("   <name>"+author+"</name>");
            /*
            //  This may be useful later on, once I figure out which <link>-tag to use.
            if( wiki.pageExists(author) )
            {
                out.println("<homepage>"+wiki.getAbsoluteViewURL(author)+"</homepage>");
            }
            */
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
