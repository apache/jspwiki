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

      <h3>New Wiki Group</h3>
      <p>Create a new wiki group.</p>
      
      <form action="<wiki:Variable var="baseURL"/>NewGroup.jsp" 
            method="POST"
            accept-charset="UTF-8">
         <table border="0">
         
           <!-- Group name -->
           <tr>
             <td width="15%">
               <b>Group name:</b>
             </td>
             <td>
                 <input type="text" name="name" size="30" 
                  value="<%=pageContext.getAttribute("name",PageContext.REQUEST_SCOPE)%>"/>
             </td>
           </tr>

           <tr>
             <td />
             <td>
               <i>The name of the group.</i>
             </td>
           </tr>

           <!-- Members -->
           <tr>
             <td width="15%">
               <b>Members:</b>
             </td>
             <td>
              
                 <input type="text" name="members" size="30" 
                  value="<%=pageContext.getAttribute("members",PageContext.REQUEST_SCOPE)%>"/>
             </td>
           </tr>

           <tr>
             <td />
             <td>
               <i>The membership for this group. Enter each user's wiki name
                  or full name, separated by commas.</i>
             </td>
           </tr>
           
           <tr>
             <td colspan="2">
               <!-- Any errors? -->
               <%
               if ( errors != null && errors.size() > 0 )
               { 
                   out.println("<blockquote><p>Could not save group:<ul>");
                   for ( Iterator it = errors.iterator(); it.hasNext(); )
                   {
                       out.println( "<li>" + it.next().toString() + "</li>" );
                   }
                   out.println("</ul></p></blockquote>");
               }
               %>
             </td>
           </tr>
           
           <tr>
             <td />
             <td>
               <br />
               <p>When you click 'save', this group will be saved as a wiki page
                  called <b>Group<i>Name</i></b>.  E.g. if you type in "Admin", the
                  group will be called "GroupAdmin".</p>
             </td>
           </tr>
           
           <tr>
             <td />
             <td>
               <input type="submit" name="ok" value="Save" />
               <input type="hidden" name="action" value="save" />
             </td>
           </tr>
         </table>

      </form>
