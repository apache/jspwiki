<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
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
    String pagereq = "UserPreferences";
    String    skin = wiki.getTemplateDir();
    String headerTitle = "";

    NDC.push( wiki.getApplicationName()+":"+pagereq );
    
    WikiPage wikipage = new WikiPage( pagereq );

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    wikiContext.setRequestContext("prefs");
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    String ok = request.getParameter("ok");
    String clear = request.getParameter("clear");

    if( ok != null || "save".equals(request.getParameter("action")) )
    {
        String name = wiki.safeGetParameter( request, "username" );

        wiki.logout( pageContext.getSession() );
        UserProfile profile = new UserProfile();
        profile.setName( TranslatorReader.cleanLink(name) );
        Cookie prefs = new Cookie( WikiEngine.PREFS_COOKIE_NAME, 
                                   profile.getStringRepresentation() );
        prefs.setMaxAge( 90*24*60*60 ); // 90 days is default.

        response.addCookie( prefs );

        response.sendRedirect( wiki.getBaseURL()+"UserPreferences.jsp" );
    }
    else if( clear != null )
    {
        Cookie prefs = new Cookie( WikiEngine.PREFS_COOKIE_NAME, "" );
        prefs.setMaxAge( 0 );
        response.addCookie( prefs );

        response.sendRedirect( wiki.getBaseURL()+"UserPreferences.jsp" );
    }       
    else
    {
        response.setContentType("text/html; charset="+wiki.getContentEncoding() );
        String contentPage = "templates/"+skin+"/ViewTemplate.jsp";
%>

        <wiki:Include page="<%=contentPage%>" />

<%
    } // Else
    NDC.pop();
    NDC.remove();
%>

