<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<s:useActionBean beanclass="org.apache.wiki.action.ViewActionBean" event="info" executeResolution="true" id="wikiActionBean" />
<s:layout-render name="/templates/default/ViewLayout.jsp">

  <s:layout-component name="content">
    <wiki:NoSuchPage>
      <fmt:message key="common.nopage">
        <fmt:param><wiki:EditLink><fmt:message key="common.createit" /></wiki:EditLink></fmt:param>
      </fmt:message>
    </wiki:NoSuchPage>
    <wiki:PageExists>
      <wiki:PageType type="page">
        <jsp:include page="/templates/default/PageInfoTab.jsp" />
      </wiki:PageType>
      <wiki:PageType type="attachment">
        <jsp:include page="/templates/default/AttachmentInfoTab.jsp" />
      </wiki:PageType>
    </wiki:PageExists>
  </s:layout-component>
  
</s:layout-render>
