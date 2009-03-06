<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="org.apache.wiki.action.GroupActionBean" event="view" executeResolution="true" id="wikiActionBean" />
<stripes:layout-render name="/templates/default/DefaultLayout.jsp">
  <stripes:layout-component name="content">
    <jsp:include page="/templates/default/GroupContent.jsp" />
  </stripes:layout-component>
</stripes:layout-render>
