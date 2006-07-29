<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="org.apache.commons.lang.time.StopWatch" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Logger log = Logger.getLogger("JSPWiki"); 
    WikiEngine wiki;

%>

<%
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW );
    if(!wikiContext.hasAccess( response )) return;
    String pagereq = wikiContext.getName();

    // Redirect if the request was for a 'special page'
    String redirect = wiki.getRedirectURL( wikiContext );
    if( redirect != null )
    {
        response.sendRedirect( redirect );
        return;
    }
    
    StopWatch sw = new StopWatch();
    sw.start();
    
    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );

%><wiki:Include page="<%=contentPage%>" /><%
    sw.stop();
    if( log.isDebugEnabled() ) log.debug("Total response time from server on page "+pagereq+": "+sw);
%>

