<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page errorPage="/Error.jsp" %>
<%! 
    Category log = Category.getInstance("JSPWiki"); 
%>

<%
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
%>

      <h3>Your <wiki:Variable var="applicationname" /> Profile</h3>
      <p>Manage your wiki profile here.</p>
      
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
               <wiki:UserProfile property='loginname'/>
             </td>
           </tr>
           <tr>
             <td />
             <td colspan="2">
               <wiki:UserCheck status="customAuth">
                 <i>This is your login id; it cannot be changed.
                    It is only used for authentication, not for page access control.</i>
                 </i>
               </wiki:UserCheck>
               <wiki:UserCheck status="containerAuth">
                 <i>This is your login id. It was set by the web container and 
                    cannot be changed.</i>
               </wiki:UserCheck>
             </td>
           </tr>

           <!-- Password; not displayed if container auth used -->
           <wiki:UserCheck status="customAuth">
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
                 <i>Sets your account password. Leave blank if you don't want to change it now.</i>
               </td>
             </tr>
           </wiki:UserCheck>

           <!-- Wiki name -->
           <tr>
             <td width="15%">
               <b>Wiki name:</b>
             </td>
             <td>
               <wiki:UserProfile property='wikiname'/>
             </td>
           </tr>
           <tr>
             <td />
             <td colspan="2">
               <i>This must be a proper WikiName, and cannot contain spaces or punctuation.
             </td>
           </tr>

           <!-- Full name -->
           <tr>
             <td width="15%">
               <b>Full name:</b>
             </td>
             <td>
               <wiki:UserProfile property='fullname'/>
             </td>
           </tr>
           <tr>
             <td />
             <td colspan="2">
               <i>This is your full name.</i>
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
               <i>Your e-mail address is optional. In the future, it will be used
                  by JSPWiki for resetting lost passwords.</i>
             </td>
           </tr>
           
         </table>

        <!-- Any errors? -->
        <%
        if ( errors != null && errors.size() > 0 )
        { 
            out.println("<blockquote><p>Could not save profile:<ul>");
            for ( Iterator it = errors.iterator(); it.hasNext(); )
            {
                out.println( "<li>" + it.next().toString() + "</li>" );
            }
            out.println("</ul></p></blockquote>");
        }
        %>

        <p>Access control lists or wiki groups containing your identity
           should specify <strong><wiki:UserProfile property="wikiname"/></strong> or
           <b><wiki:UserProfile property="fullname"/></b>.
           You are also a member of these roles and groups: 
           <strong><wiki:UserProfile property="roles" /></strong>. 
           ACLs containing these roles and groups should work, too.</p>
        <p>Click 'save profile' to change your wiki profile.
           You created your profile on <wiki:UserProfile property="created"/>,
           and last saved it on <wiki:UserProfile property="modified"/></p>
        <input type="submit" name="ok" value="Save profile" />
        <input type="hidden" name="action" value="save" />
      </form>

      <wiki:UserCheck status="assertionsAllowed">
        <hr />
        <p>This wiki automatically remembers you using cookies, without
           requiring additional authentication. To use this feature, your browser
           must accept cookies from this website. When you click 'save profile,'
           the cookie will be saved by your browser.</p>
        <p>If you want this wiki to "forget" your identity, you will need to remove
           the user cookie from your browser. Click the 'remove cookie' button
           to do that.</p>
        <form action="<wiki:Variable var="baseURL"/>UserPreferences.jsp"
              method="POST"
              accept-charset="UTF-8">
        <input type="submit" name="clear" value="Remove cookie" />
        </form>
      </wiki:UserCheck>

