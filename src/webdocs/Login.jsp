<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.attachment.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<!--
    This is a sample login page, in case you prefer a clear
    front page instead of the default sign-in type login box
    at the side of the normal entry page. Set this page in
    the welcome-file-list tag in web.xml to default here 
    when entering the site.
-->


<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;
%>

<%
    WikiContext wikiContext = wiki.createContext( request, WikiContext.LOGIN );
    String pagereq = wikiContext.getPage().getName();

    NDC.push( wiki.getApplicationName() + ":Login.jsp"  );

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    String action = request.getParameter("action");
    String uid    = wiki.safeGetParameter( request,"j_username" );
    String passwd = wiki.safeGetParameter( request,"j_password" );

    AuthenticationManager mgr = wiki.getAuthenticationManager();

    session.setAttribute("msg","");

    if( !mgr.isContainerAuthenticated() && "login".equals(action) )
    {
        if( mgr.login( wikiContext.getWikiSession(), uid, passwd ) )
        {
            response.sendRedirect( wiki.getViewURL(pagereq) );
            return;
        }
        else
        {
            if( passwd.length() > 0 && passwd.toUpperCase().equals(passwd) )
            {
                session.setAttribute("msg", "Invalid login (please check your Caps Lock key)");
            }
            else
            {
                session.setAttribute("msg", "Not a valid login.");
            }
        }
    }
    else if( "logout".equals(action) )
    {
        mgr.logout( session );
        response.sendRedirect( wiki.getViewURL(pagereq) );
        return;
    }

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "LoginContent.jsp" );
%>

    <wiki:Include page="<%=contentPage%>" />

<%
    NDC.pop();
    NDC.remove();
%>
