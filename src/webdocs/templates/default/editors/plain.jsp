<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki"%>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="com.ecyrd.jspwiki.rpc.*" %>
<%@ page import="com.ecyrd.jspwiki.rpc.json.*" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.DefaultResources"/>
<%--
        This is a plain editor for JSPWiki.
--%>
<style>
#findSuggestionMenu { position:absolute;
                      top: 0px;
                      left: 0px; 
                      border: 2px inset black;
                      background-color: #f0f0f0;
                      z-index: 1;}
</style>
<script>
function getSuggestions(id)
{
  var textNode = document.getElementById(id);
  var val = textNode.value;
  var searchword;
  
  var pos = getCursorPos(textNode);
  for( i = pos-1; i > 0; i-- )
  {
    if( val.charAt(i) == ']' ) break;
    if( val.charAt(i) == '[' && i < val.length-1 ) { searchword = val.substring(i+1,pos); break; }
  }

  if( searchword )
  {
    jsonrpc.search.getSuggestions( callback, searchword, 10 );
  }
  else
  {
    var menuNode = document.getElementById("findSuggestionMenu");
    menuNode.style.visibility = "hidden";
  }
}
function callback(result,exception)
{   
   if(exception) { alert(exception.message); return; }
   
   var menuNode = document.getElementById("findSuggestionMenu");
   
   var html = "<ul>";
   for( i = 0; i < result.list.length; i++ )
   {
      html += "<li>"+result.list[i]+"</li>";
   }
   html += "</ul>";
   menuNode.innerHTML = html;
   menuNode.style.visibility = "visible";
}

</script>
<% WikiContext context = WikiContext.findContext( pageContext ); %>
<% String usertext = EditorManager.getEditedText( pageContext );
   JSONRPCManager.requestJSON( WikiContext.findContext(pageContext) );

   TemplateManager.addResourceRequest( context, "script", 
                                       context.getURL(WikiContext.NONE,"scripts/searchreplace.js") );
   String changenote = (String)session.getAttribute("changenote");
   changenote = changenote != null ? TextUtil.replaceEntities(changenote) : ""; %>
<wiki:CheckRequestContext context="edit"><%
    if( usertext == null )
    {
        usertext = context.getEngine().getPureText( context.getPage() );
    }%>
</wiki:CheckRequestContext>
<% if( usertext == null ) usertext = ""; %>

<div style="width:100%"> <%-- Required for IE6 on Windows --%>
      <div id="findSuggestionMenu" style='visibility:hidden;'></div>
<form accept-charset="<wiki:ContentEncoding/>" method="post" 
      action="<wiki:CheckRequestContext context="edit"><wiki:EditLink format="url"/></wiki:CheckRequestContext><wiki:CheckRequestContext context="comment"><wiki:CommentLink format="url"/></wiki:CheckRequestContext>" 
      name="editForm" enctype="application/x-www-form-urlencoded">
    <p>
        <%-- Edit.jsp relies on these being found.  So be careful, if you make changes. --%>
        <input name="page" type="hidden" value="<wiki:Variable var="pagename"/>" />
        <input name="action" type="hidden" value="save" />
        <input name="edittime" type="hidden" value="<%=pageContext.getAttribute("lastchange",
                                                                       PageContext.REQUEST_SCOPE )%>" />
        <input name="addr" type="hidden" value="<%=request.getRemoteAddr()%>" />
    </p>
    <textarea style="width:100%;" class="editor" onkeyup="getSuggestions(this.id)"
              onclick="setCursorPos(this.id)" onchange="setCursorPos(this.id)"
              id="editorarea" name="<%=EditorManager.REQ_EDITEDTEXT%>" rows="25" cols="80"><%=TextUtil.replaceEntities(usertext)%></textarea>

   <wiki:CheckRequestContext context="edit">
       <label for="changenote">Change note</label>
       <input type="text" id="changenote" name="changenote" size="40" maxlength="80" value="<%=changenote%>"/>
   </wiki:CheckRequestContext>
   <wiki:CheckRequestContext context="comment">
        <table border="0" class="small">
          <tr>
            <td><label for="authorname" accesskey="n"><fmt:message key="editor.plain.name"/></label></td>
            <td><input type="text" name="author" id="authorname" value="<%=session.getAttribute("author")%>" /></td>
            <td><label for="rememberme"><fmt:message key="editor.plain.remember"/></label>
            <input type="checkbox" name="remember" id="rememberme" <%=TextUtil.isPositive((String)session.getAttribute("remember")) ? "checked='checked'" : ""%>"/></td>
          </tr>
          <tr>
            <td><label for="link" accesskey="m"><fmt:message key="editor.plain.email"/></label></td>
            <td colspan="2"><input type="text" name="link" id="link" size="40" value="<%=session.getAttribute("link")%>" /></td>
          </tr>
        </table>
    </wiki:CheckRequestContext>

    <p>
        <input name='ok' type='submit' value='<fmt:message key="editor.plain.save.submit"/>' />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input name='preview' type='submit' value='<fmt:message key="editor.plain.preview.submit"/>' />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input name='cancel' type='submit' value='<fmt:message key="editor.plain.cancel.submit"/>' />
    </p>
</form>
</div>

    <%-- Search and replace section --%>
    <form name="searchbar" id="searchbar" action="#">
      <label for="findText"><fmt:message key="editor.plain.find"/></label>
      <input type="text" id="findText" size="16"/>
      <label for="replaceText"><fmt:message key="editor.plain.replace"/></label>
      <input type="text" id="replaceText" size="16"/>

      <input type="checkbox" id="matchCase" /><label for="matchCase"><fmt:message key="editor.plain.matchcase"/></label>
      <input type="checkbox" id="regExp" /><label for="regExp"><fmt:message key="editor.plain.regexp"/></label>
      <input type="checkbox" id="global" checked="checked"/><label for="global"><fmt:message key="editor.plain.global"/></label>
      &nbsp;
      <input type="button" id="replace" value="<fmt:message key="editor.plain.find.submit"/>" onclick="SearchReplace.editReplace(this.form, document.getElementById('editorarea') );" />

      <span id="undoHideOrShow" style="visibility:hidden;" >
        <input type="button" id="undo" value="<fmt:message key="editor.plain.undo.submit"/>" onclick="SearchReplace.editUndo(this.form, document.getElementById('editorarea') );" />
      </span>
      <input type="hidden" id="undoMemory" value="" />
    </form>
    
