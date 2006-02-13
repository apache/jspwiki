<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.WikiContext" %>
<%@ page import="com.ecyrd.jspwiki.WikiSession" %>
<%@ page import="com.ecyrd.jspwiki.WikiEngine" %>
<%@ page import="com.ecyrd.jspwiki.auth.NoSuchPrincipalException" %>
<%@ page import="com.ecyrd.jspwiki.auth.UserManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.WikiSecurityException" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.WikiPermission" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.DuplicateUserException" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.UserProfile" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Logger log = Logger.getLogger("JSPWiki"); 
    WikiEngine wiki;
%>

<%
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.PREFS );
    if(!wikiContext.hasAccess( response )) return;
    
    // Extract the user profile and action attributes
    UserManager userMgr = wiki.getUserManager();
    UserProfile profile = userMgr.parseProfile( wikiContext );
    WikiSession wikiSession = wikiContext.getWikiSession();

    // Are we saving the profile?
    if( "saveProfile".equals(request.getParameter("action")) )
    {
        // Validate the profile
        userMgr.validateProfile( wikiContext, profile );

        // If no errors, save the profile now & refresh the principal set!
        if ( wikiSession.getMessages( "profile" ).length == 0 )
        {
            try
            {
                userMgr.setUserProfile( wikiSession, profile );
                CookieAssertionLoginModule.setUserCookie( response, profile.getFullname() );
            }
            catch( DuplicateUserException e )
            {
                // User collision! (full name or wiki name already taken)
                wikiSession.addMessage( "profile", e.getMessage() );
            }
            catch( WikiSecurityException e )
            {
                // Something went horribly wrong! Maybe it's an I/O error...
                wikiSession.addMessage( "profile", e.getMessage() );
            }
        }
        if ( wikiSession.getMessages( "profile" ).length == 0 )
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
%><wiki:Include page="<%=contentPage%>" />

