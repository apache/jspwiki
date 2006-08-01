<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="com.ecyrd.jspwiki.auth.PrincipalComparator" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.Group" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.GroupManager" %>
<%@ page errorPage="/Error.jsp" %>

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

<h3>Create New Group</h3>

<wiki:Messages div="error" topic="<%=GroupManager.MESSAGES_KEY%>" prefix="Error: "/>

<div class="formcontainer">
  <div class="instructions">
    This page allows you to create a new wiki group.
  </div>
  <form id="createGroup" action="<wiki:Link format="url" jsp="NewGroup.jsp"/>" 
    method="POST" accept-charset="UTF-8">
      
    <!-- Name -->
    <div class="block">
      <label>Name</label>
      <input type="text" name="group" size="30" value="<%=name%>" />
      <div class="description">
        The name of the new group.
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
      <label>Members</label>
      <textarea id="members" name="members" rows="20" cols="40"><%=s.toString()%></textarea>
      <div class="description">
        The membership for this group. Enter each user&#8217;s wiki name
        or full name, separated by carriage returns.
      </div>
    </div>

    <div class="block">
      <div class="instructions">
        When you click &#8220;Save group,&#8221; this group will be saved as a group.
        You can specify the group's name in page access control lists (ACLs).
      </div>
      <input type="submit" name="ok" value="Create group" />
      <input type="hidden" name="action" value="save" />
    </div>
  </form>
</div>
