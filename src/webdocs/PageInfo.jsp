<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="java.security.Permission" %>
<%@ page import="java.security.Principal" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.PagePermission" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.WikiPermission" %>
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
    WikiContext wikiContext = wiki.createContext( request, WikiContext.INFO );
    String pagereq = wikiContext.getPage().getName();

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    AuthenticationManager authMgr = wiki.getAuthenticationManager();
    AuthorizationManager mgr = wiki.getAuthorizationManager();
    Principal currentUser  = wikiContext.getWikiSession().getUserPrincipal();
    Permission requiredPermission = new PagePermission( pagereq, "view" );

    if( !mgr.checkPermission( wikiContext,
                              requiredPermission ) )
    {
        if( authMgr.strictLogins() )
        {
            log.info("User "+currentUser.getName()+" has no access - redirecting to login page.");
            String pageurl = wiki.encodeName( pagereq );
            response.sendRedirect( wiki.getBaseURL()+"Login.jsp?page="+pageurl );
            return;
        }
        else
        {
            log.info("User "+currentUser.getName()+" has no access - displaying message.");
            response.sendRedirect( wiki.getViewURL("LoginError") );
        }
    }

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    if( log.isDebugEnabled() ) 
    {
        log.debug("Page info request for page '"+pagereq+"' from "+request.getRemoteAddr()+" by "+request.getRemoteUser() );
    }

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );
%>

<wiki:Include page="<%=contentPage%>" />

<%
    NDC.pop();
    NDC.remove();
%>


