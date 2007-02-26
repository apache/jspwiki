<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="com.ecyrd.jspwiki.auth.PrincipalComparator" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.Group" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.GroupManager" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.DefaultResources"/>

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

<h3><fmt:message key="newgroup.heading.create"/></h3>

<wiki:Messages div='error' topic='<%=GroupManager.MESSAGES_KEY%>' prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"newgroup.errorprefix")%>' />

<div class="formcontainer">
  <div class="instructions">
     <fmt:message key="newgroup.instructions.start"/>
  </div>
  <form id="createGroup" action="<wiki:Link format="url" jsp="NewGroup.jsp"/>" 
    method="POST" accept-charset="UTF-8">
      
    <!-- Name -->
    <div class="block">
      <label><fmt:message key="newgroup.name"/></label>
      <input type="text" name="group" size="30" value="<%=name%>" />
      <div class="description">
        <fmt:message key="newgroup.name.description"/>
      </div>
    </div>
    
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
      <label><fmt:message key="group.members"/></label>
      <textarea id="members" name="members" rows="20" cols="40"><%=s.toString()%></textarea>
      <div class="description">
        <fmt:message key="newgroup.members.description"/>
      </div>
    </div>

    <div class="block">
      <div class="instructions">
         <fmt:message key="newgroup.instructions.end"/>
      </div>
      <input type="submit" name="ok" value="Create group" />
      <input type="hidden" name="action" value="save" />
    </div>
  </form>
</div>
