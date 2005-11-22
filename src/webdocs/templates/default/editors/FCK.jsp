<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki"%>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.editor.EditorManager" %>
<%@ page import="com.ecyrd.jspwiki.tags.*" %>
<%@ page import="org.apache.commons.lang.*" %>

<%--
    This provides the FCK editor for JSPWiki.
--%>
<%  WikiContext context = (WikiContext)pageContext.getAttribute( WikiTagBase.ATTR_CONTEXT, PageContext.REQUEST_SCOPE );
    String usertext = EditorManager.getEditedText(pageContext);%>

<wiki:CheckRequestContext context="edit"><%
    if( usertext == null )
    {
        usertext = context.getEngine().getText( context, context.getPage() );
    }%>
</wiki:CheckRequestContext>
<% if( usertext == null ) usertext = "";
   String pageAsHtml = StringEscapeUtils.escapeJavaScript( context.getEngine().textToHTML( context, usertext ) );
%>

<div>
<script type="text/javascript">
   var oFCKeditor = new FCKeditor( 'htmlPageText' );
   oFCKeditor.BasePath = 'scripts/fckeditor';
   oFCKeditor.Value = '<%=pageAsHtml%>';
   oFCKeditor.Width  = '100%';
   oFCKeditor.Height = '500';
   oFCKeditor.ToolbarSet = 'JSPWiki';
   oFCKeditor.Config['CustomConfigurationsPath'] = '<wiki:BaseURL/>/scripts/fckconfig.js';
   oFCKeditor.Create();
</script>
<noscript>
  <br>
  <h3 class="previewnote">You need to enable Javascript in your browser to use the WYSIWYG editor</h3>
</noscript>