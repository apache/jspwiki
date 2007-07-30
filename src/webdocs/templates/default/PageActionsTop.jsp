<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>
<%
  //WikiContext c = WikiContext.findContext(pageContext);
  //String frontpage = c.getEngine().getFrontPage(); 
%>

<%-- similar to PageActionsBottom, except for accesskeys, More... dropdown, quick2Bottom --%>
<div id="actionsTop" class="pageactions"> 
  <form class="wikiform" method="get" action="" >
  <ul>

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
    <option class="actionsMore" value="" selected='selected'><fmt:message key="actions.more"/></option>

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
    
    <option class="action rawpage" value="<wiki:Link format='url' ><wiki:Param name='skin' value='raw'/></wiki:Link>"
       title="<fmt:message key='actions.rawpage.title' />"><fmt:message key='actions.rawpage' />
    </option>
  
    <wiki:UserCheck status="authenticated">
      <option class="action workflow" value="<wiki:Link jsp='Workflow.jsp' format='url' />" 
        title="<fmt:message key='actions.workflow.title' />"><fmt:message key='actions.workflow' />
      </option>

    </wiki:UserCheck>

    <wiki:Permission permission="createGroups">
      <option class="action creategroup" value="<wiki:Link jsp='NewGroup.jsp' format='url' />" 
        title="<fmt:message key='actions.creategroup.title' />"><fmt:message key='actions.creategroup' />
      </option>
    </wiki:Permission>

    <option value="separator" disabled="disabled" ><fmt:message key='actions.separator' /></option>

    <option class="action index" value="<wiki:LinkTo page='PageIndex' format='url' />"
       title="<fmt:message key='actions.index.title' />"><fmt:message key='actions.index' />
    </option>
  
    <option class="action recentchanges" value="<wiki:LinkTo page='RecentChanges' format='url' />"
        title="<fmt:message key='actions.recentchanges.title'/>" ><fmt:message key='actions.recentchanges' />
    </option>

	<%--
    <option class="action systeminfo" value="<wiki:Link page='SystemInfo' format='url' />"
      title="<fmt:message key='actions.systeminfo.title' />"><fmt:message key='actions.systeminfo' />
    </option>
    --%>
    

  </select>
  </li>
  <li id="morebutton">
    <a href="#" class="action more"><fmt:message key="actions.more"/></a>
  </li>
<%--
  <li>
    <a class="action quick2bottom" href="#footer" title="<fmt:message key='actions.gotobottom' />" >&raquo;</a>
  </li>
--%>
  </ul>

  </form>
</div>