<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.ViewActionBean" />
<stripes:layout-render name="/WEB-INF/jsp/templates/default/ViewTemplate.jsp">
<stripes:layout-component name="contents">
  <wiki:Include page="/WEB-INF/jsp/templates/default/PageContent.jsp" />
</stripes:layout-component>
</stripes:layout-render>
