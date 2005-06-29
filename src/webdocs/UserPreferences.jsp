<%@ page import="java.security.Principal" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.WikiEngine" %>
<%@ page import="com.ecyrd.jspwiki.tags.UserProfileTag" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.auth.AuthenticationManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.NoSuchPrincipalException" %>
<%@ page import="com.ecyrd.jspwiki.auth.WikiSecurityException" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.UserDatabase" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.UserProfile" %>
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
    String pagereq = wikiContext.getPage().getName();
    AuthenticationManager mgr = wiki.getAuthenticationManager();
    boolean containerAuth = mgr.isContainerAuthenticated();
    
    NDC.push( wiki.getApplicationName()+":"+pagereq );
    
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    String ok = request.getParameter("ok");
    String clear = request.getParameter("clear");
    String email = request.getParameter("email");
    String fullname = request.getParameter("fullname");
    String loginname = request.getParameter("loginname");
    String password = request.getParameter("password");
    String wikiname = request.getParameter("wikiname");
    boolean newProfile = true;
    pageContext.setAttribute( "newProfile", new Boolean( newProfile ) );
    
    // Check if fields contain blanks
    if ( email == null || UserProfileTag.BLANK.equals( email ) )
    {
        email = "";
    }
    if ( fullname == null || UserProfileTag.BLANK.equals( fullname ) )
    {
        fullname = "";
    }
    if ( loginname == null || UserProfileTag.BLANK.equals( loginname ) )
    {
        loginname = "";
    }
    if ( wikiname == null || UserProfileTag.BLANK.equals( wikiname ) )
    {
        wikiname = "";
    }
    
    ArrayList inputErrors = new ArrayList();
    session.setAttribute( "errors", inputErrors );

    if( ok != null || "save".equals(request.getParameter("action")) )
    {
        Principal user = wikiContext.getCurrentUser();
        UserDatabase database = wiki.getUserDatabase();
        UserProfile profile = null;
        newProfile = false;
        
        try
        {
            profile = database.find( user.getName() );
            pageContext.setAttribute( "newProfile", new Boolean( newProfile ) );
        }
        catch ( NoSuchPrincipalException e )
        {
            newProfile = true;
            profile = database.newProfile();
        }
        
        // Existing profiles can't change the loginname, fullname, or wiki name
        if ( newProfile )
        {
            if ( fullname == null || fullname.length() < 1 )
            {
              inputErrors.add("Full name cannot be blank");
            }
            if ( !containerAuth && ( loginname == null || loginname.length() < 1 ) )
            {
              inputErrors.add("Login name cannot be blank");
            }
            if (wikiname == null || wikiname.length() < 1 )
            {
              inputErrors.add("Wiki name cannot be blank");
            }
        }
        
        // Passwords for new accounts cannot be null
        // ARJ: TODO: we don't check for the policy yet, but we should..
        if ( !containerAuth && newProfile && password == null)
        {
              inputErrors.add("Password cannot be blank");
        }
        
        // It's ok if the e-mail is null. Not everybody wants to supply this...
        if (email != null || email.length() < 1 )
        {
            profile.setEmail( email );
        }
        
        // Set the rest of the profile properties
        if ( !containerAuth )
        {
            profile.setLoginName( loginname );
            profile.setPassword( password );
        }
        profile.setFullname( fullname );
        profile.setWikiName( wikiname );
        pageContext.setAttribute("inputErrors", inputErrors);
        
        // If no errors, save the profile now & refresh the principal set!
        if ( inputErrors.size() == 0 )
        {
            try
            {
                database.save( profile );
                database.commit();
                if ( newProfile )
                {
                    mgr.loginCustom( loginname, password, request );
                }
                else 
                {
                    mgr.refreshCredentials( wikiContext.getWikiSession() );
                }
            }
            catch( WikiSecurityException e )
            {
              // Something went horribly wrong! Maybe it's an I/O error...
            }
        }

        response.sendRedirect( wiki.getBaseURL()+"UserPreferences.jsp" );
    }
    else if( clear != null )
    {
        mgr.logout( session );
        CookieAssertionLoginModule.clearUserCookie( response );
        response.sendRedirect( wiki.getBaseURL()+"UserPreferences.jsp" );
    }       
    else
    {
        response.setContentType("text/html; charset="+wiki.getContentEncoding() );
        String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                                wikiContext.getTemplate(),
                                                                "ViewTemplate.jsp" );
%>
<%
    newProfile = false;
    if ( pageContext.getAttribute( "newProfile" ) != null)
    {
        newProfile = ((Boolean)pageContext.getAttribute( "newProfile" )).booleanValue();
    }
%>

      <h3>Your Wiki profile</h3>
      <p>
      This page allows you to set up your wiki profile.
      You need to have cookies enabled for this to work, though.
      </p>
      
      <form action="<wiki:Variable var="baseURL"/>UserPreferences.jsp" 
            method="POST"
            accept-charset="UTF-8">
         <table border="0">
         
           <!-- Login name -->
           <tr>
             <td width="15%">
               <b>Login name:</b>
             </td>
             <td>
             <%
               if ( newProfile && !containerAuth ) 
               {
                  %> <input type="text" name="loginname" size="30" value="<wiki:UserProfile property='loginname'/>" /> <%
               }
               else
               {
                  %> <wiki:UserProfile property='loginname'/> <%
               }
             %>
             </td>
           </tr>
           <tr>
             <td />
             <td colspan="2">
               <i>This is the login id for authentication; once set, it cannot be changed.
                  After login, it isn't especially relevant (your user name and wiki name
                  are shown on pages, not the login id).</i>
             </td>
           </tr>

           <!-- Password -->
           <tr>
             <td width="15%">
               <b>Password:</b>
             </td>
             <td>
                <input type="password" name="password" size="30" value="" />
             </td>
           </tr>
           <tr>
             <td />
             <td colspan="2">
               <i>You'll need your password to log in using JSPWiki's custom 
                  authentication mechanism. If you are using container-managed
                  authentication instead, setting the password here has no effect.</i>
             </td>
           </tr>
           <% 
              if ( !containerAuth )
              {
           %>
           <%
             }
           %>

           <!-- Wiki name -->
           <tr>
             <td width="15%">
               <b>Wiki name:</b>
             </td>
             <td>
               <%
                 if ( newProfile ) 
                 {
                    %> <input type="text" name="wikiname" size="30" value="<wiki:UserProfile property='wikiname'/>" /> <%
                 }
                 else
                 {
                    %> <wiki:UserProfile property='wikinname'/> <%
                 }
               %>
             </td>
           </tr>
           <tr>
             <td />
             <td colspan="2">
               <i>This must be a proper WikiName; no punctuation.</i>
             </td>
           </tr>
           
           <!-- Full name -->
           <tr>
             <td width="15%">
               <b>Full name:</b>
             </td>
             <td>
               <%
                 if ( newProfile ) 
                 {
                    %> <input type="text" name="fullname" size="30" value="<wiki:UserProfile property='fullname'/>" /> <%
                 }
                 else
                 {
                    %> <wiki:UserProfile property='fullname'/> <%
                 }
               %>
             </td>
           </tr>
           <tr>
             <td />
             <td colspan="2">
               <i>This is your full name; it can have punctuation.</i>
             </td>
           </tr>

           <!-- E-mail -->
           <tr>
             <td width="15%">
               <b>E-mail address:</b>
             </td>
             <td>
                <input type="text" name="email" size="30" value="<wiki:UserProfile property="email"/>" />
             </td>
           </tr>
           <tr>
             <td />
             <td colspan="2">
               <i>This must be a proper WikiName; no punctuation.</i>
             </td>
           </tr>
           
           <!-- Any errors? -->
           <%
           ArrayList errors = (ArrayList)session.getAttribute( "inputErrors" );
           if ( errors != null && errors.size() > 0 )
           { 
               out.println("<tr><td colspan='2'><p><b>Error</b></p>");
               for ( int i = 0; i < errors.size(); i++ )
               {
                   out.println( "<p>" + errors.get(i).toString() + "</p>" );
               }
               out.println("</td></tr>");
           }
           %>

         </table>
           
         <br /><br />
         <input type="submit" name="ok" value="Save profile" />
         <input type="hidden" name="action" value="save" />
      </form>

      <hr />

      <h3>Clearing the 'remember me' cookie</h3>

      <p>In some cases, you may need to remove the user cookie from the computer.
      Click the button below to do that.</p>

      <div align="center">
      <form action="<wiki:Variable var="baseURL"/>UserPreferences.jsp"
            method="POST"
            accept-charset="UTF-8">
      <input type="submit" name="clear" value="Remove user cookie" />
      </form>
      </div>
<%
    } // Else
    NDC.pop();
    NDC.remove();
%>

