<%--
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

<%@ page language="java" pageEncoding="UTF-8"%>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ page import="java.util.Properties"%>
<%@ page import="org.apache.commons.text.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.permissions.*" %>
<%@ page import="org.apache.wiki.filters.*" %>
<%@ page import="org.apache.wiki.render.*" %>
<%@ page import="org.apache.wiki.parser.JSPWikiMarkupParser" %>
<%@ page import="org.apache.wiki.ui.*" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="org.apache.wiki.variables.VariableManager" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%--
    This provides the FCK editor for JSPWiki.
--%>
<%  WikiContext context = WikiContext.findContext( pageContext );
    WikiEngine engine = context.getEngine();
    context.setVariable( WikiContext.VAR_WYSIWYG_EDITOR_MODE, Boolean.TRUE );
    context.setVariable( VariableManager.VAR_RUNFILTERS,  "false" );

    WikiPage wikiPage = context.getPage();
    String originalCCLOption = (String)wikiPage.getAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS );
    wikiPage.setAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS, "false" );

    String usertext = EditorManager.getEditedText(pageContext);
    TemplateManager.addResourceRequest( context, TemplateManager.RESOURCE_SCRIPT,
   		context.getURL( WikiContext.NONE, "scripts/fckeditor/fckeditor.js" ) ); %>

<wiki:CheckRequestContext context="edit">
<wiki:NoSuchPage> <%-- this is a new page, check if we're cloning --%>
<%
  String clone = request.getParameter( "clone" );
  if( clone != null )
  {
    WikiPage p = engine.getPageManager().getPage( clone );
    if( p != null )
    {
        AuthorizationManager mgr = engine.getAuthorizationManager();
        PagePermission pp = new PagePermission( p, PagePermission.VIEW_ACTION );

        try
        {
          if( mgr.checkPermission( context.getWikiSession(), pp ) )
          {
            usertext = engine.getPageManager().getPureText( p );
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
        usertext = engine.getPageManager().getPureText( context.getPage() );
    }%>
</wiki:CheckRequestContext>
<% if( usertext == null ) usertext = "";

   String pageAsHtml = StringEscapeUtils.escapeEcmaScript( engine.getRenderingManager().getHTML( context, usertext ) );

   // Disable the WYSIWYG_EDITOR_MODE and reset the other properties immediately
   // after the XHTML for FCK has been rendered.
   context.setVariable( WikiContext.VAR_WYSIWYG_EDITOR_MODE, Boolean.FALSE );
   context.setVariable( VariableManager.VAR_RUNFILTERS,  null );
   wikiPage.setAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS, originalCCLOption );

   String templateDir = (String)engine.getWikiProperties().get( WikiEngine.PROP_TEMPLATEDIR );

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
     <input type="text" id="changenote" name="changenote" size="80" maxlength="80" value="${changenote}"/>
   </p>
   <wiki:CheckRequestContext context="comment">
    <fieldset>
	<legend><fmt:message key="editor.commentsignature"/></legend>
    <p>
    <label for="authorname" accesskey="n"><fmt:message key="editor.plain.name"/></label>
    <input type="text" name="author" id="authorname" value="${author}" />
    <input type="checkbox" name="remember" id="rememberme" <%=TextUtil.isPositive((String)session.getAttribute("remember")) ? "checked='checked'" : ""%> />
    <label for="rememberme"><fmt:message key="editor.plain.remember"/></label>
    </p>
	<%--FIXME: seems not to read the email of the user, but some odd previously cached value --%>
    <p>
    <label for="link" accesskey="m"><fmt:message key="editor.plain.email"/></label>
    <input type="text" name="link" id="link" size="24" value="${link}" />
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
