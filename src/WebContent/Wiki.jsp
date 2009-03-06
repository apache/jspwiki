<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<s:useActionBean beanclass="org.apache.wiki.action.ViewActionBean" event="view" executeResolution="true" id="wikiActionBean" />
<s:layout-render name="/templates/default/DefaultLayout.jsp">

  <%-- If wiki page is current, allow search engines to spider it --%>
  <wiki:CheckVersion mode="latest">
    <s:layout-component name="head.meta.robots">
      <meta name="robots" content="index,follow" />
    </s:layout-component>
  </wiki:CheckVersion>
    
  <s:layout-component name="content">
    <wiki:NoSuchPage>
      <fmt:message key="common.nopage">
        <fmt:param><wiki:EditLink><fmt:message key="common.createit" /></wiki:EditLink></fmt:param>
      </fmt:message>
    </wiki:NoSuchPage>
    <wiki:PageExists>
      <jsp:include page="/templates/default/PageContent.jsp" />
    </wiki:PageExists>
  </s:layout-component>
  
</s:layout-render>
