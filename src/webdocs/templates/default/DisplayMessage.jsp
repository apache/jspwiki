<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

<%-- Inserts a string message. --%>

   <div class="error">
      <%=TextUtil.replaceEntities(pageContext.getAttribute("message",PageContext.REQUEST_SCOPE))%>
   </div>

   <br clear="all" />
