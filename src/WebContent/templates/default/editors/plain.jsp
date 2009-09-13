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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%--
        This is a plain editor for JSPWiki.
--%>
<div style="width:100%"> <%-- Required for IE6 on Windows --%>
  
  <%-- Print any validation errors --%>
  <s:errors />
  
  <s:form beanclass="org.apache.wiki.action.EditActionBean" class="wikiform"
    id="editform" method="post" acceptcharset="UTF-8" enctype="application/x-www-form-urlencoded" 
    onsubmit="window.onbeforeunload = null;">
    
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
    
    <%-- Toolbar --%>
    <div id="toolbar" class="line">

    <div id="configDialog" style="display:none;">
      <label for="tabcompletion" title="<fmt:message key='editor.plain.tabcompletion.title'/>">
      <input type="checkbox" name="tabcompletion" id="tabcompletion" /><fmt:message key="editor.plain.tabcompletion"/></label>
      <br />
      <label for="smartpairs" title="<fmt:message key='editor.plain.smartpairs.title'/>">
      <input type="checkbox" name="smartpairs" id="smartpairs" /><fmt:message key="editor.plain.smartpairs"/></label>	  
    </div>

    <fieldset class="unit">
    <legend><fmt:message key='editor.plain.action'/></legend>
    <a href="#" class="tool tUNDO" title="<fmt:message key='editor.plain.undo.title'/>">undo</a>
    <a href="#" class="tool tREDO" title="<fmt:message key='editor.plain.redo.title'/>">redo</a>
    <a href="#" class="tool tSEARCH" title="<fmt:message key='editor.plain.find.title'/>">find</a>
    <a href="#" class="tool tCONFIG" title="<fmt:message key='editor.plain.config.title'/>">config</a>
    </fieldset>

    <fieldset class="unit">
    <legend><fmt:message key='editor.plain.insert'/></legend>
    <a href="#" class="tool tLink" title="<fmt:message key='editor.plain.tbLink.title'/>">link</a>
    <a href="#" class="tool tH1" title="<fmt:message key='editor.plain.tbH1.title'/>">h1</a>
    <a href="#" class="tool tH2" title="<fmt:message key='editor.plain.tbH2.title'/>">h2</a>
    <a href="#" class="tool tH3" title="<fmt:message key='editor.plain.tbH3.title'/>">h3</a>
    <a href="#" class="tool tPRE" title="<fmt:message key='editor.plain.tbPRE.title'/>">pre</a>
    <a href="#" class="tool tHR" title="<fmt:message key='editor.plain.tbHR.title'/>">hr</a>
    <a href="#" class="tool tBR" title="<fmt:message key='editor.plain.tbBR.title'/>">br</a>
    <a href="#" class="tool tCHAR" title="<fmt:message key='editor.plain.tbCHAR.title'/>">special</a>
    </fieldset>

    <fieldset class="unit">
      <legend><fmt:message key='editor.plain.style'/></legend>
      <a href="#" class="tool tB" title="<fmt:message key='editor.plain.tbB.title'/>">bold</a>
      <a href="#" class="tool tI" title="<fmt:message key='editor.plain.tbI.title'/>">italic</a>
      <a href="#" class="tool tMONO" title="<fmt:message key='editor.plain.tbMONO.title'/>">mono</a>
      <a href="#" class="tool tCSS" title="<fmt:message key='editor.plain.tbCSS.title'/>">%%</a>
      <a href="#" class="tool tFONT" title="<fmt:message key='editor.plain.tbFONT.title'/>">font</a>
      <a href="#" class="tool tCOLOR" title="<fmt:message key='editor.plain.tbCOLOR.title'/>">color</a>
      <!--
      <a href="#" class="tool tSUP" title="<fmt:message key='editor.plain.tbSUP.title'/>">sup</a>
      <a href="#" class="tool tSUB" title="<fmt:message key='editor.plain.tbSUB.title'/>">sub</a>
      <a href="#" class="tool tSTRIKE" title="<fmt:message key='editor.plain.tbSTRIKE.title'/>">strike</a>
      -->
    </fieldset>

    <fieldset class="unit">
      <legend><fmt:message key='editor.plain.extra'/></legend>
      <a href="#" class="tool tACL" title="<fmt:message key='editor.plain.tbACL.title'/>">acl</a>
      <a href="#" class="tool tIMG" title="<fmt:message key='editor.plain.tbIMG.title'/>">img</a>
      <a href="#" class="tool tTABLE" title="<fmt:message key='editor.plain.tbTABLE.title'/>">table</a>
      <a href="#" class="tool tPLUGIN" title="<fmt:message key='editor.plain.tbPLUGIN.title'/>">plugin</a>
      <%-- 
      <a href="#" class="tool tTOC" title="<fmt:message key='editor.plain.tbTOC.title'/>">toc</a>  
      --%>
      <a href="#" class="tool tDL" title="<fmt:message key='editor.plain.tbDL.title'/>">dl</a>
      <a href="#" class="tool tCODE" title="<fmt:message key='editor.plain.tbCODE.title'/>">code</a>
      <a href="#" class="tool tTAB" title="<fmt:message key='editor.plain.tbTAB.title'/>">tab</a>
      <a href="#" class="tool tSIGN" title="<fmt:message key='editor.plain.tbSIGN.title'/>">sign</a>
      <%-- --%>
    </fieldset>

    <fieldset class="unit lastUnit">
      <legend><fmt:message key="editor.plain.livepreview"/></legend>

      <a href="#" class="tool tHORZ" title="<fmt:message key='editor.plain.tbHORZ.title'/>">tile-horz</a>
      <a href="#" class="tool tVERT" title="<fmt:message key='editor.plain.tbVERT.title'/>">tile-vert</a>
      <label for="autopreview" title="<fmt:message key='editor.plain.livepreview.title'/>">
      <input type="checkbox" name="autopreview" id="autopreview" <%=TextUtil.isPositive((String)session.getAttribute("autopreview")) ? "checked='checked'" : ""%> />On</label>

    </fieldset>

    <div class="clearbox">
      <p id="findDialog" style="display:none;">
        <%--<label for="tbFIND" ><fmt:message key="editor.plain.find"/>--%>
        <input type="text" name="tbFIND" id="tbFIND" size="16" value="find"/>
        <%--</label>--%>
        <label for="tbMatchCASE"><input type="checkbox" name="tbMatchCASE" id="tbMatchCASE" />
          <fmt:message key="editor.plain.matchcase"/></label>
      	<label for="tbREGEXP" ><input type="checkbox" name="tbREGEXP" id="tbREGEXP" />
      	  <fmt:message key="editor.plain.regexp"/></label>
  
        <%--<label for="tbREPLACE" ><fmt:message key="editor.plain.replace"/>--%>
          <input type="text" name="tbREPLACE" id="tbREPLACE" size="16" />
        <%--</label>--%>
        <a class="btn" id="doreplace"><span><span><fmt:message key='editor.plain.find.submit' /></span></span></a>
      	<label for="tbGLOBAL"><input type="checkbox" name="tbGLOBAL" id="tbGLOBAL" checked="checked" />
      	  <fmt:message key="editor.plain.global"/></label>
      </p>
    </div>

    </div><%-- end of the toolbar --%>

    <%-- You knew this would be here somewhere. Yes, it's the textarea where the user
         actually edits stuff. --%>
    <div id="editor-content" class="line" style="clear:both;">
  
      <div class="unit size1of2">
        <div class="editor-container">
        <%-- js-insert: <div id="snipetoc"><ul>...</ul></div> --%>
        <s:textarea id="editorarea" name="text" class="editor" rows="20" cols="80" />
        <%-- js insert: <div class="resize-bar"></div>  --%>        
        </div>  
      </div>

      <div class="unit size1of2 lastUnit">
        <div id="previewspin" class="spin" style="display:none;"><fmt:message key="common.ajax.loading"/></div>
        <div id="livepreview" class="xflow"></div>
	  </div>

    </div>

    <%-- Spam detection fields --%>
    <wiki:SpamProtect />
  </s:form>
  
</div>