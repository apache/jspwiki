<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%-- Inserts a string message. --%>

   <div class="error">
      <%=pageContext.getAttribute("message",PageContext.REQUEST_SCOPE)%>
   </div>

   <br clear="all" />
