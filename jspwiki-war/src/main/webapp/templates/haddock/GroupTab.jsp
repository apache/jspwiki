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

  /* json=[ group1, group2, .. ] */
  String groupsToJson(PageContext pageContext,String groupname) throws NoSuchPrincipalException  {

    WikiContext c = WikiContext.findContext( pageContext );
    Principal[] roles = c.getWikiSession().getRoles();

    StringBuffer result = new StringBuffer();

    result.append( "[" );
    for( int i = 0; i < roles.length; i++ )
    {
      if ( roles[i] instanceof GroupPrincipal ) /* bugfix */
      {
        String name = roles[i].getName();
        Group group = c.getEngine().getGroupManager().getGroup( name );

        result.append( groupToJson( group, name, name.equals( groupname ), pageContext ) );
        
        if( i+1<roles.length ){ result.append( ",\n" ); }
      }
    }
    result.append( "\n]" );
    return result.toString();

  }
  
/* json= {"name":"..","members":["a","b","c"] ,"createdon":"...","lastmodified":"...","cursor":yes|no} */

  String groupToJson( Group group, String name, boolean cursor, PageContext pageContext )
  {
    Principal[] m = group.members();
    java.util.Arrays.sort( m, new PrincipalComparator() );

    StringBuffer ss = new StringBuffer();
    MessageFormat mf = null;
    Object[] args = null;
      
      ss.append( "{ \"name\":\"" );
      
      ss.append( name );
      ss.append( "\",\"members\":[" );
      
      for( int j=0; j < m.length; j++ ) { 
        ss.append( "\""+m[j].getName().trim()+"\"" ); 
        if(j+1 < m.length) ss.append( "," );
      }
      
      ss.append( "],\"createdon\":\"" );
      mf = new MessageFormat(LocaleSupport.getLocalizedMessage(pageContext, "grp.createdon") );
      args = new Object[]{(group.getCreated()==null) ? "" : Preferences.renderDate(WikiContext.findContext( pageContext ), group.getCreated(),Preferences.TimeFormat.DATETIME), group.getCreator()};
      ss.append( mf.format( args ) );
      
      ss.append( "\",\"lastmodified\":\"" );
      mf = new MessageFormat(LocaleSupport.getLocalizedMessage(pageContext, "grp.lastmodified") );
      args = new Object[]{(group.getLastModified()==null) ? "" : Preferences.renderDate(WikiContext.findContext( pageContext ), group.getLastModified(),Preferences.TimeFormat.DATETIME), group.getModifier()};
      ss.append( mf.format( args ) );
      
      ss.append( "\",\"cursor\":" );
      ss.append( ( cursor ) ? "true" : "false" );
      
      ss.append( "}" );

    return ss.toString();
  
  }

%>

<h3><fmt:message key="group.tab" /></h3>


<wiki:Messages div="error" topic="<%=GroupManager.MESSAGES_KEY%>" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"group.errorprefix")%>'/>

<div class="form-group">
  <label class="control-label form-col-20"><fmt:message key="group.name" /></label>

    <%--
    <div id="grouptemplate" 
            style="display:none; " 
            title='<fmt:message key="grp.groupnames.title"/>'
          onclick="WikiGroup.toggle(); WikiGroup.onMouseOverGroup(this);"
      onmouseover="WikiGroup.onMouseOverGroup(this);" >
    </div>
    --%>
    <wiki:Permission permission="createGroups">
      <input type="text" size="30" 
           class="form-control form-col-50"
               id="newgroup"
            value='<fmt:message key="grp.newgroupname"/>'
           onblur="if( this.value == '' ) { this.value = this.defaultValue; }; " 
          onfocus="if( this.value == this.defaultValue ) { this.value = ''; WikiGroup.onClickNew(); }; ">
    </wiki:Permission>
</div>

<div class="form-group">
  <label class="control-label form-col-20"><fmt:message key="group.members" /></label>

  <textarea class="form-control form-col-50" rows="8" cols="30" disabled="disabled"
              name="membersfield" id="membersfield" ></textarea>
</div>
<div class="form-group">
  <form action="<wiki:Link format='url' jsp='Group.jsp'/>" 
              id="groupForm" 
          method="post" accept-charset="<wiki:ContentEncoding />" >

      <input type="hidden" name="group"   value="" />
      <input type="hidden" name="members" value="" />
      <input type="hidden" name="action"  value="save" />
      <input type="button" disabled="disabled"
          class="btn btn-primary  form-col-offset-20"
             name="saveButton" id="saveButton" 
            value='<fmt:message key="grp.savegroup"/>' 
          onclick="WikiGroup.onSubmit( this.form, '<wiki:Link format='url' jsp='EditGroup.jsp' />' );" />

      <wiki:Permission permission="createGroups">
      <input type="button" disabled="disabled"  
           class="btn btn-default"
             name="createButton" id="createButton"
            value='<fmt:message key="grp.savenewgroup"/>' 
            style="display:none; "
          onclick="WikiGroup.onSubmitNew( this.form, '<wiki:Link format='url' jsp='NewGroup.jsp' />' );" />
      </wiki:Permission>

      <input type="button" disabled="disabled"
            class="btn btn-default"
             name="cancelButton" id="cancelButton" 
            value='<fmt:message key="grp.cancel"/>' 
          onclick="WikiGroup.toggle();" />

      <wiki:Permission permission="deleteGroup">
      <input type="button" disabled="disabled" 
            class="btn btn-danger"
             name="deleteButton" id="deleteButton"
            value='<fmt:message key="grp.deletegroup"/>' 
          onclick="confirm( '<fmt:message key="grp.deletegroup.confirm"/>' ) 
                && WikiGroup.onSubmit( this.form, '<wiki:Link format='url' jsp='DeleteGroup.jsp' />' );" />
      </wiki:Permission>
    </form>
   
  <p class="help-block form-col-offset-20"><fmt:message key="grp.formhelp"/></p>
  <p id="groupinfo" class="help-block"></p>
</div>

<div class="form-group">
  <label class="control-label form-col-20"><fmt:message key="grp.allgroups"/></label>
  <p class="help-block form-col-75"><wiki:Translate>[{Groups}]</wiki:Translate></p>

  <div id="groups" class="hidden json"><%= groupsToJson(pageContext,request.getParameter( "group" )) %></div>

<%--
<script>
console.log( $('groups').get('text'), JSON.decode( $('groups').get('text') ) );
</script>
--%>
<%--
WikiGroup.putGroup( "Group1qsdf qsdf qsdf qsdf qsdffsdfq en nog een beetje langer he", "Member1\nMember2\nMember3\nMember4\nMember5\nMember6", "createdon", "createdby", "changedon", "changedby" );
--%>

</div>