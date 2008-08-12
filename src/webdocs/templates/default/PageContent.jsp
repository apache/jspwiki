<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.attachment.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext( pageContext );
  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";
%>

<wiki:TabbedSection defaultTab='${param.tab}' >

  <wiki:Tab id="pagecontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "view.tab")%>' accesskey="v">
    <wiki:Include page="PageTab.jsp"/>
    <wiki:PageType type="attachment">
      <div class="information">
	    <fmt:message key="info.backtoparentpage" >
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
    <wiki:Include page="AttachmentTab.jsp"/>
  </wiki:Tab>
  </wiki:PageType>
    
  <wiki:Tab id="info" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab")%>'
           url="<%=c.getURL(WikiContext.INFO, c.getPage().getName())%>"
           accesskey="i" >
  </wiki:Tab>
    
  </wiki:PageExists>

</wiki:TabbedSection>