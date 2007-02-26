<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.DefaultResources"/>
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

<h3><fmt:message key="editgroup.heading.edit"><fmt:param><%=name%></fmt:param></fmt:message></h3>

<div class="formcontainer">
  <div class="instructions">
    <fmt:message key="editgroup.instructions">
      <fmt:param><%=name%></fmt:param>
     </fmt:message>
  </div>
  <div class="instructions">
    <wiki:Messages div="error" topic="group" prefix="<%=LocaleSupport.getLocalizedMessage(pageContext,"editgroup.saveerror") %>" />
  </div>
  <form id="editGroup" action="<wiki:Link format="url" jsp="EditGroup.jsp"/>" 
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
    <div class="block">
      <label>Members</label>
      <textarea id="members" name="members" rows="20" cols="40"><%=s.toString()%></textarea>
      <div class="description">
        <fmt:message key="editgroup.memberlist"/>
      </div>
    </div>

    <div class="block">
      <!-- Any errors? -->
      <div class="instructions">
        <fmt:message key="editgroup.savehelp"><fmt:param><%=name%></fmt:param></fmt:message>
      </div>
      <input type="submit" name="ok" value="<fmt:message key="editgroup.submit.save"/>" />
      <input type="hidden" name="group" value="<%=name%>" />
      <input type="hidden" name="action" value="save" />
    </div>
  </form>
</div>
