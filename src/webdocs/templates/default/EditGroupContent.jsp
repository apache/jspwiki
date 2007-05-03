<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>
<%@ page import="java.security.Principal" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="com.ecyrd.jspwiki.auth.PrincipalComparator" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.Group" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page errorPage="/Error.jsp" %>
<%! 
    Logger log = Logger.getLogger("JSPWiki"); 
%>

<%
  // Extract the group name and members
  String name = request.getParameter( "group" );
  Group group = (Group)pageContext.getAttribute( "Group",PageContext.REQUEST_SCOPE );
  Principal[] members = null;
  
  if ( group != null )
  {
    name = group.getName();
    members = group.members();
    Arrays.sort( members, new PrincipalComparator() );
  }
%>

<wiki:TabbedSection defaultTab="${param.tab}">
  <wiki:Tab id="logincontent" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "editgroup.tab")%>">

  <h3><%=name%></h3>

  <form action="<wiki:Link format='url' jsp='EditGroup.jsp'/>" 
         class="wikiform"
            id="editGroup" 
        method="POST" accept-charset="UTF-8">
      
    <!-- Members -->
    <%
      StringBuffer s = new StringBuffer();
      for ( int i = 0; i < members.length; i++ )
      {
        s.append( members[i].getName().trim() );
        s.append( '\n' );
      }
    %>
    <div class="formhelp">
    <fmt:message key="editgroup.instructions">
      <fmt:param><%=name%></fmt:param>
     </fmt:message>
    </div>
    <div class="formhelp">
      <wiki:Messages div="error" topic="group" prefix="<%=LocaleSupport.getLocalizedMessage(pageContext,"editgroup.saveerror") %>" />
    </div>

    <table class="wikitable">
    <tr>
      <th>Group Name</th>
      <td><%=name%></td>
    </tr>
    <tr>
      <th><label>Members</label></th>
      <td>
      <textarea id="members" name="members" rows="10" cols="30"><%=s.toString()%></textarea>
      <div class="formhelp"><fmt:message key="editgroup.memberlist"/></div>
      </td>
    </tr>
    </table>
      <input type="submit" name="ok" value="<fmt:message key="editgroup.submit.save"/>" />
      <input type="hidden" name="group" value="<%=name%>" />
      <input type="hidden" name="action" value="save" />
      <div class="formhelp">
        <fmt:message key="editgroup.savehelp"><fmt:param><%=name%></fmt:param></fmt:message>
      </div>
  </form>


</wiki:Tab>
</wiki:TabbedSection>