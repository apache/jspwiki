<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.util.*" %>
<%@ page import="org.apache.commons.lang.time.StopWatch" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    Logger log = Logger.getLogger("JSPWiki"); 
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW );
    if(!wikiContext.hasAccess( response )) return;
    String pagereq = wikiContext.getName();

    // Redirect if the request was for a 'special page'
    String redirect = wiki.getWikiActionBeanFactory().getSpecialPageReference( pagereq );
    if( redirect != null )
    {
        response.sendRedirect( wikiContext.getViewURL( redirect ) );
        return;
    }
    
    StopWatch sw = new StopWatch();
    sw.start();
    WatchDog w = wiki.getCurrentWatchDog();
    try {
        w.enterState("Generating VIEW response for "+wikiContext.getPage(),60);
    
        // Set the content type and include the response content
        wikiContext.setVariable( "contentTemplate", "PageContent.jsp" );
        response.setContentType("text/html; charset="+wiki.getContentEncoding() );
        String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                                wikiContext.getTemplate(),
                                                                "ViewTemplate.jsp" );

%><wiki:Include page="<%=contentPage%>" /><%
    }
    finally
    {
        sw.stop();
        if( log.isDebugEnabled() ) log.debug("Total response time from server on page "+pagereq+": "+sw);
        w.exitState();
    }
%>

