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
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.permissions.*" %>
<%@ page import="org.apache.wiki.tags.*" %>
<%@ page import="org.apache.wiki.filters.SpamFilter" %>
<%@ page import="org.apache.wiki.ui.*" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%--
        This is a plain editor for JSPWiki.
--%>
<%
   WikiContext context = WikiContext.findContext( pageContext );
   WikiEngine engine = context.getEngine();

   String usertext = EditorManager.getEditedText( pageContext );
%>
<wiki:RequestResource type="script" resource="scripts/haddock-edit.js" />
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

<form method="post" accept-charset="<wiki:ContentEncoding/>"
      action="<wiki:CheckRequestContext
     context='edit'><wiki:EditLink format='url'/></wiki:CheckRequestContext><wiki:CheckRequestContext
     context='comment'><wiki:CommentLink format='url'/></wiki:CheckRequestContext>"

     <%--action="<wiki:Link context=${context} format='url'/>"--%>

       class="editform"
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


  <div class="snipe">
  <div class="form-inline form-group">

  <span class="cage">
    <input class="btn btn-success" type="submit" name="ok" accesskey="s"
           value="<fmt:message key='editor.plain.save.submit'/>" />

    <wiki:CheckRequestContext context="edit">
      <input class="form-control" data-hover-parent="span" type="text" size="80" maxlength="80"
             name="changenote" id="changenote"
             placeholder="<fmt:message key='editor.plain.changenote'/>"
             value="${changenote}" />
    </wiki:CheckRequestContext>
    <wiki:CheckRequestContext context="comment">
      <div class="commentsignature form-inline form-group" data-hover-parent="span">
        <label><fmt:message key="editor.commentsignature"/></label>
        <input class="form-control" type="text" name="author" id="authorname"
             placeholder="<fmt:message key='editor.plain.name'/>"
             value="${author}" />
        <label class="btn btn-default btn-xs" for="rememberme">
          <input type="checkbox" name="remember" id="rememberme"
            <%=TextUtil.isPositive((String)session.getAttribute("remember")) ? "checked='checked'" : ""%> />
          <fmt:message key="editor.plain.remember"/>
        </label>
        <input class="form-control" type="text" name="link" id="link" size="24"
               placeholder="<fmt:message key='editor.plain.email'/>"
               value="${link}" />
      </div>
    </wiki:CheckRequestContext>
  </span>

  <div class="btn-group editor-tools">

    <div class="btn-group sections">
      <button class="btn btn-default"><span class="icon-bookmark"><span class="caret"></span></button>
      <ul class="dropdown-menu" data-hover-parent="div">
            <li><a>first</a></li>
            <li><a>..</a></li>
            <li><a class="dropdown-divider">..</a></li>
            <li><a>..</a></li>
      </ul>
    </div>

    <div class="btn-group formatting-options">
      <button class="btn btn-default"><span class="icon-tint" /><span class="caret" /></button>
      <ul class="dropdown-menu dropdown-menu-horizontal" data-hover-parent="div">
        <li><a href="#" data-cmd="bold" title="<fmt:message key='editor.plain.tbB.title' />"><b>bold</b></a></li>
        <li><a href="#" data-cmd="italic" title="<fmt:message key='editor.plain.tbI.title' />"><i>italic</i></a></li>
        <li><a href="#" data-cmd="mono" title="<fmt:message key='editor.plain.tbMONO.title' />"><tt>mono</tt></a></li>
        <li><a href="#" data-cmd="sub" title="<fmt:message key='editor.plain.tbSUB.title' />">a<span class="sub">sub</span></a></li>
        <li><a href="#" data-cmd="sup" title="<fmt:message key='editor.plain.tbSUP.title' />">a<span class="sup">sup</span></a></li>
        <li><a href="#" data-cmd="strike" title="<fmt:message key='editor.plain.tbSTRIKE.title' />"><span class="strike">strike</span></a></li>
        <li><a href="#" data-cmd="link" title="<fmt:message key='editor.plain.tbLink.title'/>"><span class="icon-link"/></a></li>
        <li><a href="#" data-cmd="img" title="<fmt:message key='editor.plain.tbIMG.title'/>"><span class="icon-picture"/></a></li>
        <li><a href="#" data-cmd="plugin" title="<fmt:message key='editor.plain.tbPLUGIN.title'/>"><span class="icon-puzzle-piece"/></a></li>
        <li><a href="#" data-cmd="font" title="<fmt:message key='editor.plain.tbFONT.title' />">Font<span class="caret" /></a></li>
        <li><a href="#" data-cmd="chars" title="<fmt:message key='editor.plain.tbCHARS.title' />"><span class="icon-euro"/><span class="caret" /></a></li>

       </ul>
     </div>


    <fmt:message key='editor.plain.undo.title' var='msg'/>
    <button class="btn btn-default" data-cmd="undo" title="${msg}"><span class="icon-undo"></span></button>
    <fmt:message key='editor.plain.redo.title' var='msg'/>
    <button class="btn btn-default" data-cmd="redo" title="${msg}"><span class="icon-repeat"></span></button>
    <button class="btn btn-default" data-cmd="find"><span class="icon-search" /></button>

    <div class="btn-group config">
      <%-- note: 'dropdown-toggle' is only here to style the last button properly! --%>
      <button class="btn btn-default"><span class="icon-wrench"></span><span class="caret"></span></button>
      <ul class="dropdown-menu" data-hover-parent="div">
            <li>
              <a>
                <label for="autosuggest">
                  <input type="checkbox" data-cmd="autosuggest" id="autosuggest" />
                  <fmt:message key='editor.plain.autosuggest'/>
                </label>
              </a>
            </li>
            <li>
              <a>
                <label for="tabcompletion">
                  <input type="checkbox" data-cmd="tabcompletion" id="tabcompletion" />
                  <fmt:message key='editor.plain.tabcompletion'/>
                </label>
              </a>
            </li>
            <li>
              <a>
                <label for="smartpairs">
                  <input type="checkbox" data-cmd="smartpairs" id="smartpairs" />
                  <fmt:message key='editor.plain.smartpairs'/>
                </label>
              </a>
            </li>
            <li class="divider"></li>
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

    <c:set var="editors" value="<%= context.getEngine().getEditorManager().getEditorList() %>" />
    <c:if test='${fn:length(editors) > 1}'>
    <div class="btn-group config">
      <%-- note: 'dropdown-toggle' is only here to style the last button properly! --%>
      <button class="btn btn-default"><span class="icon-pencil"></span><span class="caret"></span></button>
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

      <div class="dialog float find">
        <div class="caption"><fmt:message key='editor.plain.find'/> &amp; <fmt:message key='editor.plain.replace'/> </div>
        <div class="body">
        <a class="close"">&times;</a>
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
            <fmt:message key='editor.plain.find.submit' />
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
      </div>


  <%-- is PREVIEW functionality still needed - with livepreview ?
  <input class="btn btn-primary" type="submit" name="preview" accesskey="v"
         value="<fmt:message key='editor.plain.preview.submit'/>"
         title="<fmt:message key='editor.plain.preview.title'/>" />
  --%>
  <input class="btn btn-danger pull-right" type="submit" name="cancel" accesskey="q"
         value="<fmt:message key='editor.plain.cancel.submit'/>"
         title="<fmt:message key='editor.plain.cancel.title'/>" />

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

    <div class="row edit-area"><%-- .livepreview  .previewcolumn--%>
      <div class="col-50">
        <textarea class="editor form-control" id="editorarea" name="<%=EditorManager.REQ_EDITEDTEXT%>"
              autofocus="autofocus"
                   rows="20" cols="80"><%= TextUtil.replaceEntities(usertext) %></textarea>
      </div>
      <div class="ajaxpreview col-50"></div>
    </div>

    <div class="resizer"
    data-pref="editorHeight"
         title="<fmt:message key='editor.plain.edit.resize'/>"></div>

  </div><%-- end of .snipe --%>

</form>

<%-- </div>   ??CHECK: needed of IEx--%>