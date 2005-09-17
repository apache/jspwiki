<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.WikiProvider" %>
<%@ page import="com.ecyrd.jspwiki.auth.AuthorizationManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.*" %>
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
    WikiContext wikiContext = wiki.createContext( request, 
                                                  WikiContext.DELETE );
    String pagereq = wikiContext.getPage().getName();

    NDC.push( wiki.getApplicationName()+":"+pagereq );    

    WikiPage wikipage      = wikiContext.getPage();
    WikiPage latestversion = wiki.getPage( pagereq );

    String delete = wiki.safeGetParameter( request, "delete" );
    String deleteall = wiki.safeGetParameter( request, "delete-all" );

    if( latestversion == null )
    {
        latestversion = wikiContext.getPage();
    }

    AuthorizationManager mgr = wiki.getAuthorizationManager();
    Principal currentUser  = wikiContext.getCurrentUser();
    Permission requiredPermission = new PagePermission( wikipage, "delete" );

    if( !mgr.checkPermission( wikiContext.getWikiSession(),
                              requiredPermission ) )
    {
        log.info("User "+currentUser.getName()+" has no access - redirecting to login page.");
        String pageurl = wiki.encodeName( pagereq );
        response.sendRedirect( wiki.getBaseURL()+"Login.jsp?page="+pageurl );
        return;
    }

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    //
    //  Set the response type before we branch.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    if( deleteall != null )
    {
        log.info("Deleting page "+pagereq+". User="+request.getRemoteUser()+", host="+request.getRemoteAddr() );

        wiki.deletePage( pagereq );
        response.sendRedirect(wiki.getViewURL(pagereq));
        return;
    }
    else if( delete != null )
    {
        log.info("Deleting a range of pages from "+pagereq);
        
        for( Enumeration params = request.getParameterNames(); params.hasMoreElements(); )
        {
            String paramName = (String)params.nextElement();
            
            if( paramName.startsWith("delver") )
            {
                int version = Integer.parseInt( paramName.substring(7) );
                
                WikiPage p = wiki.getPage( pagereq, version );
                
                log.debug("Deleting version "+version);
                wiki.deleteVersion( p );
            }
        }
        
        response.sendRedirect(wiki.getURL( WikiContext.INFO, pagereq, null, false ));
        return; 
    }

    // FIXME: not so.
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "EditTemplate.jsp" );
%>

<wiki:Include page="<%=contentPage%>" />

<%
    NDC.pop();
    NDC.remove();
%>
