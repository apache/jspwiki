<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki"%>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="com.ecyrd.jspwiki.rpc.*" %>
<%@ page import="com.ecyrd.jspwiki.rpc.json.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setBundle basename="templates.default"/>
<%--
        This is a plain editor for JSPWiki.
--%>
<% 
   WikiContext context = WikiContext.findContext( pageContext ); 
   WikiEngine engine = context.getEngine();
   JSONRPCManager.requestJSON( context );  //FIXME: to be replace by standard mootools lib

   String usertext = EditorManager.getEditedText( pageContext );
%>
<wiki:CheckRequestContext context="edit">
<wiki:NoSuchPage> <%-- this is a new page, check if we're cloning --%>
<%
  String clone = request.getParameter( "clone" ); 
  if( clone != null )
  {
    WikiPage p = engine.getPage( clone );
    if( p != null )
    {
      usertext = engine.getPureText( p );
    }
  }
%>
</wiki:NoSuchPage>
<%
  if( usertext == null )
  {
    usertext = engine.getPureText( context.getPage() );
  }
%>
</wiki:CheckRequestContext>
<% if( usertext == null ) usertext = "";  %>


<div style="width:100%"> <%-- Required for IE6 on Windows --%>

<%-- FIXME: better have it created by some js :: move it to jspwiki-edit.js --%>
<div id="findSuggestionMenu" style='visibility:hidden;'></div>

<form action="<wiki:CheckRequestContext 
     context='edit'><wiki:EditLink format='url'/></wiki:CheckRequestContext><wiki:CheckRequestContext 
     context='comment'><wiki:CommentLink format='url'/></wiki:CheckRequestContext>" 
       class="wikiform"
        name="editform" id="editform" 
    onsubmit="return Wiki.submitOnce( this );"
      method="post" accept-charset="<wiki:ContentEncoding/>"
     enctype="application/x-www-form-urlencoded" >

  <%-- Edit.jsp relies on these being found.  So be careful, if you make changes. --%>
  <input name="page" type="hidden" value="<wiki:Variable var='pagename' />" />
  <input name="action" type="hidden" value="save" />
  <input name="edittime" type="hidden" value="<c:out value='${lastchange}' />" />
  <input name="addr" type="hidden" value="<%=request.getRemoteAddr()%>" />

  <p>
  <input type="submit" name="ok" value="Save" style="display:none;"/>
  <input type="button" name="ox" value="<fmt:message key='editor.plain.save.submit'/>" 
      onclick="this.form.ok.click();" 
    accesskey="s"
        title="<fmt:message key='editor.plain.save.title'/>" />
  <input type="submit" name="preview" value="Preview" style="display:none;"/>
  <input type="button" name="previex" value="<fmt:message key='editor.plain.preview.submit'/>" 
      onclick="this.form.preview.click();" 
    accesskey="v"
        title="<fmt:message key='editor.plain.preview.title'/>" />
  <input type="submit" name="cancel" value="Cancel" style="display:none;"/>
  <input type="button" name="cancex" value="<fmt:message key='editor.plain.cancel.submit'/>" 
      onclick="this.form.cancel.click();" 
    accesskey="q" 
        title="<fmt:message key='editor.plain.cancel.title'/>" />
  </p>
  
  <wiki:CheckRequestContext context="edit">
  <textarea id="editorarea" name="<%=EditorManager.REQ_EDITEDTEXT%>" 
         class="editor" 
       onkeyup="getSuggestions(this.id)"
       onclick="setCursorPos(this.id)" 
      onchange="setCursorPos(this.id)"
          rows="<c:out value='${prefEditAreaHeight}' />"
          cols="80"><%=TextUtil.replaceEntities(usertext)%></textarea>

    <p>
    <label for="changenote"><fmt:message key='editor.plain.changenote'/></label>
    <input type="text" id="changenote" name="changenote" size="80" maxlength="80" value="<c:out value='${changenote}'/>"/>
    </p>
  </wiki:CheckRequestContext>

  <wiki:CheckRequestContext context="comment">
  <textarea id="editorarea" name="<%=EditorManager.REQ_EDITEDTEXT%>" 
         class="editor" 
       onkeyup="getSuggestions(this.id)"
       onclick="setCursorPos(this.id)" 
      onchange="setCursorPos(this.id)"
          rows="10" cols="80"><%=TextUtil.replaceEntities(usertext)%></textarea>

    <fieldset>
	<legend><fmt:message key="editor.commentsignature"/></legend>
    <p>
    <label for="authorname" accesskey="n"><fmt:message key="editor.plain.name"/></label></td>
    <input type="text" name="author" id="authorname" value="<c:out value='${sessionScope.author}' />" />
    <input type="checkbox" name="remember" id="rememberme" <%=TextUtil.isPositive((String)session.getAttribute("remember")) ? "checked='checked'" : ""%>"/>
    <label for="rememberme"><fmt:message key="editor.plain.remember"/></label>
    </p>
	<%--FIXME: seems not to read the email of the user, but some odd previously cached value --%>
    <p>
    <label for="link" accesskey="m"><fmt:message key="editor.plain.email"/></label>
    <input type="text" name="link" id="link" size="24" value="<c:out value='${sessionScope.link}' />" />
    </p>
    </fieldset>
  </wiki:CheckRequestContext>

</form>
</div>

<form class='wikiform'>
<p>
<%-- Search and replace section --%>
  <label for="tbFIND"><fmt:message key="editor.plain.find"/></label>
  <input type="text" name="tbFIND" id="tbFIND" size="16" />
  <label for="tbREPLACE"><fmt:message key="editor.plain.replace"/></label>
  <input type="text" name="tbREPLACE" id="tbREPLACE" size="16" />
  <input type="checkbox" name="tbMatchCASE" id="tbMatchCASE" />
  <label for="tbMatchCASE"><fmt:message key="editor.plain.matchcase"/></label>
  <input type="checkbox" name="tbREGEXP" id="tbREGEXP" />
  <label for="tbREGEXP"><fmt:message key="editor.plain.regexp"/></label>
  <input type="checkbox" name="tbGLOBAL" id="tbGLOBAL" checked />
  <label for="tbGLOBAL"><fmt:message key="editor.plain.global"/></label>
  <input type="button" name="replace" id="replace" value="<fmt:message key='editor.plain.find.submit' />" 
  onmousedown="EditTools.doReplace()" />
  <input type="button" name="tbREDO" id="tbREDO" value="<fmt:message key='editor.plain.redo.submit' />" 
  onmousedown="EditTools.redoTextarea()" title="Redo" disabled />
  <input type="button" name="tbUNDO" id="tbUNDO" value="<fmt:message key='editor.plain.undo.submit' />" 
  onmousedown="EditTools.undoTextarea()" title="<fmt:message key='editor.plain.undo.title' />" disabled accesskey="z"/>
</p>
</form>
