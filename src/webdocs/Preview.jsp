<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.auth.AccessRuleSet" %>
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
    String pagereq = wiki.safeGetParameter( request, "page" );

    String skin = wiki.getTemplateDir();

    if( pagereq == null )
    {
        throw new ServletException("No page defined");
    }

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    WikiPage wikipage = wiki.getPage( pagereq );

    if( wikipage == null )
    {
        wikipage = new WikiPage( pagereq );
    }

    // To prevent manual URL typing..
    AccessRuleSet accessRules = wikipage.getAccessRules();
    UserProfile userProfile = wiki.getUserProfile( request );
    if( accessRules.hasReadAccess( userProfile ) == false )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "<h4>Unable to preview " + pagereq + ".</h4>\n" );
        buf.append( "You do not have sufficient privileges to view this page.\n" );
        buf.append( "Have you logged in?\n" );
        throw new WikiSecurityException( buf.toString() );
    }

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    wikiContext.setRequestContext( WikiContext.PREVIEW );
    wikiContext.setHttpRequest( request );

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    pageContext.setAttribute( "usertext",
                              wiki.safeGetParameter( request, "text" ),
                              PageContext.REQUEST_SCOPE );

    String contentPage = "templates/"+skin+"/ViewTemplate.jsp";
%>
<wiki:Include page="<%=contentPage%>" />
<%
    NDC.pop();
    NDC.remove();
%>

