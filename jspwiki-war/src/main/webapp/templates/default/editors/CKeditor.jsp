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
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%--
    This provides the WYSIWYG CKeditor for JSPWiki.
--%>
<%
    WikiContext context = WikiContext.findContext( pageContext );
    WikiEngine engine = context.getEngine();

    /* local download of CKeditor */
    TemplateManager.addResourceRequest( context, TemplateManager.RESOURCE_SCRIPT,
           context.getURL( WikiContext.NONE, "scripts/ckeditor/ckeditor.js" ) );

    /*  Use CKEditor from a CDN
    TemplateManager.addResourceRequest( context, TemplateManager.RESOURCE_SCRIPT,
           "//cdn.ckeditor.com/4.5.1/standard/ckeditor.js" );
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

   RenderingManager renderingManager = engine.getRenderingManager();

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
        pageAsHtml = "Error in converting wiki-markup to well-formed HTML \n" + e.toString();
        //pageAsHtml = e.toString() + "\n" + usertext; //error
    }

   // Disable the WYSIWYG_EDITOR_MODE and reset the other properties immediately
   // after the XHTML for CKeditor has been rendered.
   context.setVariable( RenderingManager.WYSIWYG_EDITOR_MODE, Boolean.FALSE );

   context.setVariable( WikiEngine.PROP_RUNFILTERS,  null );
   wikiPage.setAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS, originalCCLOption );

   /*not used
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
  <div style="display:none;">Authentication code: <input type="text" name="<%=SpamFilter.getBotFieldName()%>" id="<%=SpamFilter.getBotFieldName()%>" value="" /></div>

    <p>
      <input name='ok' type='submit' value='<fmt:message key="editor.plain.save.submit"/>' />
      <input name='preview' type='submit' value='<fmt:message key="editor.plain.preview.submit"/>' />
      <input name='cancel' type='submit' value='<fmt:message key="editor.plain.cancel.submit"/>' />

      <wiki:CheckRequestContext context="edit">
      <label for="changenote"><fmt:message key='editor.plain.changenote'/></label>
      <input type="text" id="changenote" name="changenote" size="80" maxlength="80" value="${changenote}"/>
      </wiki:CheckRequestContext>
    </p>
    <wiki:CheckRequestContext context="comment">
      <fieldset>
	    <legend><fmt:message key="editor.commentsignature"/></legend>
        <p>
          <label for="authorname" accesskey="n"><fmt:message key="editor.plain.name"/></label>

          <%--<input type="text" name="author" id="authorname" value="${author}" />--%>
          <input type="text" name="author" id="authorname" value="<c:out value='${sessionScope.author}' />" />

          <input type="checkbox" name="remember" id="rememberme" <%=TextUtil.isPositive((String)session.getAttribute("remember")) ? "checked='checked'" : ""%> />
          <label for="rememberme"><fmt:message key="editor.plain.remember"/></label>
        </p>
	    <%--FIXME: seems not to read the email of the user, but some odd previously cached value --%>
        <p>
          <label for="link" accesskey="m"><fmt:message key="editor.plain.email"/></label>
          <%--<input type="text" name="link" id="link" size="24" value="${link}" />--%>
          <input type="text" name="link" id="link" size="24" value="<c:out value='${sessionScope.link}' />" />
        </p>
      </fieldset>
    </wiki:CheckRequestContext>

    <textarea name="htmlPageText"><%=pageAsHtml%></textarea>
</form>
<script type="text/javascript">
//<![CDATA[

/* some helper function */
    /*
    Event: debounce
        Returns a function, that, as long as it continues to be invoked, will not
        be triggered. The function will be called after it stops being called for
        N milliseconds. If `immediate` is passed, trigger the function on the
        leading edge, instead of the trailing.
        I.e. collapse a number of events into a single event.

    Credits:
        http://davidwalsh.name/function-debounce

    Example:
        el.addEvent('resize', resizePage.debounce(250, true).bind(this) );
    */
    function debounce(func, wait, immediate){

        var timer;

        return function(){

            var args = arguments,
                context = this,
                callNow = immediate && !timer;

            function later(){
                timer = null;
                if( !immediate ){ func.apply(context, args); }
            }

            clearTimeout(timer);
            timer = setTimeout(later, wait || 250);

            if( callNow ){ func.apply(context, args); }

        };

    }


Wiki.addPageRender({

render: function( page, name ){

    $ES("[name=htmlPageText]", page).each( function(element){

    	CKEDITOR.replace(element,{
          //uiColor: "#e5e8ed",
          //allowedContent:" ...  ",
          disallowedContent:"h1;h5;h6;blockquote",
          language: Wiki.prefs.get( "Language" ),
          height: Wiki.prefs.get( "EditorCookie" ),
          startupFocus: true,
          contentsCss: $("main-stylesheet").href,

          on: {
            instanceReady : function(eventReady) {

                eventReady.editor.on( "resize", ( debounce( function( eventResize ){
                   Wiki.prefs.set("EditorCookie", eventResize.data.contentsHeight);
                }) ) );
            }
          }
        });

    });
}

});

//]]>
</script>
