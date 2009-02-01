<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.attachment.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<%
  WikiContext c = WikiContextFactory.findContext( pageContext );
  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";
%>

<wiki:TabbedSection defaultTab='${param.tab}'>

  <wiki:Tab id="pagecontent" titleKey="view.tab" accesskey="v">
    <wiki:Include page="PageTab.jsp" />
    <wiki:PageType type="attachment">
      <div class="information">
	    <fmt:message key="info.backtoparentpage">
	      <fmt:param><wiki:LinkToParent><wiki:ParentPageName/></wiki:LinkToParent></fmt:param>
        </fmt:message>
      </div>
      <div style="overflow:hidden;">
        <wiki:Translate>[<%= c.getPage().getName()%>]</wiki:Translate>
      </div>
    </wiki:PageType>    
  </wiki:Tab>

  <wiki:PageExists>

  <wiki:PageType type="page">
  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a">
    <wiki:Include page="AttachmentTab.jsp" />
  </wiki:Tab>
  </wiki:PageType>
    
  <wiki:Tab id="info" titleKey="info.tab"
           url="<%=c.getURL(WikiContext.INFO, c.getPage().getName())%>"
           accesskey="i" >
  </wiki:Tab>
    
  </wiki:PageExists>

</wiki:TabbedSection>