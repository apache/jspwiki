<%@ page language="java" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki"%>
<%@ page import="java.util.Properties"%>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.*" %>
<%@ page import="com.ecyrd.jspwiki.render.*" %>
<%@ page import="com.ecyrd.jspwiki.parser.JSPWikiMarkupParser" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="com.ecyrd.jspwiki.filters.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%--
    This provides the FCK editor for JSPWiki.
--%>
<%  WikiContext context = WikiContext.findContext( pageContext );
    WikiEngine engine = context.getEngine();
    context.setVariable( RenderingManager.WYSIWYG_EDITOR_MODE, Boolean.TRUE );
    context.setVariable( WikiEngine.PROP_RUNFILTERS,  "false" );

    WikiPage wikiPage = context.getPage();
    String originalCCLOption = (String)wikiPage.getAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS );
    wikiPage.setAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS, "false" );
    
    String usertext = EditorManager.getEditedText(pageContext);
    TemplateManager.addResourceRequest( context, TemplateManager.RESOURCE_SCRIPT,"scripts/fckeditor/fckeditor.js" );
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
        AuthorizationManager mgr = engine.getAuthorizationManager();
        PagePermission pp = new PagePermission( p, PagePermission.VIEW_ACTION );

        try
        {            
          if( mgr.checkPermission( context.getWikiSession(), pp ) )
          {
            usertext = engine.getPureText( p );
          }
        }
        catch( Exception e ) {  /*log.error( "Accessing clone page "+clone, e );*/ }
    }
  }
%>
</wiki:NoSuchPage>
<%
    if( usertext == null )
    {
        usertext = engine.getPureText( context.getPage() );
    }%>
</wiki:CheckRequestContext>
<% if( usertext == null ) usertext = "";

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
   
   String protocol = "http://";
   if( request.isSecure() )
   {
       protocol = "https://";
   }   
%>

<form accept-charset="<wiki:ContentEncoding/>" method="post" 
      action="<wiki:CheckRequestContext context='edit'><wiki:EditLink format='url'/></wiki:CheckRequestContext><wiki:CheckRequestContext context='comment'><wiki:CommentLink format='url'/></wiki:CheckRequestContext>" 
      name="editform" id="editform"
      enctype="application/x-www-form-urlencoded">
    <p>
        <%-- Edit.jsp relies on these being found.  So be careful, if you make changes. --%>
        <input name="page" type="hidden" value="<wiki:Variable var="pagename"/>" />
        <input name="action" type="hidden" value="save" />
        <input name="<%=SpamFilter.getHashFieldName(request)%>" type="hidden" value="<c:out value='${lastchange}' />" />
    </p>
<div style="width:100%"> <%-- Required for IE6 on Windows --%>
<script type="text/javascript">
//<![CDATA[

   var oFCKeditor = new FCKeditor( 'htmlPageText' );
   oFCKeditor.BasePath = 'scripts/fckeditor/';
   oFCKeditor.Value = '<%=pageAsHtml%>';
   oFCKeditor.Width  = '100%';
   oFCKeditor.Height = '450';
   oFCKeditor.Config['CustomConfigurationsPath'] = '<%=request.getContextPath()%>/scripts/fckconfig.js';
   oFCKeditor.Config['StylesXmlPath'] = '<%=request.getContextPath()%>/scripts/fckstyles.xml';
   oFCKeditor.Config['TemplatesXmlPath'] = '<%=request.getContextPath()%>/scripts/fcktemplates.xml';
   oFCKeditor.Config['BaseHref'] = '<%=protocol%><%=request.getServerName()%>:<%=request.getServerPort()%><%=request.getContextPath()%>/';
   oFCKeditor.Config['EditorAreaCSS'] = '<%=request.getContextPath()%>/templates/<%=templateDir%>/jspwiki.css';
   oFCKeditor.Config['SmileyPath'] = oFCKeditor.Config['BaseHref'] + 'scripts/fckeditor/editor/images/smiley/msn/' ;
   oFCKeditor.Create();

//]]>
</script>

<noscript>
  <div class="error"><fmt:message key="editor.fck.noscript" /></div>
</noscript>

   <p>
     <label for="changenote"><fmt:message key='editor.plain.changenote'/></label>
     <input type="text" id="changenote" name="changenote" size="80" maxlength="80" value="<c:out value='${changenote}'/>"/>
   </p>
   <wiki:CheckRequestContext context="comment">
    <fieldset>
	<legend><fmt:message key="editor.commentsignature"/></legend>
    <p>
    <label for="authorname" accesskey="n"><fmt:message key="editor.plain.name"/></label>
    <input type="text" name="author" id="authorname" value="<c:out value='${sessionScope.author}' />" />
    <input type="checkbox" name="remember" id="rememberme" <%=TextUtil.isPositive((String)session.getAttribute("remember")) ? "checked='checked'" : ""%> />
    <label for="rememberme"><fmt:message key="editor.plain.remember"/></label>
    </p>
	<%--FIXME: seems not to read the email of the user, but some odd previously cached value --%>
    <p>
    <label for="link" accesskey="m"><fmt:message key="editor.plain.email"/></label>
    <input type="text" name="link" id="link" size="24" value="<c:out value='${sessionScope.link}' />" />
    </p>
    </fieldset>
  </wiki:CheckRequestContext>

  <p>
    <input name='ok' type='submit' value='<fmt:message key="editor.plain.save.submit"/>' />
    <input name='preview' type='submit' value='<fmt:message key="editor.plain.preview.submit"/>' />
    <input name='cancel' type='submit' value='<fmt:message key="editor.plain.cancel.submit"/>' />
  </p>
</div>
</form>
