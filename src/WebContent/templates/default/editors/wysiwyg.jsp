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
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
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
    This provides a WYSIWYG editor for JSPWiki, based on mooeditable.

	FIXME:
		first hack of the wysiwyg editor for jspwiki

	Clientside js is based on "mooeditable".
	See http://github.com/cheeaun/mooeditable, by Lim Chee Aun.

--%>
<%
    WikiContext context = WikiContextFactory.findContext( pageContext );
    WikiEngine engine = context.getEngine();
    //c.setVariable( RenderingManager.WYSIWYG_EDITOR_MODE, Boolean.TRUE );
    //c.setVariable( WikiEngine.PROP_RUNFILTERS,  "false" );

    WikiPage wikiPage = context.getPage();
    //String originalCCLOption = (String)wikiPage.getAttribute(JSPWikiMarkupParser.PROP_CAMELCASELINKS );
    //wikiPage.setAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS, "false" );

    //String usertext = EditorManager.getEditedText(pageContext);
	String usertext = engine.getPureText( wikiPage );
%>
<s:layout-render name="${templates['layout/DefaultLayout.jsp']}">

  <%-- Page title should say Edit: + pagename --%>
  <s:layout-component name="headTitle">
    <fmt:message key="edit.title.edit">
      <fmt:param><wiki:Variable var="ApplicationName" /></fmt:param>
      <fmt:param><wiki:PageName/></fmt:param>
    </fmt:message>
  </s:layout-component>

  <%-- Add Javascript for WYSIWYG editor --%>
  <s:layout-component name="script">
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-edit.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/dialog.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/wysiwyg/MooEditable.js' />"></script>
  </s:layout-component>

  <%-- Stylesheets for WYSIWYG editor --%>
  <s:layout-component name="stylesheet">
    <link rel="stylesheet" media="screen, projection, print" type="text/css" href="<wiki:Link format='url' jsp='scripts/wysiwyg/MooEditable.css' />" />
    <link rel="stylesheet" media="screen, projection, print" type="text/css" href="<wiki:Link format='url' jsp='scripts/wysiwyg/MooEditableSilkTheme.css' />" />
  </s:layout-component>
    
  <s:layout-component name="content">
    <%-- FIXME: select CommentLayout or EditorLayout based on "comment" or "edit" event--%>
    <s:layout-render name="${templates['layout/EditorLayout.jsp']}">
      <s:layout-component name="editor">
        
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
          <%--
              if( usertext == null )
              {
                  usertext = engine.getPureText( context.getPage() );
              }
          --%>
        </wiki:CheckRequestContext>
        <%
           if( usertext == null ) usertext = "";
        
           RenderingManager renderingManager = new RenderingManager();
        
           // since the WikiProperties are shared, we'll want to make our own copy of it for modifying.
           Properties props = new Properties();
           props.putAll( engine.getWikiProperties() );
           props.setProperty( "jspwiki.renderingManager.renderer", WysiwygEditingRenderer.class.getName() );
           renderingManager.initialize( engine, props );
        
           //String pageAsHtml = StringEscapeUtils.escapeJavaScript( renderingManager.getHTML( context, usertext ) );
           String pageAsHtml = renderingManager.getHTML( context, usertext ) ;
        
           // Disable the WYSIWYG_EDITOR_MODE and reset the other properties immediately
           // after the XHTML has been rendered.
           //context.setVariable( RenderingManager.WYSIWYG_EDITOR_MODE, Boolean.FALSE );
           //context.setVariable( WikiEngine.PROP_RUNFILTERS, null );
           //wikiPage.setAttribute( JSPWikiMarkupParser.PROP_CAMELCASELINKS, originalCCLOption );
        
        %>
        
        <div style="width:100%"> <%-- Required for IE6 on Windows --%>
        
          <%-- Print any validation errors --%>
          <s:errors />
        
          <s:form beanclass="org.apache.wiki.action.EditActionBean"
                      class="wikiform"
                         id="editform"
                     method="post"
              acceptcharset="UTF-8"
                    enctype="application/x-www-form-urlencoded" >
        
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
              <s:hidden name="append" />
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
        
            <%-- Fields for changenote, renaming etc. --%>
            <table>
              <tr>
                <td>
                  <s:label for="changenote" name="changenote" />
                </td>
                <td>
                  <s:text name="changenote" id="changenote" size="50" maxlength="80" />
                </td>
              </tr>
        
              <wiki:CheckRequestContext context="comment">
              <tr>
                <td><s:label for="author" accesskey="n" name="author" /></td>
                <td><s:text id="author" name="author" />
                    <s:checkbox id="remember" name="remember" />
                    <s:label for="remember" name="remember" />
                </td>
              </tr>
              <tr>
                <td><s:label for="link" accesskey="m" name="editor.plain.email" /></td>
                <td><s:text id="link" name="link" size="24" /></td>
              </tr>
              </wiki:CheckRequestContext>
        
            </table>
        
          <%-- FIXME: just for testing...
          <s:textarea id="htmlPageText" name="htmlPageText" class="editor" rows="10" cols="80" />
          --%>
          <%-- FIXME:
          	   Remove all <a class="hashlink" >.</a> from the pageAsHtml !!
          --%>
          <textarea id="htmlPageText" name="htmlPageText" class="editor" rows="20" cols="80" ><%= pageAsHtml%></textarea>
        
          <%-- FIXME: fake textarea to fool the action bean --%>
          <div style="display:none">
          <s:textarea id="wikiText" name="wikiText" class="editor" rows="20" cols="80" />
          </div>
        
        
          <%-- Spam detection fields --%>
          <wiki:SpamProtect />
        
        </s:form>
        </div>
        
        <script type="text/javascript">
        //<![CDATA[
        
        //FIXME: move this to jspwiki-edit.js
        window.addEvent('load', function(){
        	$('htmlPageText').mooEditable();
        
        	// Post submit
        	/*
        	$('editform').addEvent('submit', function(e){
        		alert($('htmlPageText').value);
        		return true;
        	});
        	*/
        });
        
        //]]>
        </script>
        
      </s:layout-component>
    </s:layout-render>
  </s:layout-component>

</s:layout-render>
