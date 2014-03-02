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

<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.permissions.*" %>
<%@ page import="org.apache.wiki.tags.*" %>
<%@ page import="org.apache.wiki.filters.SpamFilter" %>
<%@ page import="org.apache.wiki.ui.*" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="org.apache.wiki.rpc.*" %>
<%@ page import="org.apache.wiki.rpc.json.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%--
        This is a plain editor for JSPWiki.
--%>
<%
   WikiContext context = WikiContext.findContext( pageContext );
   WikiEngine engine = context.getEngine();

   TemplateManager.addResourceRequest( context, TemplateManager.RESOURCE_SCRIPT,
           context.getURL( WikiContext.NONE, "scripts/haddock-edit.js" ) );
   String usertext = EditorManager.getEditedText( pageContext );
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
<% if( usertext == null ) usertext = "";  %>

<%-- <div style="width:100%"> <%-- Required for IE6 on Windows --%>

<form action="<wiki:CheckRequestContext
     context='edit'><wiki:EditLink format='url'/></wiki:CheckRequestContext><wiki:CheckRequestContext
     context='comment'><wiki:CommentLink format='url'/></wiki:CheckRequestContext>"
       class=""
          id="editform"
      method="post" accept-charset="<wiki:ContentEncoding/>"
     enctype="application/x-www-form-urlencoded" >

  <%-- Edit.jsp relies on these being found.  So be careful, if you make changes. --%>
  <div class="form-group form-inline">
  <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
  <input type="hidden" name="action" value="save" />
  <%=SpamFilter.insertInputFields( pageContext )%>
  <input type="hidden" name="<%=SpamFilter.getHashFieldName(request)%>" value="<c:out value='${lastchange}' />" />
  
  <input class="btn btn-primary" type="submit" name="ok" accesskey="s"
         value="<fmt:message key='editor.plain.save.submit'/>"
         title="<fmt:message key='editor.plain.save.title'/>" />
<%--
  <input class="btn btn-primary" type="submit" name="preview" accesskey="v"
         value="<fmt:message key='editor.plain.preview.submit'/>"
         title="<fmt:message key='editor.plain.preview.title'/>" />
--%>
  <input class="btn btn-danger pull-right" type="submit" name="cancel" accesskey="q"
         value="<fmt:message key='editor.plain.cancel.submit'/>"
         title="<fmt:message key='editor.plain.cancel.title'/>" />

  <%-- This following field is only for the SpamFilter to catch bots which are just randomly filling all fields and submitting.
       Normal user should never see this field, nor type anything in it. --%>
  <input class="hidden" type="text" name="<%=SpamFilter.getBotFieldName()%>" id="<%=SpamFilter.getBotFieldName()%>" value="" />

  <%--TODO
    <wiki:Permission permission="rename">
    <div class="form-group form-inline">
    <label for="renameto"><fmt:message key='editor.renameto'/></label>
    <input type="text" name="renameto" value="<wiki:Variable var='pagename' />" size="40" />
    <input type="checkbox" name="references" checked="checked" />
    <fmt:message key="info.updatereferrers"/>
    </div>
    </wiki:Permission>
  --%>

  <wiki:CheckRequestContext context="edit">
  <input class="form-control form-col-50" type="text" size="80" maxlength="80"
         name="changenote" id="changenote" 
         placeholder="<fmt:message key='editor.plain.changenote'/>"
         value="${changenote}"/>
  </wiki:CheckRequestContext>
  <wiki:CheckRequestContext context="comment">
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
  </wiki:CheckRequestContext>

  </div>  
  
<div class="snipe">

<div class="toolbar row">

  <div class="btn-group">

  <div class="cage" style="float:left">
    <div class="btn btn-link"><span class="icon-bookmark"><span class="caret"></span></div>
    <ul class="dropdown-menu" data-sections="div">
      <li><a>first</a></li>
      <li><a>..</a></li>
      <li><a class="dropdown-divider">..</a></li>
      <li><a>..</a></li>
    </ul>
  </div>

  <div class="btn btn-link" data-cmd="undo" title="<fmt:message key='editor.plain.undo.title'/>"><span class="icon-undo"></span></div>
  <div class="btn btn-link" data-cmd="redo" title="<fmt:message key='editor.plain.redo.title'/>"><span class="icon-repeat"></span></div>
  <div class="btn btn-link" data-cmd="find" title="<fmt:message key='editor.plain.find.title'/>"><span class="icon-search"></span></div>  

  <div class="cage" style="float:left">
    <div class="btn btn-link"><span class="icon-wrench"></span><span class="caret"></span></div>
    <ul class="dropdown-menu" data-hover-parent=".cage">
    <li><a><label for="autosuggest">
        <input type="checkbox" data-cmd="autosuggest" id="autosuggest" />
        <fmt:message key='editor.plain.autosuggest'/>
    </label></a></li>
    <li><a><label for="tabcompletion">
        <input type="checkbox" data-cmd="tabcompletion" id="tabcompletion" />
        <fmt:message key='editor.plain.tabcompletion'/>
    </label></a></li>
    <li><a><label for="smartpairs">
        <input type="checkbox" data-cmd="smartpairs" id="smartpairs" />
        <fmt:message key='editor.plain.smartpairs'/>
    </label></a></li>
    <li class="divider"></li>
    <li><a><label for="livepreview">
        <input type="checkbox" data-cmd="livepreview" id="livepreview"/>
        <fmt:message key='editor.plain.livepreview'/> <span class="icon-refresh">
    </label></a></li>
    <li><a><label for="previewcolumn">
        <input type="checkbox" data-cmd="previewcolumn" id="previewcolumn" />
        Preview Side by Side <span class="icon-columns">
    </label></a></li>
    </ul>
    </div>
  </div>

  <div class="btn-group">
    <%--<div class="btn btn-link" data-cmd="h" title="<fmt:message key='editor.plain.tbH1.title'/>"><span class="icon-header"></span></div>--%>
    <div class="btn btn-link" data-cmd="bold" title="<fmt:message key='editor.plain.tbB.title'/>"><span class="icon-bold"></span></div>
    <div class="btn btn-link" data-cmd="italic" title="<fmt:message key='editor.plain.tbI.title'/>"><span class="icon-italic"></span></div>
    <%--<div class="btn btn-link  tMONO" title="<fmt:message key='editor.plain.tbMONO.title'/>"><i>mono</i></div>--%>
    <div class="btn btn-link" data-cmd="css" title="<fmt:message key='editor.plain.tbCSS.title'/>"><span class="icon-tint"></span></div>
    <div class="btn btn-link" data-cmd="font" title="<fmt:message key='editor.plain.tbFONT.title'/>"><span class="icon-font"></span></div>
    <div class="btn btn-link" data-cmd="color" title="<fmt:message key='editor.plain.tbCOLOR.title'/>"><span class="icon-none"></span></div>
    <%--<div class="btn btn-link  tPRE" title="<fmt:message key='editor.plain.tbPRE.title'/>"><i>pre</i></div>--%>
    <%--<div class="btn btn-link  tCODE" title="<fmt:message key='editor.plain.tbCODE.title'/>"><i>code</i></div>--%>
    <%--<div class="btn btn-link  tHR" title="<fmt:message key='editor.plain.tbHR.title'/>"><i>hr</i></div>--%>
  </div>

  <div class="btn-group">
    <div class="btn btn-link" data-cmd="link" title="<fmt:message key='editor.plain.tbLink.title'/>"><span class="icon-link"></span></div>
    <div class="btn btn-link" data-cmd="img" title="<fmt:message key='editor.plain.tbIMG.title'/>"><span class="icon-picture"></span></div>
    <%--<div class="cmd btn btn-link" data-cmd="table" title="<fmt:message key='editor.plain.tbTABLE.title'/>"><i>table</i></div>--%>
    <div class="btn btn-link" data-cmd="char" title="<fmt:message key='editor.plain.tbCHAR.title'/>"><span class="icon-euro"></div>
    <div class="btn btn-link" data-cmd="plugin" title="<fmt:message key='editor.plain.tbPLUGIN.title'/>"><span class="icon-plus"></div>
    <%--<div class="btn btn-link  tBR" title="<fmt:message key='editor.plain.tbBR.title'/>"><i>br</i></div>--%>
    <%--<div class="cmd btn btn-link" data-cmd="sign" title="<fmt:message key='editor.plain.tbSIGN.title'/>"><span class="icon-user"></span></div>--%>
    <%--<div class="cmd btn btn-link" data-cmd="acl" title="<fmt:message key='editor.plain.tbACL.title'/>"><span class="icon-lock"></span></div>--%>
    <%--<div class="cmd tDL" title="<fmt:message key='editor.plain.tbDL.title'/>"><i>dl</i></div>--%>
  </div>

  <div class="dialog float find">
    <div class="caption"><fmt:message key='editor.plain.find'/> &amp; <fmt:message key='editor.plain.replace'/> </div>
      <div class="form-group">
        <span class="tbHITS"></span>
        <input class="form-control" type="text" name="tbFIND" size="16" 
               placeholder="<fmt:message key='editor.plain.find'/>" />
      </div>
      <div class="form-group">      
      <input class="form-control" type="text" name="tbREPLACE" size="16" 
             placeholder="<fmt:message key='editor.plain.replace'/>" />
      </div>
      <div class="btn-group">      
        <button class="btn btn-primary" type="button" name="replace">  
          <fmt:message key='editor.plain.find.submit' /> first
        </button>
        <button class="btn btn-primary" type="button" name="replaceall">
          <fmt:message key='editor.plain.global'/>
        </button>
        <label class="btn btn-default" for="tbMatchCASE">
          <input type="checkbox" name="tbMatchCASE" id="tbMatchCASE"/>
          <fmt:message key="editor.plain.matchcase"/>
        </label>
        <label class="btn btn-default" for="tbREGEXP">
          <input type="checkbox" name="tbREGEXP" id="tbREGEXP"/>
          <fmt:message key="editor.plain.regexp"/>
        </label>
      </div>
       
  </div>
      
</div><%-- end of .toolbar --%>

  
<div class="row edit-area">
  <div class="col-50" >
    <textarea class="editor form-control" id="editorarea" name="<%=EditorManager.REQ_EDITEDTEXT%>"         
                autofocus="autofocus"
                rows="20" cols="80"><%=TextUtil.replaceEntities(usertext)%></textarea>
  </div>
  <div class="ajaxpreview col-50" ></div>
</div>
<div class="resizer" title="<fmt:message key='editor.plain.edit.resize'/>"></div>

</div><%-- end of .snipe --%>

</form>


<%-- </div>   ??CHECK: needed of IEx--%>