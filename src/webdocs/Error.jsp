<%@ page isErrorPage="true" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
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
    String skin    = wiki.safeGetParameter( request, "skin" );

    if( pagereq == null )
    {
        pagereq = wiki.getFrontPage();
    }

    if( skin == null )
    {
        skin = wiki.getTemplateDir();
    }

    NDC.push( wiki.getApplicationName() + ":" + pagereq );

    WikiContext wikiContext = new WikiContext( wiki, pagereq );
    wikiContext.setRequestContext( WikiContext.ERROR );
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT, wikiContext, PageContext.REQUEST_SCOPE );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    String msg = "An unknown error was caught by Error.jsp";
    if( exception != null )        
    {   
        msg = exception.getMessage();
        if( msg == null || msg.length() == 0 )
        {
            msg = "An unknown exception "+exception.getClass().getName()+" was caught by Error.jsp.";
        }
    }

    log.debug("Error.jsp exception is: ",exception);

    pageContext.setAttribute( "message", msg, PageContext.REQUEST_SCOPE );

    String contentPage = "templates/"+skin+"/ViewTemplate.jsp";
%>
<wiki:Include page="<%=contentPage%>" />
<%
    NDC.pop();
    NDC.remove();
%>

