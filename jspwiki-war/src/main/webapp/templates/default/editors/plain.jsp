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
<c:set var='context'><wiki:Variable var='requestcontext' /></c:set>
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
  }
%>
</wiki:CheckRequestContext>
<% if( usertext == null ) usertext = "";  %>

<form method="post" accept-charset="<wiki:ContentEncoding/>"
      action="<wiki:Link context='${context}' format='url'/>"
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

    <div class="localstorage modal">
      <div class="modal-footer">
        <button class="btn btn-success"><fmt:message key="editor.plain.localstorage.restore"/></button>
        <button class="btn btn-danger"><fmt:message key="editor.plain.localstorage.delete"/></button>
      </div>
    </div>

    <div class="form-inline form-group sticky">

    <div class="form-inline form-group dropdown">
    <button class="btn btn-success" type="submit" name="ok" accesskey="s">
      <fmt:message key='editor.plain.save.submit${ context == "edit" ? "" : ".comment" }'/>
      <span class="caret"></span>
    </button>
    <ul class="dropdown-menu" data-hover-parent="div">
      <li class="dropdown-header">
        <input class="form-control" type="text" name="changenote" id="changenote" size="80" maxlength="80"
             placeholder="<fmt:message key='editor.plain.changenote'/>"
             value="${changenote}" />
      </li>
      <wiki:CheckRequestContext context="comment">
      <li class="divider" />
      <li class="dropdown-header">
        <fmt:message key="editor.commentsignature"/>
      </li>
      <li class="dropdown-header">
        <input class="form-control" type="text" name="author" id="authorname"  size="80" maxlength="80"
             placeholder="<fmt:message key='editor.plain.name'/>"
             value="${author}" />
      </li>
      <li  class="dropdown-header">
        <label class="btn btn-default btn-xs" for="rememberme">
          <input type="checkbox" name="remember" id="rememberme" ${ remember ? "checked='checked'" : "" } />
          <fmt:message key="editor.plain.remember"/>
        </label>
      </li>
      <li  class="dropdown-header">
        <input class="form-control" type="text" name="link" id="link" size="80" maxlength="80"
               placeholder="<fmt:message key='editor.plain.email'/>"
               value="${link}" />
      </li>
      </wiki:CheckRequestContext>
    </ul>
    </div>

  <div class="btn-group editor-tools">

    <wiki:CheckRequestContext context="edit">
    <div class="btn-group sections">
      <button class="btn btn-default" type="button"><span class="icon-bookmark"></span><span class="caret"></span></button>
      <ul class="dropdown-menu" data-hover-parent="div">
            <li><a>first</a></li>
            <li><a>..</a></li>
            <li><a class="dropdown-divider">..</a></li>
            <li><a>..</a></li>
      </ul>
    </div>
    </wiki:CheckRequestContext>

    <button class="btn btn-default" type="button" data-cmd="lipstick"><span class="icon-tint" /></button>
    <button class="btn btn-default" type="button" data-cmd="find"><span class="icon-search" /></button>


    <fmt:message key='editor.plain.undo.title' var='msg'/>
    <button class="btn btn-default" type="button" data-cmd="undo" title="${msg}"><span class="icon-undo"></span></button>
    <fmt:message key='editor.plain.redo.title' var='msg'/>
    <button class="btn btn-default" type="button" data-cmd="redo" title="${msg}"><span class="icon-repeat"></span></button>

    <div class="btn-group config">
      <%-- note: 'dropdown-toggle' is only here to style the last button properly! --%>
      <button class="btn btn-default" type="button"><span class="icon-wrench"></span><span class="caret"></span></button>
      <ul class="dropdown-menu" data-hover-parent="div">

            <li>
        <a class="slimbox-link" href="<wiki:Link format='url' page='EditPageHelp' ><wiki:Param name='skin' value='reader'/></wiki:Link>">
          <fmt:message key="edit.tab.help" />
        </a>
    <%--
      <wiki:NoSuchPage page="EditPageHelp">
        <div class="error">
        <fmt:message key="comment.edithelpmissing">
        <fmt:param><wiki:EditLink page="EditPageHelp">EditPageHelp</wiki:EditLink></fmt:param>
        </fmt:message>
        </div>
      </wiki:NoSuchPage>
    --%>
      </li>
      <li class="divider"></li>

            <li>
              <a>
                <label for="autosuggest">
                  <input type="checkbox" data-cmd="autosuggest" id="autosuggest" ${prefs.autosuggest ? 'checked="checked"' : ''}/>
                  <fmt:message key='editor.plain.autosuggest'/>
                </label>
              </a>
            </li>
            <li>
              <a>
                <label for="tabcompletion">
                  <input type="checkbox" data-cmd="tabcompletion" id="tabcompletion" ${prefs.tabcompletion ? 'checked="checked"' : ''}/>
                  <fmt:message key='editor.plain.tabcompletion'/>
                </label>
              </a>
            </li>
            <li>
              <a>
                <label for="smartpairs">
                  <input type="checkbox" data-cmd="smartpairs" id="smartpairs" ${prefs.smartpairs ? 'checked="checked"' : ''}/>
                  <fmt:message key='editor.plain.smartpairs'/>
                </label>
              </a>
            </li>
            <li class="divider"></li>
            <li>
              <a>
                <label for="livepreview">
                  <input type="checkbox" data-cmd="livepreview" id="livepreview" ${prefs.livepreview ? 'checked="checked"' : ''}/>
                  <fmt:message key='editor.plain.livepreview'/> <span class="icon-refresh"/>
                </label>
              </a>
            </li>
            <li>
              <a>
                <label for="previewcolumn">
                  <input type="checkbox" data-cmd="previewcolumn" id="previewcolumn" ${prefs.previewcolumn ? 'checked="checked"' : ''}/>
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
      <button class="btn btn-default" type="button"><span class="icon-pencil"></span><span class="caret"></span></button>
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

<%--
    <div class="dialog selection dialog-horizontal formatting">
      <div class="body">
        <ul>
          <li class="item" data-cmd="bold" title="<fmt:message key='editor.plain.tbB.title' />"><b>bold</b></li>
          <li class="item" title="''{italic}''"><i>italic</i></li>
          <li class="item" title="{{{monospaced}}}"><tt>mono</tt></li>

          <li class="item" title="{{{{code}}}}"><span style="font-family:monospace;">pre</span></li>
          <li class="item" title="[description|{pagename or url}|options]"><span class="icon-link"></span></li>
          <li class="item" title="[{Image src='{image.jpg}'}]"><span class="icon-picture"></span></li>
          <li class="item" title="[{Image src='{image.jpg}'}]"><span class="icon-puzzle-piece"></span></li>

        <%--
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
        -- % >
        </ul>
      </div>
    </div>
--%>

    <div class="dialog float find">
      <div class="caption"><fmt:message key='editor.plain.find'/> &amp; <fmt:message key='editor.plain.replace'/> </div>
      <div class="body">
        <a class="close">&times;</a>
        <textarea class="form-control form-group" name="tbTEXTSEL" disabled rows="4" >TUUT</textarea>
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
      <div>
        <textarea class="editor form-control snipeable"
           <wiki:CheckRequestContext context="edit">placeholder="<fmt:message key='editor.plain.create'/>"</wiki:CheckRequestContext>
           <wiki:CheckRequestContext context="comment">placeholder="<fmt:message key='editor.plain.comment'/>"</wiki:CheckRequestContext>
                  autofocus="autofocus"
                  rows="20" cols="80"></textarea>
        <textarea class="editor form-control hidden" id="editorarea" name="<%=EditorManager.REQ_EDITEDTEXT%>"
                  rows="20" cols="80"><%= TextUtil.replaceEntities(usertext) %></textarea>
      </div>
      <div class="ajaxpreview empty"></div>
    </div>
    <div class="resizer" data-resize=".ajaxpreview,.snipeable" data-pref="editorHeight"
         title="<fmt:message key='editor.plain.edit.resize'/>"></div>

  </div><%-- end of .snipe --%>

</form>