<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%
  /* see commonheader.jsp */
  String prefDateFormat     = (String) session.getAttribute("prefDateFormat");
  String prefTimeZone       = (String) session.getAttribute("prefTimeZone");
  String group              = request.getParameter( "group" );
%>
<fmt:setBundle basename="templates.DefaultResources"/>
  <div class="pageactions">
  
    <span class='quick2Top'><a href="#Top" title='<fmt:message key="actions.gototop"/>' >&nbsp;</a></span>

    <wiki:CheckRequestContext context='info|diff|upload|edit'>
      <span>
        <wiki:PageType type="page">
          <wiki:LinkTo><fmt:message key="actions.view"/></wiki:LinkTo>
        </wiki:PageType>
        <wiki:PageType type="attachment">
          <wiki:LinkToParent><fmt:message key="actions.view"/></wiki:LinkToParent>
        </wiki:PageType>
      </span>
    </wiki:CheckRequestContext>
  
    <wiki:CheckRequestContext context='view|info|diff|upload'>
      <wiki:Permission permission="edit">
        <span>
          <wiki:PageType type="page">
            <wiki:EditLink><fmt:message key="actions.edit"/></wiki:EditLink>
          </wiki:PageType>
        </span>
      </wiki:Permission>
  
      <wiki:Permission permission="comment">
        <span>
          <wiki:PageType type="page">
            <wiki:CommentLink><fmt:message key="actions.addcomment"/></wiki:CommentLink>
          </wiki:PageType>
          <wiki:PageType type="attachment">
            <a href="Comment.jsp?page=<wiki:ParentPageName />"
               title="<fmt:message key="actions.addcommenttoparent"/>" ><fmt:message key="actions.addcomment"/></a>
          </wiki:PageType>
        </span>
      </wiki:Permission>
    </wiki:CheckRequestContext>
  
    <wiki:PageExists>
      <wiki:CheckRequestContext context='view|info|diff|edit'>
        <wiki:PageType type="page">
          <wiki:Permission permission="upload">
            <span>
              <a href="<wiki:UploadLink format='url' />"><fmt:message key="actions.upload"/></a>
            </span>
          </wiki:Permission>
        </wiki:PageType>
      </wiki:CheckRequestContext>
      <span>
        <wiki:CheckRequestContext context='view|diff|edit|upload'>
          <wiki:PageInfoLink><fmt:message key="actions.info"/></wiki:PageInfoLink>
        </wiki:CheckRequestContext>
      </span>
    </wiki:PageExists>
  
    <wiki:CheckRequestContext context='!prefs'>
      <wiki:CheckRequestContext context='!preview'>
        <span>
          <wiki:Link jsp="UserPreferences.jsp"><fmt:message key="actions.prefs"/></wiki:Link>
        </span>
      </wiki:CheckRequestContext>
    </wiki:CheckRequestContext>
    
    <wiki:UserCheck status="notAuthenticated">
      <wiki:Permission permission="login">
        <span>
          <wiki:Link jsp="Login.jsp"><fmt:message key="actions.login"/></wiki:Link>
        </span>
      </wiki:Permission>
    </wiki:UserCheck>
    
    <wiki:CheckRequestContext context='viewGroup'>
      <wiki:Permission permission="editGroup">
        <span>
          <wiki:Link jsp="EditGroup.jsp">
            <wiki:Param name="group" value="<%=group%>" />
            <fmt:message key="actions.editgroup"/>
          </wiki:Link>
        </span>
      </wiki:Permission>
      <wiki:Permission permission="deleteGroup">
        <span>
          <a onclick="return confirmDelete()" href="<wiki:Link jsp='DeleteGroup.jsp' format='url'><wiki:Param name='group' value='<%=group%>' /></wiki:Link>"><fmt:message key="actions.deletegroup"/></a>
        </span>
      </wiki:Permission>
    </wiki:CheckRequestContext>
    
    <wiki:CheckRequestContext context='editGroup'>
      <span>
        <wiki:Link jsp="Group.jsp">
          <wiki:Param name="group" value="<%=group%>" />
          <fmt:message key="actions.viewgroup"/>
        </wiki:Link>
      </span>
    </wiki:CheckRequestContext>
    
    <wiki:Permission permission="createGroups">
      <span>
        <wiki:Link jsp="NewGroup.jsp"><fmt:message key="actions.creategroup"/></wiki:Link>
      </span>
    </wiki:Permission>

    <wiki:UserCheck status="authenticated">
      <span>
        <wiki:Link jsp="Workflow.jsp"><fmt:message key="actions.workflow"/></wiki:Link>
      </span>
      <span>
        <wiki:Link jsp="Logout.jsp"><fmt:message key="actions.logout"/></wiki:Link>
      </span>
    </wiki:UserCheck>
  
    <wiki:CheckRequestContext context='view|diff|edit|upload|info'>
      <div class="pageInfo">
        <wiki:CheckVersion mode="latest">
           <fmt:message key="actions.lastchange">
              <fmt:param><wiki:PageVersion /></fmt:param>
              <fmt:param><wiki:DiffLink version="latest" newVersion="previous"><wiki:PageDate format='<%= prefDateFormat %>'/></wiki:DiffLink></fmt:param>
              <fmt:param><wiki:Author /></fmt:param>
           </fmt:message>
        </wiki:CheckVersion>
  
        <wiki:CheckVersion mode="notlatest">
          <fmt:message key="actions.publishedon">
             <fmt:param><wiki:PageDate format='<%= prefDateFormat %>'/></fmt:param>
             <fmt:param><wiki:Author /></fmt:param>
          </fmt:message>
        </wiki:CheckVersion>
  
        <wiki:NoSuchPage><fmt:message key="actions.notcreated"/></wiki:NoSuchPage>
  
      </div>
    </wiki:CheckRequestContext>
  
    <span class='quick2Bottom'><a href="#Bottom" title='<fmt:message key="actions.gotobottom"/>' >&nbsp;</a></span>

</div>
