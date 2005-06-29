<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.ecyrd.jspwiki.WikiEngine" %>
<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
        containerAuth = wiki.getAuthenticationManager().isContainerAuthenticated();
    }
    WikiEngine wiki;
    boolean containerAuth;
%>
<%
    ArrayList inputErrors = new ArrayList();
    boolean newProfile = false;
    if ( session.getAttribute( "errors" ) != null)
    {
        inputErrors = (ArrayList)session.getAttribute( "inputErrors" );
    }
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
             <td width="20%">
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
             <td colspan="2">
               <i>This is the login id for authentication; once set, it cannot be changed.
                  After login, it isn't especially relevant (your user name and wiki name
                  are shown on pages, not the login id).</i>
             </td>
           </tr>

           <!-- Password -->
           <tr>
             <td width="20%">
               <b>Password:</b>
             </td>
             <td>
                <input type="password" name="password" size="30" value="" />
             </td>
           </tr>
           <tr>
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
             <td width="20%">
               <b>Wiki name:</b>
             </td>
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
           </tr>
           <tr>
             <td colspan="2">
               <i>This must be a proper WikiName; no punctuation.</i>
             </td>
           </tr>
           
           <!-- Full name -->
           <tr>
             <td width="20%">
               <b>Full name:</b>
             </td>
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
           </tr>
           <tr>
             <td colspan="2">
               <i>This is your full name; it can have punctuation.</i>
             </td>
           </tr>

           <!-- E-mail -->
           <tr>
             <td width="20%">
               <b>E-mail address:</b>
             </td>
             <td>
                <input type="text" name="email" size="30" value="<wiki:UserProfile property="email"/>" />
             </td>
           </tr>
           <tr>
             <td colspan="2">
               <i>This must be a proper WikiName; no punctuation.</i>
             </td>
           </tr>
           
           <!-- Any errors? -->
           <%
           if ( inputErrors.size() > 0 )
           { 
           %>  <tr>
                 <td colspan="2">
                   <p><b>Error</b></p>
           <%
               for ( int i = 0; i < inputErrors.size(); i++ )
               {
                   out.println( "<p>" + inputErrors.get(i).toString() + "</p>" );
               }
           %>    </td>
               </tr>
           <%
           }
           %>

         </table>
           
         <br /><br />
         <input type="submit" name="ok" value="Set my preferences!" />
         <input type="hidden" name="action" value="save" />
      </form>

      <hr />

      <h3>Removing your preferences</h3>

      <p>In some cases, you may need to remove the above preferences from the computer.
      Click the button below to do that.  Note that it will remove all preferences
      you've set up, permanently.  You will need to enter them again.</p>

      <div align="center">
      <form action="<wiki:Variable var="baseURL"/>UserPreferences.jsp"
            method="POST"
            accept-charset="UTF-8">
      <input type="submit" name="clear" value="Remove preferences from this computer" />
      </form>
      </div>
