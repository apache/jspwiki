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
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.permissions.*" %>
<%@ page import="org.apache.wiki.render.*" %>
<%@ page import="org.apache.wiki.parser.JSPWikiMarkupParser" %>
<%@ page import="org.apache.wiki.ui.*" %>

<%@ page import="org.apache.wiki.util.TextUtil" %>

<%@ page import="org.apache.wiki.filters.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%--
    This provides the WYSIWYG TinyMCE for JSPWiki.
--%>
<%
    WikiContext context = WikiContext.findContext( pageContext );
    WikiEngine engine = context.getEngine();

    /* local download of TinyMCE */
    TemplateManager.addResourceRequest( context, TemplateManager.RESOURCE_SCRIPT,
           context.getURL( WikiContext.NONE, "scripts/tinymce/js/tinymce/tinymce.min.js" ) );

    /*  Use TinyMCE from a CDN
    TemplateManager.addResourceRequest( context, TemplateManager.RESOURCE_SCRIPT,
           "//tinymce.cachefly.net/4.2/tinymce.min.js" );
    */

    context.setVariable( RenderingManager.WYSIWYG_EDITOR_MODE, Boolean.TRUE );
    context.setVariable( WikiEngine.PROP_RUNFILTERS,  "false" );

    WikiPage wikiPage = context.getPage();
    String originalCCLOption = (String)wikiPage.getAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS );
    wikiPage.setAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS, "false" );

    String usertext = EditorManager.getEditedText(pageContext);

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
  }
%>
</wiki:CheckRequestContext>
<%
    if( usertext == null ) usertext = "";

    RenderingManager renderingManager = new RenderingManager();

    // since the WikiProperties are shared, we'll want to make our own copy of it for modifying.
    Properties copyOfWikiProperties = new Properties();
    copyOfWikiProperties.putAll( engine.getWikiProperties() );
    copyOfWikiProperties.setProperty( "jspwiki.renderingManager.renderer", WysiwygEditingRenderer.class.getName() );
    renderingManager.initialize( engine, copyOfWikiProperties );

    String pageAsHtml;
    try
    {
        //pageAsHtml = StringEscapeUtils.escapeJavaScript( renderingManager.getHTML( context, usertext ) );
        pageAsHtml = renderingManager.getHTML( context, usertext );

    }
        catch( Exception e )
    {
        pageAsHtml = "<div class='error'>Error in converting wiki-markup to well-formed HTML <br/>" + e.toString() +  "</div>";

        /*
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        pageAsHtml += "<pre>" + sw.toString() + "</pre>";
        */
    }


   // Disable the WYSIWYG_EDITOR_MODE and reset the other properties immediately
   // after the XHTML for TinyMCE has been rendered.
   context.setVariable( RenderingManager.WYSIWYG_EDITOR_MODE, Boolean.FALSE );

   context.setVariable( WikiEngine.PROP_RUNFILTERS,  null );
   wikiPage.setAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS, originalCCLOption );


   /*FSS not used
   String templateDir = (String)copyOfWikiProperties.get( WikiEngine.PROP_TEMPLATEDIR );

   String protocol = "http://";
   if( request.isSecure() )
   {
       protocol = "https://";
   }
   */
%>
<form method="post" accept-charset="<wiki:ContentEncoding/>"
      action="<wiki:CheckRequestContext
     context='edit'><wiki:EditLink format='url'/></wiki:CheckRequestContext><wiki:CheckRequestContext
     context='comment'><wiki:CommentLink format='url'/></wiki:CheckRequestContext>"
       class="editform wysiwyg"
          id="editform"
     enctype="application/x-www-form-urlencoded" >

  <%-- Edit.jsp relies on these being found.  So be careful, if you make changes. --%>
  <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
  <input type="hidden" name="action" value="save" />
  <%=SpamFilter.insertInputFields( pageContext )%>
  <input type="hidden" name="<%=SpamFilter.getHashFieldName(request)%>" value="${lastchange}" />
  <%-- This following field is only for the SpamFilter to catch bots which are just randomly filling all fields and submitting.
       Normal user should never see this field, nor type anything in it. --%>
  <input class="hidden" type="text" name="<%=SpamFilter.getBotFieldName()%>" id="<%=SpamFilter.getBotFieldName()%>" value="" />


  <div class="form-inline form-group">

  <span class="cage">
    <input class="btn btn-success" type="submit" name="ok" accesskey="s"
           value="<fmt:message key='editor.plain.save.submit'/>"
           title="<fmt:message key='editor.plain.save.title'/>" />

      <wiki:CheckRequestContext context="edit">
      <input class="form-control" data-hover-parent="span" type="text" size="80" maxlength="80"
             name="changenote" id="changenote"
             placeholder="<fmt:message key='editor.plain.changenote'/>"
             value="${changenote}" />
      </wiki:CheckRequestContext>
  </span>

  <div class="btn-group editor-tools">

    <div class="btn-group config">
      <%-- note: 'dropdown-toggle' is only here to style the last button properly! --%>
      <button class="btn btn-default dropdown-toggle"><span class="icon-wrench"></span><span class="caret"></span></button>
      <ul class="dropdown-menu" data-hover-parent="div">
            <li>
              <a>
                <label for="livepreview">
                  <input type="checkbox" data-cmd="livepreview" id="livepreview"/>
                  <fmt:message key='editor.plain.livepreview'/> <span class="icon-refresh"/>
                </label>
              </a>
            </li>
            <li>
              <a>
                <label for="previewcolumn">
                  <input type="checkbox" data-cmd="previewcolumn" id="previewcolumn" />
                  <fmt:message key='editor.plain.sidebysidepreview'/> <span class="icon-columns"/>
                </label>
              </a>
            </li>

      </ul>
    </div>

    <c:set var="editors" value="<%= engine.getEditorManager().getEditorList() %>" />
    <c:if test='${fn:length(editors)>1}'>
   <div class="btn-group config">
      <%-- note: 'dropdown-toggle' is only here to style the last button properly! --%>
      <button class="btn btn-default dropdown-toggle"><span class="icon-pencil"></span><span class="caret"></span></button>
      <ul class="dropdown-menu" data-hover-parent="div">
        <c:forEach items="${editors}" var="edt">
          <c:choose>
            <c:when test="${edt != prefs.editor}">
              <li>
                <wiki:Link context="edit" cssClass="editor-type">${edt}</wiki:Link>
              </li>
            </c:when>
            <c:otherwise>
              <li class="active"><a>${edt}</a></li>
            </c:otherwise>
          </c:choose>
      </c:forEach>
      </ul>
    </div>
    </c:if>

  </div>

  <div class="form-group pull-right">

  <%-- is PREVIEW functionality still needed - with livepreview ?
  <input class="btn btn-primary" type="submit" name="preview" accesskey="v"
         value="<fmt:message key='editor.plain.preview.submit'/>"
         title="<fmt:message key='editor.plain.preview.title'/>" />
  --%>
  <input class="btn btn-danger" type="submit" name="cancel" accesskey="q"
         value="<fmt:message key='editor.plain.cancel.submit'/>"
         title="<fmt:message key='editor.plain.cancel.title'/>" />

  </div>
  <%--TODO: allow page rename as part of an edit session
    <wiki:Permission permission="rename">
    <div class="form-group form-inline">
    <label for="renameto"><fmt:message key='editor.renameto'/></label>
    <input type="text" name="renameto" value="<wiki:Variable var='pagename' />" size="40" />
    <input type="checkbox" name="references" checked="checked" />
    <fmt:message key="info.updatereferrers"/>
    </div>
    </wiki:Permission>
  --%>

  </div>

  <wiki:CheckRequestContext context="comment">
    <div class="info">
    <label><fmt:message key="editor.commentsignature"/></label>
    <input class="form-control form-col-20" type="text" name="author" id="authorname"
           placeholder="<fmt:message key='editor.plain.name'/>"
           value="${author}" />
    <label class="btn btn-default btn-sm" for="rememberme">
      <input type="checkbox" name="remember" id="rememberme"
             <%=TextUtil.isPositive((String)session.getAttribute("remember")) ? "checked='checked'" : ""%> />
      <fmt:message key="editor.plain.remember"/>
    </label>
    <input class="form-control form-col-20" type="text" name="link" id="link" size="24"
           placeholder="<fmt:message key='editor.plain.email'/>"
           value="${link}" />
    </div>
  </wiki:CheckRequestContext>


  <div class="row edit-area livepreview previewcolumn"><%-- .livepreview  .previewcolumn--%>
      <div class="col-50">
        <textarea name="htmlPageText"
             autofocus="autofocus"><%=pageAsHtml%></textarea>
      </div>
      <div class="ajaxpreview col-50" >Preview comes here</div>
  </div>

</form>
<script type="text/javascript">
//<![CDATA[

Wiki.add("[name=htmlPageText]", function( element){

    function containerHeight(){ return $(editor.getContainer()).getStyle("height"); }
    function editorHeight(){ return $(editor.getContentAreaContainer()).getStyle("height"); }
    function editorContent(){ return editor.getContent(); }
    function resizePreview(){ preview.setStyle("height", containerHeight()); }

    var form = element.form,
        editor,
        preview = form.getElement(".ajaxpreview"),
        resizer = form.getElement(".resizer"),
        resizeCookie = "editorHeight",

        html2markup = Wiki.getXHRPreview( editorContent, preview );

    Wiki.configPrefs( form, function(cmd, isChecked){
        if(isChecked && (cmd=="livepreview")){ html2markup(); }
    });

    tinymce.init({
      selector: "textarea[name=htmlPageText]",
      //content_css : "template/haddock/haddock.css",
      //extended_valid_elements: "...]",
      //invalid_elements: blockquote ...",
      language: Wiki.prefs.get( "Language" ), //"${prefs.Language}",
      height: Wiki.prefs.get( resizeCookie ),
      content_css: $("main-stylesheet").href,
      //plugins from the standard 4.2.2. package
      plugins: 'anchor charmap code fullscreen image insertdatetime link lists media paste preview searchreplace table wordcount',

      setup: function( ed ){

        editor = ed;

        editor.on("init", function(){
            // container height is apparently not yet properly initialized at this time - ugh!
            (function () {
                resizePreview();
                html2markup();
            }).delay(100);
        });
        editor.on("ResizeEditor", function(){
           Wiki.prefs.set(resizeCookie, editorHeight() );
           resizePreview();
        });
        editor.on("change", html2markup );

      }
    });
});

//]]>
</script>
