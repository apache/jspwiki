<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.Calendar,java.util.Date" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.WikiProvider" %>
<!-- %@ page errorPage="/Error.jsp" % -->
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
    String verstr  = request.getParameter("version");
    int    version = WikiProvider.LATEST_VERSION;

    if( verstr != null )
    {
        version = Integer.parseInt(verstr);    
    }

    if( pagereq == null )
    {
        throw new ServletException("No page defined");
    }

    String skin = wiki.getTemplateDir();

    NDC.push( wiki.getApplicationName()+":"+pagereq );    

    WikiPage wikipage = wiki.getPage( pagereq, version );

    if( wikipage == null )
    {
        wikipage = new WikiPage( pagereq );
    }

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    wikiContext.setRequestContext( WikiContext.LOGIN );
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    String pageurl = wiki.encodeName( pagereq );

    // action is login or logout
    String action = request.getParameter("action");
    String uid = request.getParameter("uid");
    String passwd = request.getParameter("passwd");

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    if( action == null )
    {
        action = "logout";
    }
    if( action.equals( "login" ) )
    {
        wiki.login( uid, passwd, session );
    }
    else if( action.equals( "logout" ) )
    {
        wiki.logout( session );
    }

    // Direct back to the page the login box was used on.
    response.sendRedirect(wiki.getBaseURL()+"Wiki.jsp?page="+pageurl);
%>


<%
    NDC.pop();
    NDC.remove();
%>
