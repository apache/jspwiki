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
<%@ page import="org.apache.log4j.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.render.*" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%!
  public void jspInit()
  {
    wiki = WikiEngine.getInstance( getServletConfig() );
  }
  //Logger log = Logger.getLogger("XHRMarkup2Wysiwyg");
  WikiEngine wiki;
%>
<%
  WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW );
  //WikiEngine engine = wikiContext.getEngine();

  if( !wiki.getAuthorizationManager().hasAccess( wikiContext, response ) ) return;

  response.setContentType("text/html; charset="+wiki.getContentEncoding() );
  //response.setHeader( "Cache-control", "max-age=0" );
  //response.setDateHeader( "Expires", new Date().getTime() );
  //response.setDateHeader( "Last-Modified", new Date().getTime() );

  String usertext = request.getParameter( "markupPageText" );

  if( usertext != null )
  {

    RenderingManager renderingManager = new RenderingManager();

    // since the WikiProperties are shared, we'll want to make our own copy of it for modifying.
    Properties copyOfWikiProperties = new Properties();
    copyOfWikiProperties.putAll( wiki.getWikiProperties() );
    copyOfWikiProperties.setProperty( "jspwiki.renderingManager.renderer", WysiwygEditingRenderer.class.getName() );
    renderingManager.initialize( wiki, copyOfWikiProperties );

    String pageAsHtml;
    try
    {
        pageAsHtml = renderingManager.getHTML( wikiContext, usertext );

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
   // after the XHTML for wysiwyg editor has been rendered.
   context.setVariable( RenderingManager.WYSIWYG_EDITOR_MODE, Boolean.FALSE );
   context.setVariable( WikiEngine.PROP_RUNFILTERS,  null );


%><%= pageAsHtml %><%
  }
%>