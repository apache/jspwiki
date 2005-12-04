<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

<%
  /* see commonheader.jsp */
  String prefDateFormat     = (String) session.getAttribute("prefDateFormat");
  String prefTimeZone       = (String) session.getAttribute("prefTimeZone");
%>

<div class="pageactions">

  <div class="block">
  
    <span class='quick2Top'><a href="#Top" title='Go to Top' >9</a></span>
  
    <wiki:CheckRequestContext context='info|diff|upload|edit'>
      <span class="actionView">
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
        <span class="actionEdit">
          <wiki:PageType type="page">
            <wiki:EditLink>Edit page</wiki:EditLink>
          </wiki:PageType>
          <wiki:PageType type="attachment">
            <a href="<wiki:BaseURL/>Edit.jsp?page=<wiki:ParentPageName />"
               title="Edit parent page" >Edit page</a>
          </wiki:PageType>
        </span>
  
        <span class="actionComment">
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
  
      <span class="actionAttach">
        <wiki:PageType type="page">
        <wiki:Permission permission="upload">
           <a href="<wiki:UploadLink format='url' />">Attach File</a>
        </wiki:Permission>
        </wiki:PageType>
      </span>
    </wiki:CheckRequestContext>
  
    <wiki:CheckRequestContext context='view|diff|edit|upload'>
      <span class="actionInfo">
        <wiki:PageInfoLink>Page Info</wiki:PageInfoLink>
      </span>
    </wiki:CheckRequestContext>
  
    </wiki:PageExists>
  
    <wiki:CheckRequestContext context='!prefs'>
       <wiki:CheckRequestContext context='!preview'>
        <span class="actionPrefs">
          <wiki:LinkTo page="UserPreferences">My Prefs</wiki:LinkTo>
        </span>
      </wiki:CheckRequestContext>
    </wiki:CheckRequestContext>
    
    <wiki:UserCheck status="notAuthenticated">
      <wiki:Link jsp="Login.jsp">Log in</wiki:Link>
    </wiki:UserCheck>
    
    <wiki:UserCheck status="authenticated">
      <wiki:Link jsp="Logout.jsp">Log out</wiki:Link>
    </wiki:UserCheck>
    
    <wiki:Permission permission="createGroups">
      <wiki:Link jsp="NewGroup.jsp">Create group</wiki:Link>
    </wiki:Permission>
  
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
  
    <span class='quick2Bottom'><a href="#Bottom" title='Go to Bottom' >:</a></span>

  </div>

</div>
