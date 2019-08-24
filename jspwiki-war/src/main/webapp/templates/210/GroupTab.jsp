<%--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%>

<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.text.MessageFormat" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.wiki.WikiContext" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.PrincipalComparator" %>
<%@ page import="org.apache.wiki.auth.authorize.Group" %>
<%@ page import="org.apache.wiki.auth.authorize.GroupManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%!
  String printWikiGroupPutGroup( Group group, String name, boolean cursor, PageContext pageContext)
  {
    Principal[] m = group.members();
    java.util.Arrays.sort( m, new PrincipalComparator() );

    String delim = "\", \"";

    StringBuffer ss = new StringBuffer();
    MessageFormat mf = null;
    Object[] args = null;

      ss.append( "WikiGroup.putGroup( \"" );

      ss.append( name );
      ss.append( delim );

      for( int j=0; j < m.length; j++ ) { ss.append( m[j].getName().trim()+"\\n" ); }

      ss.append( delim );
      mf = new MessageFormat(LocaleSupport.getLocalizedMessage(pageContext, "grp.createdon") );
      args = new Object[]{(group.getCreated()==null) ? "" : Preferences.renderDate(WikiContext.findContext( pageContext ), group.getCreated(),Preferences.TimeFormat.DATETIME), group.getCreator()};
      ss.append( mf.format( args ) );

      mf = new MessageFormat(LocaleSupport.getLocalizedMessage(pageContext, "grp.lastmodified") );
      args = new Object[]{(group.getLastModified()==null) ? "" : Preferences.renderDate(WikiContext.findContext( pageContext ), group.getLastModified(),Preferences.TimeFormat.DATETIME), group.getModifier()};
      ss.append( mf.format( args ) );

      ss.append( "\", " );
      ss.append( ( cursor ) ? "true" : "false" );

      ss.append( ");\n" );


    return ss.toString();
  }
%>

<wiki:Messages div="error" topic="<%=GroupManager.MESSAGES_KEY%>" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"group.errorprefix")%>'/>

<table id='wikigroups' class='wikitable' >
<tr>
  <th scope="col"><fmt:message key="group.name" /></th>
  <th scope="col"><fmt:message key="group.members" /></th>
</tr>
<tr>
  <td id="groupnames" rowspan="2">
    <div id="grouptemplate"
            style="display:none; "
            title='<fmt:message key="grp.groupnames.title"/>'
          onclick="WikiGroup.toggle(); WikiGroup.onMouseOverGroup(this);"
      onmouseover="WikiGroup.onMouseOverGroup(this);" ></div>

    <wiki:Permission permission="createGroups">
    <div id="groupfield"
      onmouseover="WikiGroup.onMouseOverGroup(this);" >
      <input type="text" size="30"
               id="newgroup"
            value='<fmt:message key="grp.newgroupname"/>'
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
      <input type="button" disabled="disabled"
             name="saveButton" id="saveButton"
            value='<fmt:message key="grp.savegroup"/>'
          onclick="WikiGroup.onSubmit( this.form, '<wiki:Link format='url' jsp='EditGroup.jsp' />' );" /></div>

      <wiki:Permission permission="createGroups">
      <div>
      <input type="button" disabled="disabled"
             name="createButton" id="createButton"
            value='<fmt:message key="grp.savenewgroup"/>'
            style="display:none; "
          onclick="WikiGroup.onSubmitNew( this.form, '<wiki:Link format='url' jsp='NewGroup.jsp' />' );" /></div>
      </wiki:Permission>

      <div>
      <input type="button" disabled="disabled"
             name="cancelButton" id="cancelButton"
            value='<fmt:message key="grp.cancel"/>'
          onclick="WikiGroup.toggle();" /></div>

      <wiki:Permission permission="deleteGroup">
      <div>
      <input type="button" disabled="disabled"
             name="deleteButton" id="deleteButton"
            value='<fmt:message key="grp.deletegroup"/>'
          onclick="confirm( '<fmt:message key="grp.deletegroup.confirm"/>' )
                && WikiGroup.onSubmit( this.form, '<wiki:Link format='url' jsp='DeleteGroup.jsp' />' );" /></div>
      </wiki:Permission>
    </form>
  </td>
  </tr>
  <tr valign="top">
  <td>
    <div class="formhelp"><fmt:message key="grp.formhelp"/></div>
    <p id="groupinfo" class="formhelp"></p>
  </td>
  </tr>
</table>

<h3><fmt:message key="grp.allgroups"/></h3>
<p><wiki:Translate>[{Groups}]</wiki:Translate></p>


<%
  String groupname = request.getParameter( "group" );
%>

<script type="text/javascript">//<![CDATA[
<%
  WikiContext c = WikiContext.findContext( pageContext );
  Principal[] roles = c.getWikiSession().getRoles();

  for( int i = 0; i < roles.length; i++ )
  {
    if ( roles[i] instanceof GroupPrincipal ) /* bugfix */
    {
      String name = roles[i].getName();
      Group group = c.getEngine().getGroupManager().getGroup( name );

      %><%= printWikiGroupPutGroup( group, name, name.equals( groupname ), pageContext )  %><%
    }
  }
%>
//]]></script>

<%--
WikiGroup.putGroup( "Group1qsdf qsdf qsdf qsdf qsdffsdfq en nog een beetje langer he", "Member1\nMember2\nMember3\nMember4\nMember5\nMember6", "createdon", "createdby", "changedon", "changedby" );
--%>
