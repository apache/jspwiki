<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<%@ page import="org.apache.wiki.attachment.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<s:useActionBean beanclass="org.apache.wiki.action.ViewActionBean" event="attachments" executeResolution="true" id="wikiActionBean" />
<s:layout-render name="/templates/default/DefaultLayout.jsp">

  <s:layout-component name="content">
    <wiki:NoSuchPage>
      <fmt:message key="common.nopage">
        <fmt:param><wiki:EditLink><fmt:message key="common.createit" /></wiki:EditLink></fmt:param>
      </fmt:message>
    </wiki:NoSuchPage>
    <wiki:PageExists>
      <jsp:include page="/templates/default/AttachmentTab.jsp" />
    </wiki:PageExists>
  </s:layout-component>
  
</s:layout-render>
