<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="com.ecyrd.jspwiki.WikiContext" %>
<%@ page import="com.ecyrd.jspwiki.auth.PrincipalComparator" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.Group" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.GroupManager" %>
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
  String modified = "";
  String created = "";
  String modifier = "";
  String creator = "";
  
  if ( group != null )
  {
    name = group.getName();
    members = group.members();
    Arrays.sort( members, new PrincipalComparator() );
    creator = group.getCreator();
    if ( group.getCreated() != null )
    {
      created = group.getCreated().toString();
    }
    modifier = group.getModifier();
    if ( group.getLastModified() != null )
    {
      modified = group.getLastModified().toString();
    }
  }
%>


<script language="javascript" type="text/javascript">
function confirmDelete()
{
  var reallydelete = confirm("Are you sure you want to permanently delete group '<%=name%>'? Users might not be able to access pages whose ACLS contain this group. \n\nIf you click OK, the group will be removed immediately.");

  return reallydelete;
}
</script>

<h3>Group <%=name%></h3>

<%
  if ( group == null )
  {
    WikiContext c = WikiContext.findContext( pageContext );
    
    if ( c.getWikiSession().getMessages( GroupManager.MESSAGES_KEY ).length == 0 )
    {
%>
    This group does not exist.
    <wiki:Permission permission="createGroups">
      Why don&#8217;t you go and
      <wiki:Link jsp="NewGroup.jsp">
        <wiki:Param name="group" value="<%=name%>" />
        <wiki:Param name="group" value="<%=name%>" />
        create it
      </wiki:Link>?
    </wiki:Permission>
<%
    }
    else
    {
%>
       <wiki:Messages div="error" topic="<%=GroupManager.MESSAGES_KEY%>" prefix="Error: "/>
<%
    }
  }
  else
  {
%>
    <div class="formcontainer">
      <div class="instructions">
        This is the wiki group called  <em><%=name%></em>.
        Only members of this group can edit it.
      </div>
    
      <!-- Members -->
      <div class="block">
        <label>Members</label>
        <div class="readonly"><%
            for ( int i = 0; i < members.length; i++ )
            {
              out.println( members[i].getName().trim() );
              if ( i < ( members.length - 1 ) )
              {
                out.println( "<br/>" );
              }
            }
          %></div>
        <div class="description">
          The group&#8217;s membership.
        </div>
      </div>
      
      <div class="instructions">
        <%=modifier%> saved this group on <%=modified%><br/>
        <%=creator%> created it on <%=created%>. 
      </div>
    </div>
<%
  }
%>

