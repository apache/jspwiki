<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="org.apache.wiki.action.UserPreferencesActionBean" event="prefs" id="wikiActionBean" />
<stripes:layout-render name="/templates/default/ViewLayout.jsp">
  <stripes:layout-component name="content">
    <jsp:include page="/templates/default/PreferencesContent.jsp" />
  </stripes:layout-component>
</stripes:layout-render>
