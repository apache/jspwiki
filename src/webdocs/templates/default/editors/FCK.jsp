<%@ page language="java" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki"%>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="org.apache.commons.lang.*" %>

<%--
    This provides the FCK editor for JSPWiki.
--%>
<%  WikiContext context = WikiContext.findContext( pageContext );
    String usertext = EditorManager.getEditedText(pageContext);
    TemplateManager.addResourceRequest( context, "script", "scripts/fckeditor/fckeditor.js" );
 %>   
<wiki:CheckRequestContext context="edit"><%
    if( usertext == null )
    {
        usertext = context.getEngine().getText( context, context.getPage() );
    }%>
</wiki:CheckRequestContext>
<% if( usertext == null ) usertext = "";
   String pageAsHtml = StringEscapeUtils.escapeJavaScript( context.getEngine().textToHTML( context, usertext ) );
%>

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
<div>
<script type="text/javascript">
   var oFCKeditor = new FCKeditor( 'htmlPageText' );
   oFCKeditor.BasePath = 'scripts/fckeditor/';
   oFCKeditor.Value = '<%=pageAsHtml%>';
   oFCKeditor.Width  = '100%';
   oFCKeditor.Height = '500';
   oFCKeditor.ToolbarSet = 'JSPWiki';
   oFCKeditor.Config['CustomConfigurationsPath'] = '<wiki:Link format="url" jsp="scripts/fckconfig.js"/>';
   oFCKeditor.Create();
</script>
<noscript>
  <br>
  <div class="error">You need to enable Javascript in your browser to use the WYSIWYG editor</div>
</noscript>

   <wiki:CheckRequestContext context="comment">

        <table border="0" class="small">
          <tr>
            <td><label for="authorname" accesskey="n">Your <u>n</u>ame</label></td>
            <td><input type="text" name="author" id="authorname" value="<wiki:UserName/>" /></td>
            <td><label for="rememberme">Remember me?</label>
            <input type="checkbox" name="remember" id="rememberme" /></td>
          </tr>
          <tr>
            <td><label for="link" accesskey="m">Homepage or e<u>m</u>ail</label></td>
            <td colspan="2"><input type="text" name="link" id="link" size="40" value="<%=pageContext.getAttribute("link",PageContext.REQUEST_SCOPE)%>" /></td>
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
</div>
</form>
