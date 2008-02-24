<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
  //WikiContext c = WikiContext.findContext(pageContext);
  //String frontpage = c.getEngine().getFrontPage(); 
%>

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
    
    <wiki:CheckRequestContext context='view|info|diff|upload|edit|comment|preview' >
    <option class="action rawpage" value="<wiki:Link format='url' ><wiki:Param name='skin' value='raw'/></wiki:Link>"
       title="<fmt:message key='actions.rawpage.title' />"><fmt:message key='actions.rawpage' />
    </option>
    </wiki:CheckRequestContext>
  
    <wiki:CheckRequestContext context='!workflow'>
    <wiki:UserCheck status="authenticated">
      <option class="action workflow" value="<wiki:Link jsp='Workflow.jsp' format='url' />" 
        title="<fmt:message key='actions.workflow.title' />"><fmt:message key='actions.workflow' />
      </option>
    </wiki:UserCheck>
    </wiki:CheckRequestContext>

    <wiki:Permission permission="createGroups">
      <option class="action creategroup" value="<wiki:Link jsp='NewGroup.jsp' format='url' />" 
        title="<fmt:message key='actions.creategroup.title' />"><fmt:message key='actions.creategroup' />
      </option>
    </wiki:Permission>

  </select>
  </li>
  <li id="morebutton" style="display:none">
    <a href="#" class="action more"><fmt:message key="actions.more"/></a>
    <div id="moremenu" >
      <wiki:InsertPage page="MoreMenu" />
    </div>
  </li>
<%--
  <li>
    <a class="action quick2bottom" href="#footer" title="<fmt:message key='actions.gotobottom' />" >&raquo;</a>
  </li>
--%>
  </ul>

  </form>
</div>
