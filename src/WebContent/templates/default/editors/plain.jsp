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
    id="editform" method="post" acceptcharset="UTF-8" enctype="application/x-www-form-urlencoded" >
    
    <%-- If any conflicts, print the conflicting text here --%>
    <c:if test="${not empty wikiActionBean.conflictText}">
      <p>
        <s:label for="conflictText" />
        <s:textarea name="conflictText" readonly="true" />
      </p>
    </c:if>
  
    <%-- EditActionBean relies on these being found.  So be careful, if you make changes. --%>
    <p id="submitbuttons">
      <s:hidden name="page"><wiki:Variable var='pagename' /></s:hidden>
      <s:hidden name="startTime" />
      <c:set var="saveTitle" scope="page"><fmt:message key="editor.plain.save.title" /></c:set>
      <wiki:CheckRequestContext context='edit'>
        <s:submit name="save" accesskey="s" title="${saveTitle}" />
      </wiki:CheckRequestContext>
      <wiki:CheckRequestContext context='comment'>
        <s:submit name="comment" accesskey="s" title="${saveTitle}"><fmt:message key="editor.plain.save.submit"/></s:submit> 
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
          <s:text name="changenote" id="changenote" size="80" maxlength="80" />
        </td>
      </tr>
    </table>
    
    <%-- Toolbar --%>
    <div id="tools">
      <h4><fmt:message key='editor.plain.toolbar' /></h4>
      <div id="toolbuttons">
        <span>
      	  <a href="#" class="tool" rel="" id="tbLink" title="<fmt:message key='editor.plain.tbLink.title' />">link</a>
      	  <a href="#" class="tool" rel="break" id="tbH1" title="<fmt:message key='editor.plain.tbH1.title' />">h1</a>
      	  <a href="#" class="tool" rel="break" id="tbH2" title="<fmt:message key='editor.plain.tbH2.title' />">h2</a>
      	  <a href="#" class="tool" rel="break" id="tbH3" title="<fmt:message key='editor.plain.tbH3.title' />">h3</a>
        </span>
        <span>
      	  <a href="#" class="tool" rel="" id="tbB" title="<fmt:message key='editor.plain.tbB.title' />">bold</a>
      	  <a href="#" class="tool" rel="" id="tbI" title="<fmt:message key='editor.plain.tbI.title' />">italic</a>
      	  <a href="#" class="tool" rel="" id="tbMONO" title="<fmt:message key='editor.plain.tbMONO.title' />">mono</a>
      	  <a href="#" class="tool" rel="" id="tbSUP" title="<fmt:message key='editor.plain.tbSUP.title' />">sup</a>
      	  <a href="#" class="tool" rel="" id="tbSUB" title="<fmt:message key='editor.plain.tbSUB.title' />">sub</a>
      	  <a href="#" class="tool" rel="" id="tbSTRIKE" title="<fmt:message key='editor.plain.tbSTRIKE.title' />">strike</a>
        </span>
        <span>
      	  <a href="#" class="tool" rel="" id="tbBR" title="<fmt:message key='editor.plain.tbBR.title' />">br</a>
      	  <a href="#" class="tool" rel="break" id="tbHR" title="<fmt:message key='editor.plain.tbHR.title' />">hr</a>
      	  <a href="#" class="tool" rel="break" id="tbPRE" title="<fmt:message key='editor.plain.tbPRE.title' />">pre</a>
      	  <a href="#" class="tool" rel="break" id="tbCODE" title="<fmt:message key='editor.plain.tbCODE.title' />">code</a>
      	  <a href="#" class="tool" rel="break" id="tbDL" title="<fmt:message key='editor.plain.tbDL.title' />">dl</a>
        </span>
        <span>
      	  <a href="#" class="tool" rel="break" id="tbTOC" title="<fmt:message key='editor.plain.tbTOC.title' />">toc</a>
      	  <a href="#" class="tool" rel="break" id="tbTAB" title="<fmt:message key='editor.plain.tbTAB.title' />">tab</a>
      	  <a href="#" class="tool" rel="break" id="tbTABLE" title="<fmt:message key='editor.plain.tbTABLE.title' />">table</a>
      	  <a href="#" class="tool" rel="" id="tbIMG" title="<fmt:message key='editor.plain.tbIMG.title' />">img</a>
      	  <a href="#" class="tool" rel="break" id="tbQUOTE" title="<fmt:message key='editor.plain.tbQUOTE.title' />">quote</a>
      	  <a href="#" class="tool" rel="break" id="tbSIGN" title="<fmt:message key='editor.plain.tbSIGN.title' />">sign</a>
        </span>
        <span>
          <a href="#" class="tool" rel="break" id="tbUNDO" title="<fmt:message key='editor.plain.undo.title' />"><fmt:message key='editor.plain.undo.submit' /></a>
        </span>
        <span>
      	  <a href="#" class="tool" rel="break" id="tbREDO" title="<fmt:message key='editor.plain.redo.title' />"><fmt:message key='editor.plain.redo.submit' /></a>
        </span>
  	  </div>
  
      <%-- Toolbar extras --%>
  	  <div id="toolextra" class="clearbox" style="display:none;">
        <span>
          <input type="checkbox" name="tabcompletion" id="tabcompletion" <%=TextUtil.isPositive((String)session.getAttribute("tabcompletion")) ? "checked='checked'" : ""%> />
          <label for="tabcompletion" title="<fmt:message key='editor.plain.tabcompletion.title' />"><fmt:message key="editor.plain.tabcompletion" /></label>
        </span>
        <span>
          <input type="checkbox" name="smartpairs" id="smartpairs" <%=TextUtil.isPositive((String)session.getAttribute("smartpairs")) ? "checked='checked'" : ""%> />
          <label for="smartpairs" title="<fmt:message key='editor.plain.smartpairs.title' />"><fmt:message key="editor.plain.smartpairs" /></label>	  
        </span>
  	  </div>
  
      <%-- Search bar --%>
  	  <div id="searchbar">
    		<span>
          <label for="tbFIND"><fmt:message key="editor.plain.find" /></label>
          <input type="text" name="tbFIND" id="tbFIND" size="16" />
          <label for="tbREPLACE"><fmt:message key="editor.plain.replace" /></label>
          <input type="text" name="tbREPLACE" id="tbREPLACE" size="16" />
          <input type="button" name="doreplace" id="doreplace" value="<fmt:message key='editor.plain.find.submit' />" />
        </span>
    		<span>
          <input type="checkbox" name="tbMatchCASE" id="tbMatchCASE" />
          <label for="tbMatchCASE"><fmt:message key="editor.plain.matchcase" /></label>
    		</span>
    		<span>
          <input type="checkbox" name="tbREGEXP" id="tbREGEXP" />
          <label for="tbREGEXP"><fmt:message key="editor.plain.regexp" /></label>
    		</span>
    		<span>
          <input type="checkbox" name="tbGLOBAL" id="tbGLOBAL" checked="checked" />
          <label for="tbGLOBAL"><fmt:message key="editor.plain.global" /></label>
    		</span>
  	  </div>
  	  <div class="clearbox"></div>
    </div>
  
    <%-- You knew this would be here somewhere. Yes, it's the textarea where the user
         actually edits stuff. --%>
    <div>
      <s:textarea id="editorarea" name="text" class="editor" rows="20" cols="80" />
      <div class="clearbox" ></div>
    </div>
  
    <wiki:CheckRequestContext context="comment">
      <fieldset>
      	<legend><fmt:message key="editor.commentsignature" /></legend>
        <p>
          <s:label for="author" accesskey="n" name="author" />
          <s:text id="author" name="author" />
          <s:checkbox id="remember" name="remember" />
          <s:label for="remember" name="remember" />
        </p>
        <p>
          <s:label for="link" accesskey="m" name="editor.plain.email" />
          <s:text id="link" name="link" size="24" />
        </p>
      </fieldset>
    </wiki:CheckRequestContext>
  
    <div id="livepreviewheader">
      <s:checkbox id="livePreview" name="livePreview" />
      <c:set var="livePreviewTitle" scope="page"><fmt:message key="editor.plain.livepreview.title"/></c:set>
      <s:label for="livePreview" title="${livePreviewTitle}" name="livePreview" />	  
      <span id="previewSpin" class="spin" style="position:absolute;display:none;"></span>
    </div>
    <div id="livepreview"></div>

    <%-- Spam detection fields --%>
    <wiki:SpamProtect />
  </s:form>
  
</div>