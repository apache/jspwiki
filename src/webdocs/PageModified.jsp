<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.Calendar,java.util.Date" %>
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

    String usertext = wiki.safeGetParameter( request, "text" );

    WikiPage wikipage = wiki.getPage( pagereq );

    // This doesn't happen usually, but in case someone types the PageModified URL in directly:
    AccessRuleSet accessRules = wikipage.getAccessRules();
    UserProfile userProfile = wiki.getUserProfile( request );
    
    if( accessRules.hasReadAccess( userProfile ) == false )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "<h4>Unable to view differences for " + pagereq + ".</h4>\n" );
        buf.append( "You do not have sufficient privileges to view this page.\n" );
        buf.append( "Have you logged in?\n" );
        throw new WikiSecurityException( buf.toString() );
    }

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    wikiContext.setRequestContext( WikiContext.CONFLICT );
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    usertext = TextUtil.replaceString( usertext, "<", "&lt;" );
    usertext = TextUtil.replaceString( usertext, ">", "&gt;" );
    usertext = TextUtil.replaceString( usertext, "\n", "<BR />" );

    pageContext.setAttribute( "usertext",
                              usertext,
                              PageContext.REQUEST_SCOPE );
    
    String conflicttext = wiki.getText(pagereq);

    conflicttext = TextUtil.replaceString( conflicttext, "<", "&lt;" );
    conflicttext = TextUtil.replaceString( conflicttext, ">", "&gt;" );
    conflicttext = TextUtil.replaceString( conflicttext, "\n", "<BR />" );

    pageContext.setAttribute( "conflicttext",
                              conflicttext,
                              PageContext.REQUEST_SCOPE );

    log.info("Page concurrently modified "+pagereq);

    String contentPage = "templates/"+skin+"/ViewTemplate.jsp";
%>

<wiki:Include page="<%=contentPage%>" />

<%
    NDC.pop();
    NDC.remove();
%>
