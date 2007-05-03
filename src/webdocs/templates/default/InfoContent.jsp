<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>

<%
  WikiContext wikiContext = WikiContext.findContext(pageContext);
  int attCount = wikiContext.getEngine().getAttachmentManager()
                            .listAttachments( wikiContext.getPage() ).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";
%>

<wiki:TabbedSection defaultTab='info' >

  <wiki:PageType type="page">

  <wiki:Tab id="pagecontent" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "view.tab")%>" accesskey="v">
    <wiki:Include page="PageTab.jsp"/>
  </wiki:Tab>

  <wiki:PageExists>

  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a">
    <wiki:Include page="AttachmentTab.jsp"/>
  </wiki:Tab>

  <wiki:Tab id="info" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab")%>" accesskey="i" >
    <wiki:Include page="InfoTab.jsp"/>
  </wiki:Tab>

  </wiki:PageExists>

  </wiki:PageType>

  <wiki:PageType type="attachment">
    <wiki:Tab id="pageinfo" title="Attachment Info" >
      <wiki:Include page="InfoTab.jsp"/>
    </wiki:Tab>
  </wiki:PageType>

</wiki:TabbedSection>