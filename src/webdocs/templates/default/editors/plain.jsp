<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki"%>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>

<%--
        This is a plain editor for JSPWiki.
--%>
<% WikiContext context = WikiContext.findContext( pageContext ); %>
<% String usertext = EditorManager.getEditedText( pageContext ); 
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
<form accept-charset="<wiki:ContentEncoding/>" method="post" 
      action="<wiki:CheckRequestContext context="edit"><wiki:EditLink format="url"/></wiki:CheckRequestContext><wiki:CheckRequestContext context="comment"><wiki:CommentLink format="url"/></wiki:CheckRequestContext>" 
      name="editForm" enctype="application/x-www-form-urlencoded">
    <p>
        <%-- Edit.jsp relies on these being found.  So be careful, if you make changes. --%>
        <input name="page" type="hidden" value="<wiki:Variable var="pagename"/>" />
        <input name="action" type="hidden" value="save" />
        <input name="edittime" type="hidden" value="<%=pageContext.getAttribute("lastchange",
                                                                       PageContext.REQUEST_SCOPE )%>" />
    </p>
    <textarea style="width:100%;" class="editor" 
              id="editorarea" name="<%=EditorManager.REQ_EDITEDTEXT%>" rows="25" cols="80"><%=TextUtil.replaceEntities(usertext)%></textarea>

   <wiki:CheckRequestContext context="edit">
       <label for="changenote">Change note</label>
       <input type="text" id="changenote" name="changenote" size="40" maxlength="80" value="<%=changenote%>"/>
   </wiki:CheckRequestContext>
   <wiki:CheckRequestContext context="comment">
        <table border="0" class="small">
          <tr>
            <td><label for="authorname" accesskey="n">Your <u>n</u>ame</label></td>
            <td><input type="text" name="author" id="authorname" value="<%=session.getAttribute("author")%>" /></td>
            <td><label for="rememberme">Remember me?</label>
            <input type="checkbox" name="remember" id="rememberme" <%=TextUtil.isPositive((String)session.getAttribute("remember")) ? "checked='checked'" : ""%>"/></td>
          </tr>
          <tr>
            <td><label for="link" accesskey="m">Homepage or e<u>m</u>ail</label></td>
            <td colspan="2"><input type="text" name="link" id="link" size="40" value="<%=session.getAttribute("link")%>" /></td>
          </tr>
        </table>
    </wiki:CheckRequestContext>

    <p>
        <input name='ok' type='submit' value='Save' />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input name='preview' type='submit' value='Preview' />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input name='cancel' type='submit' value='Cancel' />
    </p>
</form>
</div>

    <%-- Search and replace section --%>
    <form name="searchbar" id="searchbar" action="#">
      <label for="findText">Find:</label>
      <input type="text" id="findText" size="16"/>
      <label for="replaceText">Replace:</label>
      <input type="text" id="replaceText" size="16"/>

      <input type="checkbox" id="matchCase" /><label for="matchCase">Match Case</label>
      <input type="checkbox" id="regExp" /><label for="regExp">RegExp</label>
      <input type="checkbox" id="global" checked="checked"/><label for="global">Replace all</label>
      &nbsp;
      <input type="button" id="replace" value="Replace" onclick="SearchReplace.editReplace(this.form, document.getElementById('editorarea') );" />

      <span id="undoHideOrShow" style="visibility:hidden;" >
        <input type="button" id="undo" value="Undo" onclick="SearchReplace.editUndo(this.form, document.getElementById('editorarea') );" />
      </span>
      <input type="hidden" id="undoMemory" value="" />
    </form>
    
