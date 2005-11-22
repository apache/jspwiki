<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="com.ecyrd.jspwiki.editor.EditorManager" %>

<%-- Inserts page content for preview. --%>

   <div class="previewnote">
      <b>This is a PREVIEW!  Hit "Keep Editing" to go back to the editor,
      or hit "Save" if you're happy with what you see.</b>
   </div>

   <p><hr /></p>

   <div class="previewcontent">
      <wiki:Translate><%=EditorManager.getEditedText(pageContext)%></wiki:Translate>
   </div>

   <br clear="all" />

   <p><hr /></p>

   <div class="previewnote">
      <b>This is a PREVIEW!  Hit "Keep Editing" to go back to the editor,
      or hit "Save" if you're happy with what you see.</b>
   </div>

   <p><hr /></p>

   <wiki:Editor/>