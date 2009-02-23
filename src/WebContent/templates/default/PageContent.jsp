<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<%@ page import="org.apache.wiki.attachment.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%
  WikiContext c = WikiContextFactory.findContext( pageContext );
  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";
%>
<wiki:TabbedSection defaultTab="pagecontent">

  <wiki:Tab id="pagecontent" titleKey="view.tab" accesskey="v">
    <jsp:include page="/templates/default/PageTab.jsp" />
    <wiki:PageType type="attachment">
      <div class="information">
	    <fmt:message key="info.backtoparentpage">
	      <fmt:param><wiki:LinkToParent><wiki:ParentPageName/></wiki:LinkToParent></fmt:param>
        </fmt:message>
      </div>
      <div style="overflow:hidden;">
        <wiki:Translate>[${wikiActionBean.page.name}]</wiki:Translate>
      </div>
    </wiki:PageType>    
  </wiki:Tab>

  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a" url="Attachments.jsp?page=${wikiActionBean.page.name}" />

  <wiki:Tab id="info" titleKey="info.tab" url="PageInfo.jsp?page=${wikiActionBean.page.name}" accesskey="i" />

</wiki:TabbedSection>
