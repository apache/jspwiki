<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.WikiContext" %>
<%@ page import="com.ecyrd.jspwiki.WikiEngine" %>
<%@ page import="com.ecyrd.jspwiki.WikiPage" %>
<%@ page import="com.ecyrd.jspwiki.auth.AuthenticationManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.AuthorizationManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.UserManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.WikiSecurityException" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.WikiPermission" %>
<%@ page import="com.ecyrd.jspwiki.filters.RedirectException" %>
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
    WikiContext wikiContext = wiki.createContext( request, WikiContext.EDIT );
    AuthenticationManager mgr = wiki.getAuthenticationManager();
    AuthorizationManager authMgr = wiki.getAuthorizationManager();
    UserManager userMgr = wiki.getUserManager();
    boolean containerAuth = mgr.isContainerAuthenticated();
    boolean cookieAssertions = AuthenticationManager.allowsCookieAssertions();
    boolean isAuthenticated = wikiContext.getWikiSession().isAuthenticated();
    String user = wikiContext.getCurrentUser().getName();

    // User must be authenticated to create groups
    if( !authMgr.checkPermission( wikiContext.getWikiSession(), WikiPermission.CREATE_GROUPS ) )
    {
        log.info("User "+user+" cannot create groups - redirecting to login page.");
        String msg = "You do not seem to have the permissions for this operation. Would you like to login as another user?";
        wikiContext.setVariable( "msg", msg );
        String pageurl = wiki.encodeName( wikiContext.getPage().getName() );
        response.sendRedirect( wiki.getBaseURL()+"Login.jsp?page="+pageurl );
    }
    
    NDC.push( wiki.getApplicationName()+":"+ wikiContext.getPage().getName() );
    
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    // Init the errors list
    Set errors;
    if ( session.getAttribute( "errors" ) != null )
    {
       errors = (Set)session.getAttribute( "errors" );
    }
    else
    {
       errors = new HashSet();
       session.setAttribute( "errors", errors );
    }

    
    // Extract the group name, members and action attributes
    String ok      = request.getParameter( "ok" );
    String name    = request.getParameter( "name" );
    String members = request.getParameter( "members" );
    if ( name == null ) { name = ""; }
    name = name.trim();
    if ( members == null ) { members = user; }
    members = members.trim();
    
    if( ok != null || "save".equals(request.getParameter("action")) )
    {
        // Validate the group
        errors.clear();
        if ( name == null || name.length() < 1 ) 
        {
            errors.add("Group name may not be blank.");
        }
        if ( members == null || members.length() < 1 )
        {
            errors.add("The group must have at least one member.");
        }
        
        // If page already exists, disallow
        String groupPage = "Group" + name;
        if ( wiki.pageExists( groupPage ) )
        {
            errors.add("A group named '" + name + "' already exists. Choose another.");
        }
        
        // If no errors, build and save the group page
        if ( errors.size() > 0 )
        {
            response.sendRedirect( "NewGroup.jsp?name=" + name + "&members=" + members );
            return;
        }
        else
        {
            WikiContext groupContext = new WikiContext( wiki, request, new WikiPage( wiki, groupPage ) );
            log.info("Creating group "+groupPage+". User="+user+", host="+request.getRemoteAddr() );

            //  Set author information
            groupContext.getPage().setAuthor( user );
            log.info( groupContext.getPage().getName() );

            // Create the actual page text
            // By default, allow the members of the group to edit it.
            String text = "[{ALLOW edit " + name + "}]\n" +
                "[{SET members='" + members + "'}]\n" +
                "This is a wiki group. Edit this page to see its members.";
            try
            {
                wiki.saveText( groupContext, text );
            }
            catch( RedirectException ex )
            {
                session.setAttribute("msg", ex.getMessage());
                response.sendRedirect( ex.getRedirect() );
                return;
            }
        }
        response.sendRedirect( wiki.getBaseURL()+"Wiki.jsp" );
    }
    
    pageContext.setAttribute( "name", name, PageContext.REQUEST_SCOPE );
    pageContext.setAttribute( "members", members, PageContext.REQUEST_SCOPE );
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "GroupContent.jsp" );
%><wiki:Include page="<%=contentPage%>" /><%
    NDC.pop();
    NDC.remove();
%>
