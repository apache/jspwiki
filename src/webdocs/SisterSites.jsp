<%@ page import="java.util.*,com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="java.text.*" %>
<%@ page import="com.ecyrd.jspwiki.rss.*" %>
<%@ page import="com.ecyrd.jspwiki.util.*" %>
<%@ taglib uri="http://www.opensymphony.com/oscache" prefix="oscache" %>
<%!
    Logger log = LoggerFactory.getLogger("JSPWiki");
%>
<%
    /*
     *  This JSP creates support for the SisterSites standard,
     *  as specified by http://usemod.com/cgi-bin/mb.pl?SisterSitesImplementationGuide
     *
     *  FIXME: Does not honor the ACL's on the pages.
     */
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, "rss" );
    
    Set allPages = wiki.getReferenceManager().findCreated();
    
    response.setContentType("text/plain; charset=UTF-8");
    for( Iterator i = allPages.iterator(); i.hasNext(); )
    {
        String pageName = (String)i.next();
        
        // Let's not add attachments.
        // TODO: This is a kludge and not forward-compatible.
        
        if( pageName.indexOf("/") != -1 ) continue; 
        String url = wiki.getViewURL( pageName );
        
        out.write( url + " " + pageName + "\n" );
    }
 %>