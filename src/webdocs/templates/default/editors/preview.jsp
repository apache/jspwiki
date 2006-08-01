<%@ page language="java" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki"%>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>

<%--
        This is a special editor component for JSPWiki preview storage.
--%>
<% WikiContext context = WikiContext.findContext( pageContext ); %>
<% String usertext = (String)pageContext.getAttribute( EditorManager.ATTR_EDITEDTEXT, PageContext.REQUEST_SCOPE ); %>
<% if( usertext == null ) usertext = ""; %>
<% 
   String action = "comment".equals(request.getParameter("action")) ? 
                   context.getURL(WikiContext.COMMENT,context.getName()) : 
                   context.getURL(WikiContext.EDIT,context.getName());
 %>
<form accept-charset="<wiki:ContentEncoding/>" method="post" 
      action="<%=action%>" 
      name="editForm" enctype="application/x-www-form-urlencoded">
    <p>
        <%-- Edit.jsp & Comment.jsp rely on these being found.  So be careful, if you make changes. --%>
        <input name="author" type="hidden" value="<%=session.getAttribute("author")%>" />
        <input name="link" type="hidden" value="<%=session.getAttribute("link")%>" />
        <input name="remember" type="hidden" value="<%=session.getAttribute("remember")%>" />

        <input name="page" type="hidden" value="<wiki:Variable var="pagename"/>" />
        <input name="action" type="hidden" value="save" />
        <input name="edittime" type="hidden" value="<%=pageContext.getAttribute("lastchange",
                                                                                PageContext.REQUEST_SCOPE )%>" />
    </p>
    <textarea style="display:none;" readonly="true"
              id="editorarea" name="<%=EditorManager.REQ_EDITEDTEXT%>" rows="4" cols="80"><%=TextUtil.replaceEntities(usertext)%></textarea>

    <div id="previewsavebutton" align="center">
        <input type="submit" name="edit" value="Keep editing" />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input type="submit" name="ok" value="Save" />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input type="submit" name="cancel" value="Cancel" />
     </div>

</form>
