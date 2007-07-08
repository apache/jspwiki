<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext(pageContext);
  String frontpage = c.getEngine().getVariable(c,"jspwiki.frontPage");
%>

<%-- similar to PageActionsBottom, except for accesskeys, More... dropdown, quick2Bottom --%>
<div id="actionsTop" class="pageactions"> 
  <form class="wikiform" method="get" action="" >
  <ul>
  <li>
  <a href="<wiki:LinkTo page='<%=frontpage%>' format='url' />"
    class="action home"
    title="<fmt:message key='actions.home.title' ><fmt:param><%=frontpage%></fmt:param></fmt:message> "><fmt:message key='actions.home' /></a>
  </li>
  
  <wiki:CheckRequestContext context='edit'>
  <li>
      <wiki:PageType type="page">
        <a href="<wiki:Link format='url' />" accesskey="v" class="action view"
          title="<fmt:message key='actions.view.title'/>"><fmt:message 
            key='actions.view'/></a>
      </wiki:PageType>
      <wiki:PageType type="attachment">
        <a href="<wiki:LinkToParent format='url' />" accesskey="v" class="action view"
          title="<fmt:message key='actions.viewparent.title'/>"><fmt:message 
            key='actions.view'/></a>
      </wiki:PageType>
  </li>
  </wiki:CheckRequestContext>
  
  <wiki:CheckRequestContext context='view|info|diff|upload'>
    <wiki:Permission permission="edit">
	<li>
        <wiki:PageType type="page">
          <a href="<wiki:EditLink format='url' />" accesskey="e"  class="action edit"
            title="<fmt:message key='actions.edit.title'/>" ><fmt:message key='actions.edit'/></a>
        </wiki:PageType>
        <wiki:PageType type="attachment">
          <a href="<wiki:BaseURL/>Edit.jsp?page=<wiki:ParentPageName />" accesskey="e" class="action edit"
            title="<fmt:message key='actions.editparent.title'/>" ><fmt:message key='actions.editparent'/></a>
        </wiki:PageType>
    </li>
    </wiki:Permission>
  </wiki:CheckRequestContext>

  <wiki:UserCheck status="notAuthenticated">
  <wiki:CheckRequestContext context='!login'>
    <wiki:Permission permission="login">
	<li>
        <a href="<wiki:Link jsp='Login.jsp' format='url' />" class="action login"
          title="<fmt:message key='actions.login.title'/>"><fmt:message key="actions.login"/></a>
	</li>
    </wiki:Permission>
  </wiki:CheckRequestContext>
  </wiki:UserCheck>

  <wiki:UserCheck status="authenticated">
	<li>
      <a href="<wiki:Link jsp='Logout.jsp' format='url' />" class="action logout"
        title="<fmt:message key='actions.logout.title'/>"><fmt:message key="actions.logout"/></a>
    </li>
  </wiki:UserCheck>


  <%-- FIXME : to be moved into tabs of UserPreferences --%>
  <wiki:CheckRequestContext context='viewGroup'>
    <wiki:Permission permission="editGroup">
	<li>
        <wiki:Link jsp="EditGroup.jsp"><%-- class="action editgroup"--%>
          <wiki:Param name="group" value="${param.group}" />
          <fmt:message key="actions.editgroup" />
        </wiki:Link>
    </li>
    </wiki:Permission>
    <wiki:Permission permission="deleteGroup"> 
    <li>
      <a onclick="return confirmDelete()" class="action deletegroup"
              href="<wiki:Link jsp='DeleteGroup.jsp' format='url'><wiki:Param name='group'value="${param.group}" /></wiki:Link>"><fmt:message key="actions.deletegroup"/></a>
    </li>
    </wiki:Permission>
  </wiki:CheckRequestContext>
   
  <wiki:CheckRequestContext context='editGroup'>
    <li>
      <wiki:Link jsp="Group.jsp"><%-- class="action viewgroup"--%>
        <wiki:Param name="group" value="${param.group}" />
        <fmt:message key="actions.viewgroup"/>
      </wiki:Link>
    </li>
  </wiki:CheckRequestContext>

  <%-- more actions dropdown -- converted to popup by javascript 
       so all basic actions are accessible even if js is not avail --%>
  <li>
  <select name="actionsMore" id="actionsMore"
      onchange="if ((this.selectedIndex != 0) &amp;&amp; (!this.options[this.selectedIndex].disabled)) location.href=this.form.action=this.options[this.selectedIndex].value; this.selectedIndex = 0;">
    <option class="actionsMore" value="" checked='checked'><fmt:message key="actions.more"/></option>

    <wiki:CheckRequestContext context='!prefs'>
    <wiki:CheckRequestContext context='!preview'>
      <option class="action prefs" value="<wiki:Link jsp='UserPreferences.jsp' format='url' />">
        <fmt:message key="actions.prefs" />
      </option>
    </wiki:CheckRequestContext>
    </wiki:CheckRequestContext>
    
    <wiki:CheckRequestContext context='view|info|diff|upload'>
    <wiki:PageExists>  
    <wiki:Permission permission="comment">
      <wiki:PageType type="page">
        <option class="action comment" value="<wiki:CommentLink format='url' />" 
          title="<fmt:message key='actions.comment.title' />"><fmt:message key="actions.comment" />
		</option>
      </wiki:PageType>
      <wiki:PageType type="attachment">
         <option class="action comment" value="<wiki:BaseURL/>Comment.jsp?page=<wiki:ParentPageName />"
           title="<fmt:message key='actions.comment.title' />"><fmt:message key="actions.comment" />
		</option>
      </wiki:PageType>
    </wiki:Permission>
    </wiki:PageExists>  
    </wiki:CheckRequestContext>
    
    <wiki:CheckRequestContext context='view|info|diff|upload|login|edit'>
    <option value="separator" disabled="disabled" ><fmt:message key='actions.separator' /></option>
    </wiki:CheckRequestContext>

    <option class="action index" value="<wiki:LinkTo page='PageIndex' format='url' />"
       title="<fmt:message key='actions.index.title' />"><fmt:message key='actions.index' />
    </option>
  
    <option class="action recentchanges" value="<wiki:LinkTo page='RecentChanges' format='url' />"
        title="<fmt:message key='actions.recentchanges.title'/>" ><fmt:message key='actions.recentchanges' />
    </option>

    <option class="action systeminfo" value="<wiki:Link page='SystemInfo' format='url' />"
      title="<fmt:message key='actions.systeminfo.title' />"><fmt:message key='actions.systeminfo' />
    </option>
    
    <wiki:UserCheck status="authenticated">
      <option value="separator" disabled="disabled" class="actionSeparator"><fmt:message key='actions.separator' /></option>

      <option class="action workflow" value="<wiki:Link jsp='Workflow.jsp' format='url' />" 
        title="<fmt:message key='actions.workflow.title' />"><fmt:message key='actions.workflow' />
      </option>

    </wiki:UserCheck>

    <wiki:Permission permission="createGroups">
      <option class="action creategroup" value="<wiki:Link jsp='NewGroup.jsp' format='url' />" 
        title="<fmt:message key='actions.creategroup.title' />"><fmt:message key='actions.creategroup' />
      </option>
    </wiki:Permission>

  </select>
  </li>
  <li id="morebutton">
    <a href="#" class="action more"><fmt:message key="actions.more"/></a>
  </li>
  <li>
    <a class="action quick2bottom" href="#footer" title="<fmt:message key='actions.gotobottom' />" >&raquo;</a>
  </li>
  </ul>

  </form>
</div>