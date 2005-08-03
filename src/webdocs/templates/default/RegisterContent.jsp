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

      <h3>Your <wiki:Variable var="applicationname" /> profile</h3>
      <p>Create your wiki profile here.</p>
      
      <form action="<wiki:Variable var="baseURL"/>Register.jsp" 
            method="POST"
            accept-charset="UTF-8">
         <table border="0">
         
           <!-- Login name -->
           <tr>
             <td width="15%">
               <b>Login name:</b>
             </td>
             <td>
               <wiki:UserCheck status="customAuth">
                 <input type="text" name="loginname" size="30" value="<wiki:UserProfile property="loginname"/>" />
               </wiki:UserCheck>
               <wiki:UserCheck status="containerAuth">
                 <wiki:UserProfile property="loginname"/>
               </wiki:UserCheck>
             </td>
           </tr>

           <tr>
             <td />
             <td colspan="2">
               <wiki:UserCheck status="customAuth">
                 <i>This is your login id; once set, it cannot be changed.
                    It is only used for authentication, not for page access control.</i>
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
                 <i>Sets your account password. It may not be blank.</i>
               </td>
             </tr>
           </wiki:UserCheck>

           <!-- Wiki name -->
           <tr>
             <td width="15%">
               <b>Wiki name:</b>
             </td>
             <td>
                <input type="text" name="wikiname" size="30" value="<wiki:UserProfile property="wikiname"/>" />
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
                <input type="text" name="fullname" size="30" value="<wiki:UserProfile property="fullname"/>" />
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

         <p>Click 'save profile' to change your wiki profile.</p>
         <input type="submit" name="ok" value="Save profile" />
         <input type="hidden" name="action" value="save" />
      </form>

      <wiki:UserCheck status="assertionsAllowed">
        <hr />
        <p>This wiki automatically remembers you using cookies, without
           requiring additional authentication. To use this feature, your browser
           must accept cookies from this website. When you click 'save profile,'
           the cookie will be saved by your browser.</p>
      </wiki:UserCheck>
