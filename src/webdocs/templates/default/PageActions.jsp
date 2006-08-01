<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%
  /* see commonheader.jsp */
  String prefDateFormat     = (String) session.getAttribute("prefDateFormat");
  String prefTimeZone       = (String) session.getAttribute("prefTimeZone");
  String group              = request.getParameter( "group" );
%>

<div class="pageactions">
  
    <span class='quick2Top'><a href="#Top" title='Go to Top' >&nbsp;</a></span>

    <wiki:CheckRequestContext context='info|diff|upload|edit'>
      <span>
        <wiki:PageType type="page">
          <wiki:LinkTo>View page</wiki:LinkTo>
        </wiki:PageType>
        <wiki:PageType type="attachment">
          <wiki:LinkToParent>View page</wiki:LinkToParent>
        </wiki:PageType>
      </span>
    </wiki:CheckRequestContext>
  
    <wiki:CheckRequestContext context='view|info|diff|upload'>
      <wiki:Permission permission="edit">
        <span>
          <wiki:PageType type="page">
            <wiki:EditLink>Edit page</wiki:EditLink>
          </wiki:PageType>
        </span>
      </wiki:Permission>
  
      <wiki:Permission permission="comment">
        <span>
          <wiki:PageType type="page">
            <wiki:CommentLink>Add Comment</wiki:CommentLink>
          </wiki:PageType>
          <wiki:PageType type="attachment">
            <a href="Comment.jsp?page=<wiki:ParentPageName />"
               title="Add Comment to parent page" >Add Comment</a>
          </wiki:PageType>
        </span>
      </wiki:Permission>
    </wiki:CheckRequestContext>
  
    <wiki:PageExists>
      <wiki:CheckRequestContext context='view|info|diff|edit'>
        <wiki:PageType type="page">
          <wiki:Permission permission="upload">
            <span>
              <a href="<wiki:UploadLink format='url' />">Attach File</a>
            </span>
          </wiki:Permission>
        </wiki:PageType>
      </wiki:CheckRequestContext>
      <span>
        <wiki:CheckRequestContext context='view|diff|edit|upload'>
          <wiki:PageInfoLink>Page Info</wiki:PageInfoLink>
        </wiki:CheckRequestContext>
      </span>
    </wiki:PageExists>
  
    <wiki:CheckRequestContext context='!prefs'>
      <wiki:CheckRequestContext context='!preview'>
        <span>
          <wiki:LinkTo page="UserPreferences">My Prefs</wiki:LinkTo>
        </span>
      </wiki:CheckRequestContext>
    </wiki:CheckRequestContext>
    
    <wiki:UserCheck status="notAuthenticated">
      <wiki:Permission permission="login">
        <span>
          <wiki:Link jsp="Login.jsp">Log in</wiki:Link>
        </span>
      </wiki:Permission>
    </wiki:UserCheck>
    
    <wiki:CheckRequestContext context='viewGroup'>
      <wiki:Permission permission="editGroup">
        <span>
          <wiki:Link jsp="EditGroup.jsp">
            <wiki:Param name="group" value="<%=group%>" />
            Edit group
          </wiki:Link>
        </span>
      </wiki:Permission>
      <wiki:Permission permission="deleteGroup">
        <span>
          <a onclick="return confirmDelete()" href="<wiki:Link jsp='DeleteGroup.jsp' format='url'><wiki:Param name='group' value='<%=group%>' /></wiki:Link>">Delete group</a>
        </span>
      </wiki:Permission>
    </wiki:CheckRequestContext>
    
    <wiki:CheckRequestContext context='editGroup'>
      <span>
        <wiki:Link jsp="Group.jsp">
          <wiki:Param name="group" value="<%=group%>" />
          View group
        </wiki:Link>
      </span>
    </wiki:CheckRequestContext>
    
    <wiki:Permission permission="createGroups">
      <span>
        <wiki:Link jsp="NewGroup.jsp">Create group</wiki:Link>
      </span>
    </wiki:Permission>

    <wiki:UserCheck status="authenticated">
      <span>
        <wiki:Link jsp="Logout.jsp">Log out</wiki:Link>
      </span>
    </wiki:UserCheck>
  
    <wiki:CheckRequestContext context='view|diff|edit|upload|info'>
      <div class="pageInfo">
        <wiki:CheckVersion mode="latest">
           This page (revision-<wiki:PageVersion />) last changed on
           <wiki:DiffLink version="latest" newVersion="previous">
             <wiki:PageDate format='<%= prefDateFormat %>'/>
           </wiki:DiffLink>
           by <wiki:Author />.
        </wiki:CheckVersion>
  
        <wiki:CheckVersion mode="notlatest">
          This particular version was published on
            <wiki:PageDate format='<%= prefDateFormat %>'/> by <wiki:Author />.
        </wiki:CheckVersion>
  
        <wiki:NoSuchPage>Page not created yet.</wiki:NoSuchPage>
  
      </div>
    </wiki:CheckRequestContext>
  
    <span class='quick2Bottom'><a href="#Bottom" title='Go to Bottom' >&nbsp;</a></span>

</div>
