<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%-- Inserts page content for preview. --%>

   <DIV class="previewnote">
      <B>This is a PREVIEW!  Hit "back" on your browser to go back to the editor.</B>
   </DIV>

   <P><HR></P>

   <DIV class="previewcontent">
      <wiki:Translate><%=pageContext.getAttribute("usertext",PageContext.REQUEST_SCOPE)%></wiki:Translate>
   </DIV>

   <P><HR></P>

   <DIV class="previewnote">
      <B>This is a PREVIEW!  Hit "back" on your browser to go back to the editor.</B>
   </DIV>
