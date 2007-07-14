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

  TemplateManager.addResourceRequest( context, "script", "scripts/jspwiki-edit.js" );
  TemplateManager.addResourceRequest( context, "script", "scripts/posteditor.js" );
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
          id="editform" 
    onsubmit="return Wiki.submitOnce(this);"
      method="post" accept-charset="<wiki:ContentEncoding/>"
     enctype="application/x-www-form-urlencoded" >

  <%-- Edit.jsp relies on these being found.  So be careful, if you make changes. --%>
  <p>
  <input name="page" type="hidden" value="<wiki:Variable var='pagename' />" />
  <input name="action" type="hidden" value="save" />
  <input name="edittime" type="hidden" value="<c:out value='${lastchange}' />" />
  <input name="addr" type="hidden" value="<%=request.getRemoteAddr()%>" />
  <input type="submit" name="ok" value="<fmt:message key='editor.plain.save.submit'/>" 
    accesskey="s"
        title="<fmt:message key='editor.plain.save.title'/>" />
  <input type="submit" name="preview" value="<fmt:message key='editor.plain.preview.submit'/>" 
    accesskey="v"
        title="<fmt:message key='editor.plain.preview.title'/>" />
  <input type="submit" name="cancel" value="<fmt:message key='editor.plain.cancel.submit'/>" 
    accesskey="q" 
        title="<fmt:message key='editor.plain.cancel.title'/>" />
  </p>
  
  <div>
  <textarea id="editorarea" name="<%=EditorManager.REQ_EDITEDTEXT%>" 
         class="editor" 
       onkeyup="getSuggestions(this.id)"
       onclick="setCursorPos(this.id)" 
      onchange="setCursorPos(this.id)"
          rows="20" cols="80"><%=TextUtil.replaceEntities(usertext)%></textarea>
  </div>
  <div style="display:none;">
    <div id="editassist">
      <a href="#" class="tool closed" rel="" title="Toggle additional Edit tools">Edit assist</a>
    </div>
    <div id="toolbar">
      <span title="shift+enter for next next field"><label>Enter Keyword+Tab: </label></span>
	  <a href="#" class="tool" rel="" id="tbLink" title="link - Insert wiki link">link</a>
	  <a href="#" class="tool" rel="break" id="tbH1" title="h1 - Insert heading1">h1</a>
	  <a href="#" class="tool" rel="break" id="tbH2" title="h2 - Insert heading2">h2</a>
	  <a href="#" class="tool" rel="break" id="tbH3" title="h3 - Insert heading3">h3</a>
      <span>&nbsp;</span>
	  <a href="#" class="tool" rel="break" id="tbHR" title="hr - Insert horizontal ruler">hr</a>
	  <a href="#" class="tool" rel="" id="tbBR" title="br - Insert line break">br</a>
	  <a href="#" class="tool" rel="break" id="tbPRE" title="pre - Insert preformatted block">pre</a>
	  <a href="#" class="tool" rel="break" id="tbDL" title="dl - Insert definition list">dl</a>
      <span>&nbsp;</span>
	  <a href="#" class="tool" rel="" id="tbB" title="bold">bold</a>
	  <a href="#" class="tool" rel="" id="tbI" title="italic">italic</a>
	  <a href="#" class="tool" rel="" id="tbMONO" title="mono - monospace">mono</a>
	  <a href="#" class="tool" rel="" id="tbSUP" title="sup - superscript">sup</a>
	  <a href="#" class="tool" rel="" id="tbSUB" title="sub - subscript">sub</a>
	  <a href="#" class="tool" rel="" id="tbSTRIKE" title="strike - strikethrough">strike</a>
      <span>&nbsp;</span>
	  <a href="#" class="tool" rel="break" id="tbTOC" title="toc - Insert table of contents">toc</a>
	  <a href="#" class="tool" rel="break" id="tbTAB" title="tab - Insert tabbed section">tab</a>
	  <a href="#" class="tool" rel="break" id="tbTABLE" title="table - Insert table">table</a>
	  <a href="#" class="tool" rel="" id="tbIMG" title="img - Insert image">img</a>
	  <a href="#" class="tool" rel="break" id="tbCODE" title="code - Insert code block">code</a>
	  <a href="#" class="tool" rel="" id="tbQUOTE" title="quote - Insert quoted block">quote</a>
	  <a href="#" class="tool" rel="break" id="tbSIGN" title="sign - Insert your signature">sign</a>
	  <div style="clear:both;"></div>
    </div>
  </div>
    <p>
    <label for="changenote"><fmt:message key='editor.plain.changenote'/></label>
    <input type="text" name="changenote" id="changenote" size="80" maxlength="80" value="<c:out value='${changenote}'/>"/>
    </p>
  <wiki:CheckRequestContext context="comment">
    <fieldset>
	<legend><fmt:message key="editor.commentsignature"/></legend>
    <p>
    <label for="authorname" accesskey="n"><fmt:message key="editor.plain.name"/></label></td>
    <input type="text" name="author" id="authorname" value="<c:out value='${sessionScope.author}' />" />
    <input type="checkbox" name="remember" id="rememberme" <%=TextUtil.isPositive((String)session.getAttribute("remember")) ? "checked='checked'" : ""%>"/>
    <label for="rememberme"><fmt:message key="editor.plain.remember"/></label>
    </p>
    <p>
    <label for="link" accesskey="m"><fmt:message key="editor.plain.email"/></label>
    <input type="text" name="link" id="link" size="24" value="<c:out value='${sessionScope.link}' />" />
    </p>
    </fieldset>
  </wiki:CheckRequestContext>

</form>
</div>

<form id="searchbar" action="#" class='wikiform'>
<p style="display:none;">
<%-- Search and replace section --%>
  <span style="white-space:nowrap;">
  <label for="tbFIND"><fmt:message key="editor.plain.find"/></label>
  <input type="text" name="tbFIND" id="tbFIND" size="16" />
  </span>
  <span style="white-space:nowrap;">
  <label for="tbREPLACE"><fmt:message key="editor.plain.replace"/></label>
  <input type="text" name="tbREPLACE" id="tbREPLACE" size="16" />
  </span>
  <span style="white-space:nowrap;">
  <input type="checkbox" name="tbMatchCASE" id="tbMatchCASE" />
  <label for="tbMatchCASE"><fmt:message key="editor.plain.matchcase"/></label>
  </span>
  <span style="white-space:nowrap;">
  <input type="checkbox" name="tbREGEXP" id="tbREGEXP" />
  <label for="tbREGEXP"><fmt:message key="editor.plain.regexp"/></label>
  </span>
  <span style="white-space:nowrap;">
  <input type="checkbox" name="tbGLOBAL" id="tbGLOBAL" checked="checked" />
  <label for="tbGLOBAL"><fmt:message key="editor.plain.global"/></label>
  </span>
  <span style="white-space:nowrap;">
  <input type="button" name="replace" id="replace" value="<fmt:message key='editor.plain.find.submit' />" />
  <input type="button" name="tbREDO" id="tbREDO" value="<fmt:message key='editor.plain.redo.submit' />" 
        title="<fmt:message key='editor.plain.redo.title' />" disabled="disabled" />
  <input type="button" name="tbUNDO" id="tbUNDO" value="<fmt:message key='editor.plain.undo.submit' />" 
        title="<fmt:message key='editor.plain.undo.title' />" disabled="disabled" accesskey="z"/>
  </span>
</p>
</form>
