<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%-- Inserts a string message. --%>

   <DIV class="messagecontent">
      <%=pageContext.getAttribute("message",PageContext.REQUEST_SCOPE)%>
   </DIV>

   <BR clear="all" />
