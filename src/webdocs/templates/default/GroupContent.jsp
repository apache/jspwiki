<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page errorPage="/Error.jsp" %>
<%! 
    Category log = Category.getInstance("JSPWiki"); 
%>

<h3>New Wiki Group</h3>

<div class="formcontainer">
  <div class="instructions">
    Create a new wiki group.
  </div>
  <div class="instructions">
    <wiki:Messages div="error" prefix="Could not create group: " />
  </div>
  <form id="newGroup" action="<wiki:Variable var="baseURL"/>NewGroup.jsp" 
    method="POST" accept-charset="UTF-8">
      
    <!-- Group name -->
    <div class="block">
      <label>Group name</label>
      <input type="text" name="name" size="30" 
        value="<%=pageContext.getAttribute("name",PageContext.REQUEST_SCOPE)%>"/>
      <div class="description">
        The name of the group.
      </div>
    </div>

    <!-- Members -->
    <div class="block">
      <label>Members</label>
      <input type="text" name="members" size="30" 
        value="<%=pageContext.getAttribute("members",PageContext.REQUEST_SCOPE)%>"/>
      <div class="description">
        The membership for this group. Enter each user's wiki name
        or full name, separated by commas.
      </div>
    </div>

    <div class="block">
      <!-- Any errors? -->
      <div class="instructions">
        When you click 'save', this group will be saved as a wiki page
        called <b>Group<i>Name</i></b>. <em>E.g.,</em> if you type in
        "Admin", the group will be called "GroupAdmin".
      </div>
      <input type="submit" name="ok" value="Save" />
      <input type="hidden" name="action" value="save" />
    </div>
  </form>
</div>
