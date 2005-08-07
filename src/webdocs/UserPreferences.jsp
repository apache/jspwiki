<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.WikiContext" %>
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
    AuthenticationManager mgr = wiki.getAuthenticationManager();
    AuthorizationManager authMgr = wiki.getAuthorizationManager();
    UserManager userMgr = wiki.getUserManager();
    boolean containerAuth = mgr.isContainerAuthenticated();
    boolean cookieAssertions = AuthenticationManager.allowsCookieAssertions();
    boolean isAuthenticated = wikiContext.getWikiSession().isAuthenticated();
    
    // User must have permission to change the profile
    if( !authMgr.checkPermission( wikiContext, WikiPermission.PREFERENCES ) )
    {
        log.info("User "+wikiContext.getCurrentUser()+" has no access to set preferences - redirecting to login page.");
        String msg = "You do not seem to have the permissions for this operation. Would you like to login as another user?";
        wikiContext.setVariable( "msg", msg );
        String pageurl = wiki.encodeName( wikiContext.getPage().getName() );
        response.sendRedirect( wiki.getBaseURL()+"Login.jsp?page="+pageurl );
    }
    
    // If user doesn't exist, redirect to registration page
    UserProfile profile = userMgr.getUserProfile( wikiContext );
    if ( profile.isNew() )
    {
        log.info("User profile for "+wikiContext.getCurrentUser()+" doesn't exist; redirecting to registration page.");
        response.sendRedirect( wiki.getBaseURL()+"Register.jsp" );
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
    profile = userMgr.parseProfile( wikiContext );
    String ok = request.getParameter("ok");
    String clear = request.getParameter("clear");

    if( ok != null || "save".equals(request.getParameter("action")) )
    {
        // Validate the profile
        errors.clear();
        userMgr.validateProfile( wikiContext, profile, errors );

        // If no errors, save the profile now & refresh the principal set!
        if ( errors.size() == 0 )
        {
            try
            {
                userMgr.setUserProfile( wikiContext, profile );
                CookieAssertionLoginModule.setUserCookie( response, profile.getWikiName() );
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
    else if( clear != null )
    {
        CookieAssertionLoginModule.clearUserCookie( response );
        response.sendRedirect( wiki.getBaseURL()+"Wiki.jsp" );
    }
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "PreferencesContent.jsp" );
%><wiki:Include page="<%=contentPage%>" /><%
    NDC.pop();
    NDC.remove();
%>
