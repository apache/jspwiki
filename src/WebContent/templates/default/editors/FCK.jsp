<%-- 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
--%>
<%@ page language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ page import="java.util.Properties" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.permissions.*" %>
<%@ page import="org.apache.wiki.render.*" %>
<%@ page import="org.apache.wiki.parser.JSPWikiMarkupParser" %>
<%@ page import="org.apache.wiki.ui.*" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="org.apache.wiki.filters.*" %>
<%@ page import="org.apache.wiki.api.WikiPage" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<%--
    This provides the FCK editor for JSPWiki.
--%>
<%  WikiContext context = WikiContextFactory.findContext( pageContext );
    WikiEngine engine = context.getEngine();
    context.setVariable( RenderingManager.WYSIWYG_EDITOR_MODE, Boolean.TRUE );
    context.setVariable( WikiEngine.PROP_RUNFILTERS,  "false" );

    WikiPage wikiPage = context.getPage();
    String originalCCLOption = (String)wikiPage.getAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS );
    wikiPage.setAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS, "false" );
    
    String usertext = EditorManager.getEditedText(pageContext);
    TemplateManager.addResourceRequest( context, "script", "scripts/fckeditor/fckeditor.js" );
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
<div style="width:100%"> <%-- Required for IE6 on Windows --%>

  <%-- Print any messages or validation errors --%>
  <s:messages />
  <s:errors />

  <s:form beanclass="org.apache.wiki.action.EditActionBean"
              class="wikiform"
                 id="editform"
             method="post"
      acceptcharset="UTF-8"
            enctype="application/x-www-form-urlencoded">

    <%-- If any conflicts, print the conflicting text here --%>
    <c:if test="${not empty wikiActionBean.conflictText}">
      <p>
        <s:label for="conflictText" />
        <s:textarea name="conflictText" readonly="true" />
      </p>
    </c:if>
    
    <%-- EditActionBean relies on these being found.  So be careful, if you make changes. --%>
    <p id="submitbuttons">
      <s:hidden name="page" />
      <s:hidden name="startTime" />
    </p>
    
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
      <s:label for="changeNote" />
      <s:text name="changeNote" size="50" maxlength="80" />
    </p>
    
    <wiki:CheckRequestContext context="comment">
      <fieldset>
        <legend><fmt:message key="editor.commentsignature" /></legend>
        <p><s:label for="author" accesskey="n" />&nbsp;<s:text name="author" /></p>
        <p><s:label for="email" accesskey="m" />&nbsp;<s:text name="email" size="24" /></p>
      </fieldset>
    </wiki:CheckRequestContext>
    
    <p>
      <c:set var="saveTitle" scope="page"><fmt:message key="editor.plain.save.title" /></c:set>
      <wiki:CheckRequestContext context='edit'>
        <s:submit name="save" accesskey="s" title="${saveTitle}" />
      </wiki:CheckRequestContext>
      <wiki:CheckRequestContext context='comment'>
        <s:submit name="comment" accesskey="s" title="${saveTitle}" />
      </wiki:CheckRequestContext>
      
      <c:set var="previewTitle" scope="page"><fmt:message key="editor.plain.preview.title" /></c:set>
      <s:submit name="preview" accesskey="v" title="${previewTitle}" />
      
      <c:set var="cancelTitle" scope="page"><fmt:message key="editor.plain.cancel.title" /></c:set>
      <s:submit name="cancel" accesskey="q" title="${cancelTitle}" />
    </p>

    <%-- Spam detection fields --%>
    <wiki:SpamProtect />
  </s:form>

</div>
