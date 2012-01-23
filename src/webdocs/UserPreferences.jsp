<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.VariableManager" %>
<%@ page import="com.ecyrd.jspwiki.WikiContext" %>
<%@ page import="com.ecyrd.jspwiki.WikiSession" %>
<%@ page import="com.ecyrd.jspwiki.WikiEngine" %>
<%@ page import="com.ecyrd.jspwiki.auth.UserManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.WikiSecurityException" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.DuplicateUserException" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.UserProfile" %>
<%@ page import="com.ecyrd.jspwiki.workflow.DecisionRequiredException" %>
<%@ page import="com.ecyrd.jspwiki.ui.EditorManager" %>
<%@ page import="com.ecyrd.jspwiki.ui.TemplateManager" %>
<%@ page import="com.ecyrd.jspwiki.preferences.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    Logger log = Logger.getLogger("JSPWiki"); 
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.PREFS );
    if(!wikiContext.hasAccess( response )) return;
    
    // Extract the user profile and action attributes
    UserManager userMgr = wiki.getUserManager();
    WikiSession wikiSession = wikiContext.getWikiSession();

/* FIXME: Obsoslete 
    if( request.getParameter(EditorManager.PARA_EDITOR) != null )
    {
    	String editor = request.getParameter(EditorManager.PARA_EDITOR);
    	session.setAttribute(EditorManager.PARA_EDITOR,editor);
    }
*/

    // Are we saving the profile?
    if( "saveProfile".equals(request.getParameter("action")) )
    {
        UserProfile profile = userMgr.parseProfile( wikiContext );
         
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
            catch( DecisionRequiredException e )
            {
                String redirect = wiki.getURL(WikiContext.VIEW,"ApprovalRequiredForUserProfiles",null,true);
                response.sendRedirect( redirect );
                return;
            }
            catch( WikiSecurityException e )
            {
                // Something went horribly wrong! Maybe it's an I/O error...
                wikiSession.addMessage( "profile", e.getMessage() );
            }
        }
        if ( wikiSession.getMessages( "profile" ).length == 0 )
        {
            String redirectPage = request.getParameter( "redirect" );

            if( !wiki.pageExists( redirectPage ) )
            {
               redirectPage = wiki.getFrontPage();
            }
            
            String viewUrl = ( "UserPreferences".equals( redirectPage ) ) ? "Wiki.jsp" : wiki.getViewURL( redirectPage );
            log.info( "Redirecting user to " + viewUrl );
            response.sendRedirect( viewUrl );
            return;
        }
    }
    if( "setAssertedName".equals(request.getParameter("action")) )
    {
        Preferences.reloadPreferences(pageContext);
        
        String assertedName = request.getParameter("assertedName");
        CookieAssertionLoginModule.setUserCookie( response, assertedName );

        String redirectPage = request.getParameter( "redirect" );
        if( !wiki.pageExists( redirectPage ) )
        {
          redirectPage = wiki.getFrontPage();
        }
        String viewUrl = ( "UserPreferences".equals( redirectPage ) ) ? "Wiki.jsp" : wiki.getViewURL( redirectPage );

        log.info( "Redirecting user to " + viewUrl );
        response.sendRedirect( viewUrl );
        return;
    }
    if( "clearAssertedName".equals(request.getParameter("action")) )
    {
        CookieAssertionLoginModule.clearUserCookie( response );
        response.sendRedirect( wikiContext.getURL(WikiContext.NONE,"Logout.jsp") );
        return;
    }
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />

