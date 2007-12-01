<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.PrincipalComparator" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.Group" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.GroupManager" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>
<%!
    Logger log = Logger.getLogger("JSPWiki");
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
  name = TextUtil.replaceEntities(name);
%>


<script language="javascript" type="text/javascript">
function confirmDelete()
{
  var reallydelete = confirm("<fmt:message key="group.areyousure"><fmt:param><%=name%></fmt:param></fmt:message>");

  return reallydelete;
}
</script>

<wiki:TabbedSection defaultTab="${param.tab}">
  <wiki:Tab id="logincontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "group.tab")%>'>
  <h3><%=name%></h3>

<%
  if ( group == null )
  {
    WikiContext c = WikiContext.findContext( pageContext );

    if ( c.getWikiSession().getMessages( GroupManager.MESSAGES_KEY ).length == 0 )
    {
%>
    <fmt:message key="group.doesnotexist"/>
    <wiki:Permission permission="createGroups">
      <fmt:message key="group.createsuggestion">
        <fmt:param><wiki:Link jsp="NewGroup.jsp">
                      <wiki:Param name="group" value="<%=name%>" />
                      <wiki:Param name="group" value="<%=name%>" />
                      <fmt:message key="group.createit"/>
                   </wiki:Link>
        </fmt:param>
      </fmt:message>
    </wiki:Permission>
<%
    }
    else
    {
%>
       <wiki:Messages div="error" topic="<%=GroupManager.MESSAGES_KEY%>" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"group.errorprefix")%>'/>
<%
    }
  }
  else
  {
%>
 <table class="wikitable">
    <tr>
      <th>Group Name</th>
      <td>
        <fmt:message key="group.groupintro">
          <fmt:param><em><%=name%></em></fmt:param>
        </fmt:message>
      </td>
    </tr>
    <!-- Members -->
    <tr>
      <th><fmt:message key="group.members"/>
      </th>
      <td><%
            for ( int i = 0; i < members.length; i++ )
            {
              out.println( members[i].getName().trim() );
              if ( i < ( members.length - 1 ) )
              {
                out.println( "<br/>" );
              }
            }
          %></td>
          <%--fmt:message key="group.membership"/--%>
      </tr>
      <tr>
        <td colspan="2">
        <fmt:message key="group.modifier">
           <fmt:param><%=modifier%></fmt:param>
           <fmt:param><%=modified%></fmt:param>
        </fmt:message>
        </td>
      </tr>
      <tr>
        <td colspan="2">
        <fmt:message key="group.creator">
           <fmt:param><%=creator%></fmt:param>
           <fmt:param><%=created%></fmt:param>
        </fmt:message>
        </td>
      </tr>
    </table>
<%
  }
%>
</wiki:Tab>
</wiki:TabbedSection>
