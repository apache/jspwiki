<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.attachment.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.security.Principal" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
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
    AuthenticationManager mgr = wiki.getAuthenticationManager();
    WikiContext wikiContext = wiki.createContext( request, WikiContext.LOGIN );
    WikiSession wikiSession = wikiContext.getWikiSession();
    NDC.push( wiki.getApplicationName() + ":Login.jsp"  );
    session.setAttribute("msg","");
    
    if( !mgr.isContainerAuthenticated() )
    {
        // If using custom auth, we need to do the login now

        String action = request.getParameter("action");
        if( "login".equals(action) )
        {
            String uid    = wiki.safeGetParameter( request,"j_username" );
            String passwd = wiki.safeGetParameter( request,"j_password" );
            log.debug( "Attempting to authenticate user " + uid );
            
            // Log the user in!
            if ( mgr.login( wikiSession, uid, passwd ) )
            {
                log.info( "Successfully authenticated user " + uid + " (custom auth)" );
            }
            else
            {
                log.error( "Failed to authenticate user " + uid );
                if ( passwd.length() > 0 && passwd.toUpperCase().equals(passwd) )
                {
                    session.setAttribute("msg", "Invalid login (please check your Caps Lock key)");
                }
                else
                {
                    session.setAttribute("msg", "Not a valid login.");
                }
            }
        }
    }
    else 
    {
        // If using container auth, the container will have automatically
        // attempted to log in the user before Login.jsp was loaded.
        // Thus, if we got here, the container must have authenticated 
        // the user already. All we do is simply record that fact.
        // Nice and easy.
        
        Principal user = wikiSession.getLoginPrincipal();
        log.info( "Successfully authenticated user " + user.getName() + " (container auth)" );
    }    
    
    // If user logged in, set the user cookie with the wiki principal's name.
    // redirect to wherever we're supposed to go. If login.jsp
    // was called without parameters, this will be the front page. Otherwise,
    // there's probably a 'page' parameter telling us where to go.
    
    if ( wikiSession.isAuthenticated() )
    {
        // Set user cookie
        Principal principal = wikiSession.getUserPrincipal();
        CookieAssertionLoginModule.setUserCookie( response, principal.getName() );
    
        // Redirect!
        String viewUrl = wiki.getViewURL( wikiContext.getPage().getName() );
        log.info( "Redirecting user to " + wikiContext.getPage().getName() );
        response.sendRedirect( viewUrl );
        NDC.pop();
        NDC.remove();
    }
    
    // If we've gotten here, the user hasn't authenticated yet.
    // So, find the login form and include it. This should be in the same directory
    // as this page. We don't need to use the wiki:Include tag.
    
    %>
        <jsp:include page="LoginForm.jsp" />
    <%
    NDC.pop();
    NDC.remove();
%>
