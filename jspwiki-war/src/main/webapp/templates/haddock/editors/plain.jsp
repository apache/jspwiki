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
   		context.getURL( WikiContext.NONE, "scripts/jspwiki-edit.js" ) );
   /*merged by wro4j into one jspwiki-edit.js
   TemplateManager.addResourceRequest( context, TemplateManager.RESOURCE_SCRIPT,
   		context.getURL( WikiContext.NONE, "scripts/posteditor.js" ) );
   */
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

<div style="width:100%"> <%-- Required for IE6 on Windows --%>

<form action="<wiki:CheckRequestContext
     context='edit'><wiki:EditLink format='url'/></wiki:CheckRequestContext><wiki:CheckRequestContext
     context='comment'><wiki:CommentLink format='url'/></wiki:CheckRequestContext>"
       class=""
          id="editform"
    onsubmit="return Wiki.submitOnce(this);"
      method="post" accept-charset="<wiki:ContentEncoding/>"
     enctype="application/x-www-form-urlencoded" >

  <%-- Edit.jsp relies on these being found.  So be careful, if you make changes. --%>
  <div class="form-group form-inline">
  <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
  <input type="hidden" name="action" value="save" />
  <%=SpamFilter.insertInputFields( pageContext )%>
  <input type="hidden" name="<%=SpamFilter.getHashFieldName(request)%>" value="<c:out value='${lastchange}' />" />
  
  <input class="btn btn-primary" type="submit" name="ok" value="<fmt:message key='editor.plain.save.submit'/>"
    accesskey="s"
        title="<fmt:message key='editor.plain.save.title'/>" />
  <input class="btn btn-primary" type="submit" name="preview" value="<fmt:message key='editor.plain.preview.submit'/>"
    accesskey="v"
        title="<fmt:message key='editor.plain.preview.title'/>" />
  <input class="btn btn-danger pull-right" type="submit" name="cancel" value="<fmt:message key='editor.plain.cancel.submit'/>"
    accesskey="q"
        title="<fmt:message key='editor.plain.cancel.title'/>" />

  <%-- This following field is only for the SpamFilter to catch bots which are just randomly filling all fields and submitting.
       Normal user should never see this field, nor type anything in it. --%>
  <input class="hidden" type="text" name="<%=SpamFilter.getBotFieldName()%>" id="<%=SpamFilter.getBotFieldName()%>" value="" />
  <%--TODO
    <wiki:Permission permission="rename">
    <tr>
    <td><label for="renameto"><fmt:message key='editor.renameto'/></label></td>
    <td><input type="text" name="renameto" value="<wiki:Variable var='pagename' />" size="40" />
    &nbsp;&nbsp;
    <input type="checkbox" name="references" checked="checked" />
    <fmt:message key="info.updatereferrers"/>
    FIXME</td>
    </tr>
    </wiki:Permission>
  --%>

  <wiki:CheckRequestContext context="edit">
  <input type="text" size="80" maxlength="80" 
      class="form-control form-col-50"
      name="changenote" id="changenote" 
      placeholder="<fmt:message key='editor.plain.changenote'/>"
      value="${changenote}"/>
  </wiki:CheckRequestContext>

  <wiki:CheckRequestContext context="comment">
	<label><fmt:message key="editor.commentsignature"/></label>

    <input class="form-control form-col-20" type="text" name="author" id="authorname" 
        placeholder="<fmt:message key='editor.plain.name'/>"
        value="${author}" />
        
    <label for="rememberme">
        <input type="checkbox" name="remember" id="rememberme" <%=TextUtil.isPositive((String)session.getAttribute("remember")) ? "checked='checked'" : ""%> />
        <fmt:message key="editor.plain.remember"/>
    </label>

    <input class="form-control form-col-20" type="text" name="link" id="link" size="24" 
        placeholder="<fmt:message key='editor.plain.email'/>"
        value="${link}" />

  </wiki:CheckRequestContext>

  </div>  
  

  <div id="tools" class="warning">
      <%--<h4><fmt:message key='editor.plain.toolbar'/></h4>--%>
      <div id="toolbuttons" class="form-group">
      <span>
	  <a href="#" class="tool" rel="" id="tbLink" title="<fmt:message key='editor.plain.tbLink.title'/>">link</a>
	  <a href="#" class="tool" rel="break" id="tbH1" title="<fmt:message key='editor.plain.tbH1.title'/>">h1</a>
	  <a href="#" class="tool" rel="break" id="tbH2" title="<fmt:message key='editor.plain.tbH2.title'/>">h2</a>
	  <a href="#" class="tool" rel="break" id="tbH3" title="<fmt:message key='editor.plain.tbH3.title'/>">h3</a>
      </span>
      <span>
	  <a href="#" class="tool" rel="" id="tbB" title="<fmt:message key='editor.plain.tbB.title'/>">bold</a>
	  <a href="#" class="tool" rel="" id="tbI" title="<fmt:message key='editor.plain.tbI.title'/>">italic</a>
	  <a href="#" class="tool" rel="" id="tbMONO" title="<fmt:message key='editor.plain.tbMONO.title'/>">mono</a>
	  <a href="#" class="tool" rel="" id="tbSUP" title="<fmt:message key='editor.plain.tbSUP.title'/>">sup</a>
	  <a href="#" class="tool" rel="" id="tbSUB" title="<fmt:message key='editor.plain.tbSUB.title'/>">sub</a>
	  <a href="#" class="tool" rel="" id="tbSTRIKE" title="<fmt:message key='editor.plain.tbSTRIKE.title'/>">strike</a>
      </span>
      <span>
	  <a href="#" class="tool" rel="" id="tbBR" title="<fmt:message key='editor.plain.tbBR.title'/>">br</a>
	  <a href="#" class="tool" rel="break" id="tbHR" title="<fmt:message key='editor.plain.tbHR.title'/>">hr</a>
	  <a href="#" class="tool" rel="break" id="tbPRE" title="<fmt:message key='editor.plain.tbPRE.title'/>">pre</a>
	  <a href="#" class="tool" rel="break" id="tbCODE" title="<fmt:message key='editor.plain.tbCODE.title'/>">code</a>
	  <a href="#" class="tool" rel="break" id="tbDL" title="<fmt:message key='editor.plain.tbDL.title'/>">dl</a>
      </span>
      <span>
	  <a href="#" class="tool" rel="break" id="tbTOC" title="<fmt:message key='editor.plain.tbTOC.title'/>">toc</a>
	  <a href="#" class="tool" rel="break" id="tbTAB" title="<fmt:message key='editor.plain.tbTAB.title'/>">tab</a>
	  <a href="#" class="tool" rel="break" id="tbTABLE" title="<fmt:message key='editor.plain.tbTABLE.title'/>">table</a>
	  <a href="#" class="tool" rel="" id="tbIMG" title="<fmt:message key='editor.plain.tbIMG.title'/>">img</a>
	  <a href="#" class="tool" rel="break" id="tbQUOTE" title="<fmt:message key='editor.plain.tbQUOTE.title'/>">quote</a>
	  <a href="#" class="tool" rel="break" id="tbSIGN" title="<fmt:message key='editor.plain.tbSIGN.title'/>">sign</a>
      </span>
      <span>
      <a href="#" class="tool" rel="break" id="tbUNDO" title="<fmt:message key='editor.plain.undo.title'/>"><fmt:message key='editor.plain.undo.submit'/></a>
      </span>
      <span>
	  <a href="#" class="tool" rel="break" id="tbREDO" title="<fmt:message key='editor.plain.redo.title'/>"><fmt:message key='editor.plain.redo.submit'/></a>
      </span>
	  </div>

	  <div id="toolextra" class="form-group hidden" >
      <span>
      <input type="checkbox" name="tabcompletion" id="tabcompletion" <%=TextUtil.isPositive((String)session.getAttribute("tabcompletion")) ? "checked='checked'" : ""%>/>
      <label for="tabcompletion" title="<fmt:message key='editor.plain.tabcompletion.title'/>"><fmt:message key="editor.plain.tabcompletion"/></label>
      </span>
      <span>
      <input type="checkbox" name="smartpairs" id="smartpairs" <%=TextUtil.isPositive((String)session.getAttribute("smartpairs")) ? "checked='checked'" : ""%>/>
      <label for="smartpairs" title="<fmt:message key='editor.plain.smartpairs.title'/>"><fmt:message key="editor.plain.smartpairs"/></label>
      </span>
	  </div>

	  <div class="form-inline form-group" style="margin-bottom:0;" id="searchbar">
  		
        <%--<label for="tbFIND" ><fmt:message key="editor.plain.find"/></label>--%>
  		<input class="form-control form-col-25" type="text"   name="tbFIND" id="tbFIND" size="16" placeholder="<fmt:message key='editor.plain.find'/>"/>
		<%--<label for="tbREPLACE" ><fmt:message key="editor.plain.replace"/></label>--%>
		<input class="form-control form-col-25" type="text"   name="tbREPLACE" id="tbREPLACE" size="16" placeholder="<fmt:message key='editor.plain.replace'/>"/>
        <input class="btn btn-primary" type="button" name="doreplace" id="doreplace" value="<fmt:message key='editor.plain.find.submit' />" />
        
        <div class="btn-group">
  		<label class="btn btn-default btn-xs" for="tbMatchCASE">
  		  <input type="checkbox" name="tbMatchCASE" id="tbMatchCASE" />
  		  <fmt:message key="editor.plain.matchcase"/>
  		</label>
  		<label class="btn btn-default btn-xs" for="tbREGEXP" >
  		  <input type="checkbox" name="tbREGEXP" id="tbREGEXP" />
  		  <fmt:message key="editor.plain.regexp"/>
  		</label>
  		<label  class="btn btn-default btn-xs" for="tbGLOBAL">
  		  <input type="checkbox" name="tbGLOBAL" id="tbGLOBAL" checked="checked" />
  		  <fmt:message key="editor.plain.global"/>
  		</label>
  		</div>
	  </div>
	  
  </div>

  <textarea class="editor form-group form-control" id="editorarea" name="<%=EditorManager.REQ_EDITEDTEXT%>"         
            autofocus="autofocus"
            rows="20" cols="80"><%=TextUtil.replaceEntities(usertext)%></textarea>

</form>

<div class="form-control btn btn-primary" id="sneakpreviewheader">
  <input type="checkbox" name="autopreview" id="autopreview" <%=TextUtil.isPositive((String)session.getAttribute("autopreview")) ? "checked='checked'" : ""%> />
  <label for="autopreview" title="<fmt:message key='editor.plain.sneakpreview.title'/>"><fmt:message key="editor.plain.sneakpreview"/></label>
  <span id="previewSpin" class="spin" style="position:absolute;display:none;"></span>
</div>

<div id="sneakpreview" class="preview-content"></div>

</div>