<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<s:useActionBean beanclass="org.apache.wiki.action.EditActionBean" event="edit" executeResolution="true" id="wikiActionBean" />
<s:layout-render name="/templates/default/DefaultLayout.jsp">

  <%-- Page title should say Edit: + pagename --%>
  <s:layout-component name="head.title">
    <fmt:message key="edit.title.edit">
      <fmt:param><wiki:Variable var="ApplicationName" /></fmt:param>
      <fmt:param><wiki:PageName/></fmt:param>
    </fmt:message>
  </s:layout-component>

  <!-- Add Javascript for editors -->
  <s:layout-component name="script">
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-edit.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/posteditor.js' />"></script>
  </s:layout-component>

  <s:layout-component name="content">
    <jsp:include page="/templates/default/EditContent.jsp" />
  </s:layout-component>
  
</s:layout-render>
