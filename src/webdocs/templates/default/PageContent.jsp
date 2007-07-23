<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>

<%
  WikiContext c = WikiContext.findContext( pageContext );
  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";
%>

<wiki:TabbedSection defaultTab='${param.tab}' >

  <wiki:Tab id="pagecontent" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "view.tab")%>" accesskey="v">
    <wiki:Include page="PageTab.jsp"/>
  </wiki:Tab>

  <wiki:PageExists>

  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a">
    <wiki:Include page="AttachmentTab.jsp"/>
  </wiki:Tab>
    
  <wiki:Tab id="info" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab")%>'
           url="<%=c.getURL(WikiContext.INFO, c.getPage().getName())%>"
           accesskey="i" >
  </wiki:Tab>
    
    <wiki:Permission permission="edit">
      <wiki:PageType type="page">
        <wiki:Tab id="edit" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "actions.edit")%>'
           url="<%=c.getURL(WikiContext.EDIT, c.getPage().getName())%>"
           accesskey="e" >
        </wiki:Tab>
      </wiki:PageType>

      <wiki:PageType type="attachment">
        <wiki:Tab id="edit" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"actions.editparent.title")%>'
           url="<wiki:BaseURL/>Edit.jsp?page=<wiki:ParentPageName />"
           accesskey="e" >
        </wiki:Tab>
      </wiki:PageType>
    </wiki:Permission>
    
  </wiki:PageExists>

</wiki:TabbedSection>