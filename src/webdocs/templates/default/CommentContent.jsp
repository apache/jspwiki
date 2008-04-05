<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs['Language']}" />
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext( pageContext );
  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";  
%>

<wiki:TabbedSection defaultTab="commentcontent">
  <wiki:Tab id="pagecontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"comment.tab.discussionpage")%>'>
    <wiki:InsertPage/>
  </wiki:Tab>

  <wiki:Tab id="commentcontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"comment.tab.addcomment")%>'>

  <wiki:Editor />
  </wiki:Tab>

  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a">
    <wiki:Include page="AttachmentTab.jsp"/>
  </wiki:Tab>
  
  <wiki:Tab id="info" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab")%>'
           url="<%=c.getURL(WikiContext.INFO, c.getPage().getName())%>"
           accesskey="i" >
  </wiki:Tab>
    
  <wiki:Tab id="edithelp" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.help")%>'>
    <wiki:NoSuchPage page="EditPageHelp">
      <div class="error">
         <fmt:message key="comment.edithelpmissing">
            <fmt:param><wiki:EditLink page="EditPageHelp">EditPageHelp</wiki:EditLink></fmt:param>
         </fmt:message>
      </div>
    </wiki:NoSuchPage>

    <wiki:InsertPage page="EditPageHelp" />
  </wiki:Tab>
</wiki:TabbedSection>
