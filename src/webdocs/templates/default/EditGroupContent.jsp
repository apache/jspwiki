<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="com.ecyrd.jspwiki.auth.PrincipalComparator" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.Group" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page errorPage="/Error.jsp" %>
<%! 
    Category log = Category.getInstance("JSPWiki"); 
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

<h3>Edit Group <%=name%></h3>

<div class="formcontainer">
  <div class="instructions">
    This page allows you to add or edit members for the wiki group called 
    <em><%=name%></em>. Generally, only members of the group can edit the 
    membership list. By default, the person who creates the group is a member.
  </div>
  <div class="instructions">
    <wiki:Messages div="error" topic="group" prefix="Could not save group: " />
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
        The membership for this group. Enter each user&#8217;s wiki name
        or full name, separated by carriage returns.
      </div>
    </div>

    <div class="block">
      <!-- Any errors? -->
      <div class="instructions">
        When you click &#8220;Save group,&#8221; this group will be saved as a group
        called <strong><%=name%></strong>. You can specify this
        name in page access control lists (ACLs).
      </div>
      <input type="submit" name="ok" value="Save group" />
      <input type="hidden" name="group" value="<%=name%>" />
      <input type="hidden" name="action" value="save" />
    </div>
  </form>
</div>
