<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.ui.EditorManager" %>

<%-- Inserts page content for preview. --%>

<div class="information">
  This is a <strong>preview</strong>!  Hit "Keep Editing" to go back to the editor,
  or hit "Save" if you're happy with what you see.
</div>

<div class="previewcontent">
   <wiki:Translate><%=EditorManager.getEditedText(pageContext)%></wiki:Translate>
</div>

<div class="information">
  This is a <strong>preview</strong>!  Hit "Keep Editing" to go back to the editor,
  or hit "Save" if you're happy with what you see.
</div>

<wiki:Editor/>