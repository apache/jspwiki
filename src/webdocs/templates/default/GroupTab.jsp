<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.WikiContext" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.PrincipalComparator" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.Group" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.GroupManager" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>

<%!
  String printWikiGroupPutGroup( Group group, String name, boolean cursor)
  {
    Principal[] m = group.members();
    java.util.Arrays.sort( m, new PrincipalComparator() );

    String delim = "\", \"";
      
    StringBuffer ss = new StringBuffer();
      
      ss.append( "WikiGroup.putGroup( \"" );
      
      ss.append( name );
      ss.append( delim );
      
      for( int j=0; j < m.length; j++ ) { ss.append( m[j].getName().trim()+"\\n" ); }
      
      ss.append( delim );
      ss.append( "Created on " );
      ss.append( (group.getCreated()==null) ? "" : group.getCreated().toString() );
      ss.append( " by " );
      ss.append( group.getCreator() );
      ss.append( "<br />Last modified on " );
      ss.append( (group.getLastModified()==null) ? "" : group.getLastModified().toString() );
      ss.append( " by " );
      ss.append( group.getModifier() );
      ss.append( "\", " );
      ss.append( ( cursor ) ? "true" : "false" );
      
      ss.append( ");\n" );


    return ss.toString();
  }
%>

<wiki:Messages div="error" topic="<%=GroupManager.MESSAGES_KEY%>" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"group.errorprefix")%>'/>

<table id='wikigroups' class='wikitable' >
<tr>
  <th><fmt:message key="group.name" /></th>
  <th><fmt:message key="group.members" /></th>
</tr>
<tr>
  <td id="groupnames" rowspan="2">
    <div id="grouptemplate" 
            style="display:none; " 
            title="Click to edit this group"
          onclick="WikiGroup.toggle(); WikiGroup.onMouseOverGroup(this);"
      onmouseover="WikiGroup.onMouseOverGroup(this);" ></div>

    <wiki:Permission permission="createGroups">
    <div id="groupfield" 
      onmouseover="WikiGroup.onMouseOverGroup(this);" >
      <input type="text" size="30" 
               id="newgroup"
            value="(new group name)"
           onblur="if( this.value == '' ) { this.value = this.defaultValue; }; " 
          onfocus="if( this.value == this.defaultValue ) { this.value = ''; WikiGroup.onClickNew(); }; "/>
    </div>
    </wiki:Permission>
  </td>
  <td id="groupmembers">
    <div style="float:left;">
    <textarea rows="8" cols="30" disabled="disabled"
              name="membersfield" id="membersfield" ></textarea>
    </div>
    <form action="<wiki:Link format='url' jsp='Group.jsp'/>" 
              id="groupForm" 
          method="post" accept-charset="<wiki:ContentEncoding />" >
      <div>
      <input type="hidden" name="group"   value="" />
      <input type="hidden" name="members" value="" />
      <input type="hidden" name="action"  value="save" />
      <input type="button" disabled
             name="saveButton" id="saveButton" 
            value="Save Group"  
          onclick="WikiGroup.onSubmit( this.form, '<wiki:Link format='url' jsp='EditGroup.jsp' />' );" /></div>

      <wiki:Permission permission="createGroups">
      <div>
      <input type="button" disabled="disabled"  
             name="createButton" id="createButton"
            value="Save New Group" 
            style="display:none; "
          onclick="WikiGroup.onSubmitNew( this.form, '<wiki:Link format='url' jsp='NewGroup.jsp' />' );" /></div>
      </wiki:Permission>

      <div>
      <input type="button" disabled="disabled"
             name="cancelButton" id="cancelButton" 
            value="Cancel" 
          onclick="WikiGroup.toggle();" /></div>

      <wiki:Permission permission="deleteGroup">
      <div>
      <input type="button" disabled="disabled" 
             name="deleteButton" id="deleteButton"
            value="Delete Group" 
          onclick="confirm( 'Please confirm that you want to delete this group permanently!' ) 
                && WikiGroup.onSubmit( this.form, '<wiki:Link format='url' jsp='DeleteGroup.jsp' />' );" /></div>
      </wiki:Permission>
    </form>
  </td>
  </tr>
  <tr valign="top">
  <td>
    <div class="formhelp">The membership for this group. Only members of this group can edit it.
    <br />Enter each user&#8217;s wiki name or full name, separated by carriage returns.</div>
    <p id="groupinfo" class="formhelp"></p>
  </td>
  </tr>
</table>

<h3>All Groups</h3>
<p><wiki:Translate>[{Groups}]</wiki:Translate></p>


<%
  String groupname = request.getParameter( "group" );
%>
 
<script type="text/javascript">
//<![CDATA[
<%
  WikiContext c = WikiContext.findContext( pageContext );
  Principal[] roles = c.getWikiSession().getRoles();

  for( int i = 0; i < roles.length; i++ )
  {
    if ( roles[i] instanceof GroupPrincipal ) /* bugfix */
    {
      String name = roles[i].getName();
      Group group = c.getEngine().getGroupManager().getGroup( name );

      %><%= printWikiGroupPutGroup( group, name, name.equals( groupname ) )  %><%
    }
  }
%>
//]]>
</script>

<%--
WikiGroup.putGroup( "Group1qsdf qsdf qsdf qsdf qsdffsdfq en nog een beetje langer he", "Member1\nMember2\nMember3\nMember4\nMember5\nMember6", "createdon", "createdby", "changedon", "changedby" );
--%>
