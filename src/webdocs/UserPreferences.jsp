<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.WikiContext" %>
<%@ page import="com.ecyrd.jspwiki.WikiSession" %>
<%@ page import="com.ecyrd.jspwiki.WikiEngine" %>
<%@ page import="com.ecyrd.jspwiki.auth.NoSuchPrincipalException" %>
<%@ page import="com.ecyrd.jspwiki.auth.AuthenticationManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.AuthorizationManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.UserManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.WikiSecurityException" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.WikiPermission" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.DuplicateUserException" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.UserProfile" %>
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
    WikiContext wikiContext = wiki.createContext( request, WikiContext.PREFS );
    WikiSession wikiSession = wikiContext.getWikiSession();
    AuthenticationManager mgr = wiki.getAuthenticationManager();
    AuthorizationManager authMgr = wiki.getAuthorizationManager();
    UserManager userMgr = wiki.getUserManager();
    boolean containerAuth = mgr.isContainerAuthenticated();
    boolean cookieAssertions = AuthenticationManager.allowsCookieAssertions();
    boolean isAuthenticated = wikiContext.getWikiSession().isAuthenticated();
    boolean canSavePrefs = authMgr.checkPermission( wikiSession, WikiPermission.PREFERENCES );
    boolean canSaveProfile = authMgr.checkPermission( wikiSession, WikiPermission.REGISTER );
    String user = wikiContext.getCurrentUser().getName();
    
    // User must have permission to change the profile
    if( !canSavePrefs )
    {
        log.info("User "+wikiContext.getCurrentUser()+" has no access to set preferences - redirecting to login page.");
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
    
    // Extract the user profile and action attributes
    UserProfile profile = userMgr.parseProfile( wikiContext );

    if( canSaveProfile && "saveProfile".equals(request.getParameter("action")) )
    {
        // Validate the profile
        errors.clear();
        userMgr.validateProfile( wikiContext, profile, errors );

        // If no errors, save the profile now & refresh the principal set!
        if ( errors.size() == 0 )
        {
            try
            {
                userMgr.setUserProfile( wikiContext.getWikiSession(), profile );
                CookieAssertionLoginModule.setUserCookie( response, profile.getFullname() );
            }
            catch( DuplicateUserException e )
            {
                // User collision! (full name or wiki name already taken)
                errors.add( e.getMessage() );
            }
            catch( WikiSecurityException e )
            {
                // Something went horribly wrong! Maybe it's an I/O error...
                errors.add( e.getMessage() );
            }
        }
        if ( errors.size() == 0 )
        {
		   response.sendRedirect( wiki.getBaseURL()+"Wiki.jsp" );
		   return;
        }
    }
    if( "setAssertedName".equals(request.getParameter("action")) )
    {
        String assertedName = request.getParameter("assertedName");
        CookieAssertionLoginModule.setUserCookie( response, assertedName );
        response.sendRedirect( wiki.getBaseURL()+"Wiki.jsp" );
        return;
    }
    if( "clearAssertedName".equals(request.getParameter("action")) )
    {
        CookieAssertionLoginModule.clearUserCookie( response );
        response.sendRedirect( wiki.getBaseURL()+"Logout.jsp" );
        return;
    }
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" /><%
    NDC.pop();
    NDC.remove();
%>
