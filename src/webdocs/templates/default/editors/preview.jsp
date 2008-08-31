<%@ page language="java" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki"%>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="com.ecyrd.jspwiki.filters.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%--
        This is a special editor component for JSPWiki preview storage.
--%>
<% 
   WikiContext context = WikiContext.findContext( pageContext ); 
   String usertext = (String)pageContext.getAttribute( EditorManager.ATTR_EDITEDTEXT, PageContext.REQUEST_SCOPE ); 
   if( usertext == null ) usertext = ""; 
 
   String action = "comment".equals(request.getParameter("action")) ? 
                   context.getURL(WikiContext.COMMENT,context.getName()) : 
                   context.getURL(WikiContext.EDIT,context.getName());
 %>
<form action="<%=action%>"
      method="post" accept-charset="<wiki:ContentEncoding/>" 
       class="wikiform"
          id="editform"
    onsubmit="return Wiki.submitOnce( this );"
     enctype="application/x-www-form-urlencoded">

  <p>
    <%-- Edit.jsp & Comment.jsp rely on these being found.  So be careful, if you make changes. --%>
    <input type="hidden" name="author" value="<c:out value='${author}' />" />
    <input type="hidden" name="link" value="<c:out value='${link}' />" />
    <input type="hidden" name="remember" value="<c:out value='${remember}' />" />
    <input type="hidden" name="changenote" value="<c:out value='${changenote}' />" />

    <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
    <input type="hidden" name="action" value="save" />
    <input name="<%=SpamFilter.getHashFieldName(request)%>" type="hidden" value="<c:out value='${lastchange}' />" />
  </p>
  <div>
  <textarea style="display:none;" readonly="readonly"
              id="editorarea" name="<%=EditorManager.REQ_EDITEDTEXT%>" 
            rows="4" 
            cols="80"><%=TextUtil.replaceEntities(usertext)%></textarea>
  </div>
  <div id="submitbuttons">
    <input type="submit" name="edit" value="<fmt:message key='editor.preview.edit.submit'/>" 
      accesskey="e"
          title="<fmt:message key='editor.preview.edit.title'/>" />
    <input type="submit" name="ok" value="<fmt:message key='editor.preview.save.submit'/>" 
      accesskey="s"
          title="<fmt:message key='editor.preview.save.title'/>" />
    <input type="submit" name="cancel" value="<fmt:message key='editor.preview.cancel.submit'/>"  
      accesskey="q" 
          title="<fmt:message key='editor.preview.cancel.title'/>" />
  </div>
  
</form>