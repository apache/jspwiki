<%@ page language="java" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki"%>
<%@ page import="java.util.Properties"%>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.render.*" %>
<%@ page import="com.ecyrd.jspwiki.parser.JSPWikiMarkupParser" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.DefaultResources"/>

<%--
    This provides the FCK editor for JSPWiki.
--%>
<%  WikiContext context = WikiContext.findContext( pageContext );
    context.setVariable( RenderingManager.WYSIWYG_EDITOR_MODE, Boolean.TRUE );
    context.setVariable( WikiEngine.PROP_RUNFILTERS,  "false" );

    WikiPage wikiPage = context.getPage();
    String originalCCLOption = (String)wikiPage.getAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS );
    wikiPage.setAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS, "false" );
    
    String usertext = EditorManager.getEditedText(pageContext);
    TemplateManager.addResourceRequest( context, "script", "scripts/fckeditor/fckeditor.js" );
    String changenote = (String)session.getAttribute("changenote");
    changenote = changenote != null ? TextUtil.replaceEntities(changenote) : "";    
 %>   
<wiki:CheckRequestContext context="edit"><%
    if( usertext == null )
    {
        usertext = context.getEngine().getPureText( context.getPage() );
    }%>
</wiki:CheckRequestContext>
<% if( usertext == null ) usertext = "";

   WikiEngine engine = context.getEngine();
   RenderingManager renderingManager = new RenderingManager();
   
   // since the WikiProperties are shared, we'll want to make our own copy of it for modifying.
   Properties copyOfWikiProperties = new Properties();
   copyOfWikiProperties.putAll( engine.getWikiProperties() );
   copyOfWikiProperties.setProperty( "jspwiki.renderingManager.renderer", WysiwygEditingRenderer.class.getName() );
   renderingManager.initialize( engine, copyOfWikiProperties );
	
   String pageAsHtml = StringEscapeUtils.escapeJavaScript( renderingManager.getHTML( context, usertext ) );
   
   // Disable the WYSIWYG_EDITOR_MODE and reset the other properties immediately
   // after the XHTML for FCK has been rendered.
   context.setVariable( RenderingManager.WYSIWYG_EDITOR_MODE, Boolean.FALSE );
   context.setVariable( WikiEngine.PROP_RUNFILTERS,  null );
   wikiPage.setAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS, originalCCLOption );
   
   String templateDir = (String)copyOfWikiProperties.get( WikiEngine.PROP_TEMPLATEDIR );
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
<div style="width:100%"> <%-- Required for IE6 on Windows --%>
<script type="text/javascript">
   var oFCKeditor = new FCKeditor( 'htmlPageText' );
   oFCKeditor.BasePath = 'scripts/fckeditor/';
   oFCKeditor.Value = '<%=pageAsHtml%>';
   oFCKeditor.Width  = '100%';
   oFCKeditor.Height = '450';
   oFCKeditor.Config['CustomConfigurationsPath'] = '<%=request.getContextPath()%>/scripts/fckconfig.js';
   oFCKeditor.Config['StylesXmlPath'] = '<%=request.getContextPath()%>/scripts/fckstyles.xml';
   oFCKeditor.Config['TemplatesXmlPath'] = '<%=request.getContextPath()%>/scripts/fcktemplates.xml';
   oFCKeditor.Config['BaseHref'] = 'http://<%=request.getServerName()%>:<%=request.getServerPort()%><%=request.getContextPath()%>/';
   oFCKeditor.Config['EditorAreaCSS'] = '<%=request.getContextPath()%>/templates/<%=templateDir%>/jspwiki.css';
   oFCKeditor.Config['SmileyPath'] = oFCKeditor.Config['BaseHref'] + 'scripts/fckeditor/editor/images/smiley/msn/' ;
   oFCKeditor.Create();
</script>
<noscript>
  <br>
  <div class="error">You need to enable Javascript in your browser to use the WYSIWYG editor</div>
</noscript>

   <wiki:CheckRequestContext context="edit">
   	<p>
       <label for="changenote">Change note</label>
       <input type="text" id="changenote" name="changenote" size="80" maxlength="80" value="<%=changenote%>"/>
    </p>
   </wiki:CheckRequestContext>
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
        <input name='ok' type='submit' value='<fmt:message key="editor.plain.save.submit"/>' />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input name='preview' type='submit' value='<fmt:message key="editor.plain.preview.submit"/>' />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input name='cancel' type='submit' value='<fmt:message key="editor.plain.cancel.submit"/>' />
    </p>
</div>
</form>
