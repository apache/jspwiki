<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>
<%
  String homepage = "Main";
  WikiContext wikiContext = WikiContext.findContext(pageContext);
  try 
  { 
    homepage = wikiContext.getEngine().getFrontPage(); 
  } 
  catch( Exception  e )  { /* dont care */ } ;
%>

<%-- similar to PageActionsBottom, except for accesskeys, More... dropdown, quick2Bottom --%>
<div id="actionsTop" class="pageactions"> 

  <span class="actionHome">
    <a href="<wiki:LinkTo page='<%= homepage %>' format='url' />"
       title="<fmt:message key='actions.home.title' ><fmt:param><%=homepage%></fmt:param></fmt:message> "><fmt:message key='actions.home' /></a>
  </span>
  
  <wiki:CheckRequestContext context='info|diff|upload|edit'>
    <span class="actionView">
      <wiki:PageType type="page">
        <a href="<wiki:Link format='url' />" accesskey="v" 
          title="<fmt:message key='actions.view.title'/>"><fmt:message 
            key='actions.view'/></a>
      </wiki:PageType>
      <wiki:PageType type="attachment">
        <a href="<wiki:LinkToParent format='url' />" accesskey="v" 
          title="<fmt:message key='actions.viewparent.title'/>"><fmt:message 
            key='actions.view'/></a>
      </wiki:PageType>
    </span>
  </wiki:CheckRequestContext>
  
  <wiki:CheckRequestContext context='!info'>
  <span class="actionInfo" >
      <a href="<wiki:PageInfoLink format='url' />" id="moreinfo" accesskey="i" 
        title="<fmt:message key='actions.info.title'/>"><fmt:message 
          key='actions.info'/></a>
  </span>
  </wiki:CheckRequestContext>

  <wiki:CheckRequestContext context='view|info|diff|upload'>
    <wiki:Permission permission="edit">
      <span class="actionEdit">
        <wiki:PageType type="page">
          <a href="<wiki:EditLink format='url' />" accesskey="e" 
            title="<fmt:message key='actions.edit.title'/>" ><fmt:message key='actions.edit'/></a>
        </wiki:PageType>
        <wiki:PageType type="attachment">
          <a href="<wiki:BaseURL/>Edit.jsp?page=<wiki:ParentPageName />" accesskey="e" 
            title="<fmt:message key='actions.editparent.title'/>" ><fmt:message key='actions.editparent'/></a>
        </wiki:PageType>
      </span>
    </wiki:Permission>
  </wiki:CheckRequestContext>

  <wiki:UserCheck status="notAuthenticated">
  <wiki:CheckRequestContext context='!login'>
    <wiki:Permission permission="login">
      <span class="actionLogin">
        <a href="<wiki:Link jsp='Login.jsp' format='url' />"
          title="<fmt:message key='actions.login.title'/>"><fmt:message key="actions.login"/></a>
      </span>
    </wiki:Permission>
  </wiki:CheckRequestContext>
  </wiki:UserCheck>

  <wiki:UserCheck status="authenticated">
    <span class="actionLogout">
      <a href="<wiki:Link jsp='Logout.jsp' format='url' />"
        title="<fmt:message key='actions.logout.title'/>"><fmt:message key="actions.logout"/></a>
    </span>
  </wiki:UserCheck>


  <%-- FIXME : to be moved into tabs of UserPreferences --%>
  <wiki:CheckRequestContext context='viewGroup'>
    <wiki:Permission permission="editGroup">
      <span class="actionsEditGroup">
        <wiki:Link jsp="EditGroup.jsp">
          <wiki:Param name="group" value="${param.group}" />
          <fmt:message key="actions.editgroup"/>
        </wiki:Link>
      </span>
    </wiki:Permission>
    <wiki:Permission permission="deleteGroup"> 
      <span class="actionsDeleteGroup">
        <a onclick="return confirmDelete()" href="<wiki:Link jsp='DeleteGroup.jsp' format='url'><wiki:Param name='group' value="${param.group}" /></wiki:Link>"><fmt:message key="actions.deletegroup"/></a>
      </span>
    </wiki:Permission>
  </wiki:CheckRequestContext>
   
  <wiki:CheckRequestContext context='editGroup'>
    <span class="actionsEditGroup">
      <wiki:Link jsp="Group.jsp">
        <wiki:Param name="group" value="${param.group}" />
        <fmt:message key="actions.viewgroup"/>
      </wiki:Link>
    </span>
  </wiki:CheckRequestContext>

  <%-- more actions dropdown -- converted to popup by javascript 
       so all basic actions are accessible even if js is not avail --%>
  <form class="wikiform" id="actionsmenu" method="get" action="" style="display:inline;">
  <select name="actionsMore" id="actionsMore"
      onchange="if ((this.selectedIndex != 0) && (!this.options[this.selectedIndex].disabled)) location.href=this.form.action=this.options[this.selectedIndex].value; this.selectedIndex = 0;    <option class="actionsMore" value="" checked='checked'><fmt:message key="actions.more"/></option>

    <wiki:CheckRequestContext context='!prefs'>
    <wiki:CheckRequestContext context='!preview'>
      <option class="actionPrefs" value="<wiki:Link jsp='UserPreferences.jsp' format='url' />">
        <fmt:message key="actions.prefs" />
      </option>
    </wiki:CheckRequestContext>
    </wiki:CheckRequestContext>
    
    <wiki:CheckRequestContext context='view|info|diff|upload'>
    <wiki:Permission permission="comment">
      <wiki:PageType type="page">
        <option class="actionComment" value="<wiki:CommentLink format='url' />" 
          title="<fmt:message key='actions.comment.title' />"><fmt:message key="actions.comment" />
		</option>
      </wiki:PageType>
      <wiki:PageType type="attachment">
         <option class="actionComment" value="<wiki:BaseURL/>Comment.jsp?page=<wiki:ParentPageName />"
           title="<fmt:message key='actions.comment.title' />"><fmt:message key="actions.comment" />
		</option>
      </wiki:PageType>
    </wiki:Permission>
    </wiki:CheckRequestContext>
    
    <wiki:CheckRequestContext context='view|info|diff|upload|login|edit'>
    <option value="separator" disabled="disabled" class="actionSeparator"><fmt:message key='actions.separator' /></option>
    </wiki:CheckRequestContext>

    <option class="actionIndex" value="<wiki:LinkTo page='PageIndex' format='url' />"
       title="<fmt:message key='actions.index.title' />"><fmt:message key='actions.index' />
    </option>
  
    <option class="actionRecentChanges" value="<wiki:LinkTo page='RecentChanges' format='url' />"
        title="<fmt:message key='actions.recentchanges.title'/>" ><fmt:message key='actions.recentchanges' />
    </option>

    <option class="actionSystemInfo" value="<wiki:Link page='SystemInfo' format='url' />"
      title="<fmt:message key='actions.systeminfo.title' />"><fmt:message key='actions.systeminfo' />
    </option>
    
    <wiki:UserCheck status="authenticated">

      <option value="separator" disabled="disabled" class="actionSeparator"><fmt:message key='actions.separator' /></option>

      <option class="actionWorkflow" value="<wiki:Link jsp='Workflow.jsp' format='url' />" 
        title="<fmt:message key='actions.workflow.title' />"><fmt:message key='actions.workflow' />
      </option>

    </wiki:UserCheck>

    <wiki:Permission permission="createGroups">
      <option class="actionCreateGroup" value="<wiki:Link jsp='NewGroup.jsp' format='url' />" 
        title="<fmt:message key='actions.creategroup.title' />"><fmt:message key='actions.creategroup' />
      </option>
    </wiki:Permission>

  </select>
  </form>

  <span id="actionsMoreLink" style="display:none;">
    <a href="#"><fmt:message key="actions.more"/></a>
    <div id='actionsMorePopup'><ul id='actionsMorePopupItems' ></ul></div>
  </span>

  <span class="quick2bottom"><a href="#footer" title="<fmt:message key='actions.gotobottom' />" >&raquo;</a></span>

</div>