<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.auth.UserProfile" %>
<%@ page import="com.ecyrd.jspwiki.auth.UserManager" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;
%>

<%
    WikiContext wikiContext = wiki.createContext( request, WikiContext.PREFS );
    String pagereq = "UserPreferences";
    UserManager mgr = wiki.getUserManager();
    
    NDC.push( wiki.getApplicationName()+":"+pagereq );
    
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    String ok = request.getParameter("ok");
    String clear = request.getParameter("clear");

    if( ok != null || "save".equals(request.getParameter("action")) )
    {
        mgr.logout( session );
        String name = wiki.safeGetParameter( request, "username" );

        UserProfile profile = mgr.getUserProfile( TranslatorReader.cleanLink(name) );

        log.debug("Writing profile name: "+profile);
        Cookie prefs = new Cookie( WikiEngine.PREFS_COOKIE_NAME, 
                                   profile.getStringRepresentation() );
        prefs.setMaxAge( 1001*24*60*60 ); // 1001 days is default.

        response.addCookie( prefs );

        response.sendRedirect( wiki.getBaseURL()+"UserPreferences.jsp" );
    }
    else if( clear != null )
    {
        mgr.logout( session );
        Cookie prefs = new Cookie( WikiEngine.PREFS_COOKIE_NAME, "" );
        prefs.setMaxAge( 0 );
        response.addCookie( prefs );

        response.sendRedirect( wiki.getBaseURL()+"UserPreferences.jsp" );
    }       
    else
    {
        response.setContentType("text/html; charset="+wiki.getContentEncoding() );
        String contentPage = "templates/"+wikiContext.getTemplate()+"/ViewTemplate.jsp";
%>

        <wiki:Include page="<%=contentPage%>" />

<%
    } // Else
    NDC.pop();
    NDC.remove();
%>

