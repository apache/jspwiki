<?xml version="1.0" encoding="UTF-8"?>

<%@ page import="java.util.*,com.ecyrd.jspwiki.*" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="java.text.*" %>
<%@ page import="com.ecyrd.jspwiki.rss.*" %>
<%@ taglib uri="/WEB-INF/oscache.tld" prefix="oscache" %>

<%!
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    Category log = Category.getInstance("JSPWiki");
    WikiEngine wiki;
    static int MAX_CHARACTERS = 4000;
%>

<%
    WikiContext wikiContext = wiki.createContext( request, "rss" );
    WikiPage    wikipage    = wikiContext.getPage();

    if( wiki.getBaseURL().length() == 0 )
    {
        response.sendError( 500, "The jspwiki.baseURL property has not been defined for this wiki - cannot generate RSS" );
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
<oscache:cache time="300">

<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         xmlns="http://purl.org/rss/1.0/"
         xmlns:dc="http://purl.org/dc/elements/1.1/"
         xmlns:wiki="http://purl.org/rss/1.0/modules/wiki/">

<channel rdf:about="<%=wiki.getBaseURL()%>">
  <title><%=wiki.getApplicationName()%></title>
  <link><%=wiki.getBaseURL()%></link>
  <description>
     <%=RSSGenerator.format(channelDescription)%>
  </description>
  <language><%=channelLanguage%></language>
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

    //  We need two lists, which is why we gotta make a separate list if
    //  we want to do just a single pass.
    StringBuffer itemBuffer = new StringBuffer();
%>
  <items>
    <rdf:Seq>
<%
        int items = 0;
        for( Iterator i = changed.iterator(); i.hasNext() && items < 15; items++ )
        {
            WikiPage p = (WikiPage) i.next();

            String encodedName = wiki.encodeName(p.getName());

            String url = wiki.getAbsoluteURL(WikiContext.VIEW,p.getName());
%>
            <rdf:li rdf:resource="<%=url%>"/>
<%
            itemBuffer.append(" <item rdf:about=\""+url+"\">\n");

            itemBuffer.append("  <title>");

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

            itemBuffer.append( RSSGenerator.format(title) );
            itemBuffer.append("</title>\n");

            itemBuffer.append("  <link>");
            itemBuffer.append( url );
            itemBuffer.append("</link>\n");

            itemBuffer.append("  <description>");

            if( firstLine > 0 )
            {
                int maxlen = pageText.length();
                if( maxlen > MAX_CHARACTERS ) maxlen = MAX_CHARACTERS;

                if( maxlen > 0 )
                {
                    wikiContext.setVariable( WikiEngine.PROP_URLSTYLE, "absolute" );
                    pageText = wiki.textToHTML( wikiContext, 
                                                pageText.substring( firstLine+1,
                                                                    maxlen ).trim() );
                    itemBuffer.append( RSSGenerator.format(pageText) );
                    if( maxlen == MAX_CHARACTERS ) itemBuffer.append( "..." );
                }
                else
                {
                    itemBuffer.append( RSSGenerator.format(title) );
                }
            }
            else
            {
                itemBuffer.append( RSSGenerator.format(title) );
            }

            itemBuffer.append("</description>\n");

            if( p.getVersion() != -1 )
            {
                itemBuffer.append("  <wiki:version>"+p.getVersion()+"</wiki:version>\n");
            }

            if( p.getVersion() > 1 )
            {
                itemBuffer.append("  <wiki:diff>"+
                                  wiki.getAbsoluteURL( WikiContext.DIFF,
                                                       p.getName(),
                                                      "r1=-1" )+
                                  "</wiki:diff>\n");
            }

            //
            //  Modification date.
            //
            itemBuffer.append("  <dc:date>");
            Calendar cal = Calendar.getInstance();
            cal.setTime( p.getLastModified() );
            cal.add( Calendar.MILLISECOND, 
                     - (cal.get( Calendar.ZONE_OFFSET ) + 
                        (cal.getTimeZone().inDaylightTime( p.getLastModified() ) ? cal.get( Calendar.DST_OFFSET ) : 0 )) );
            itemBuffer.append( iso8601fmt.format( cal.getTime() ) );
            itemBuffer.append("</dc:date>\n");

            String author = p.getAuthor();
            if( author == null ) author = "unknown";

            //
            //  Author.
            //
            itemBuffer.append("  <dc:contributor>\n");
            itemBuffer.append("   <rdf:Description");
            if( wiki.pageExists(author) )
            {
                itemBuffer.append(" link=\""+wiki.getAbsoluteURL(WikiContext.VIEW,author)+"\"");
            }
            itemBuffer.append(">\n");
            itemBuffer.append("    <rdf:value>"+author+"</rdf:value>\n");
            itemBuffer.append("   </rdf:Description>\n");
            itemBuffer.append("  </dc:contributor>\n");


            //  PageHistory

            itemBuffer.append("  <wiki:history>");
            itemBuffer.append( wiki.getAbsoluteURL( WikiContext.INFO,
                                                    p.getName() ) );
            itemBuffer.append("</wiki:history>\n");

            //  Close up.
            itemBuffer.append(" </item>\n");
        }
%>
    </rdf:Seq>
  </items>
</channel>

<%=itemBuffer%>

<%
        String searchURL = wiki.getBaseURL()+"Search.jsp";
%>
<textinput rdf:about="<%=searchURL%>">
  <title>Search</title>
  <description>Search <%=wiki.getApplicationName()%></description>
  <name>query</name>
  <link><%=searchURL%></link>
</textinput>
</rdf:RDF>
</oscache:cache>

<%
    NDC.pop();
    NDC.remove();
%>
