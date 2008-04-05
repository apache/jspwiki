<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs['Language']}" />
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext( pageContext );
  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";
%>
  
<wiki:TabbedSection defaultTab="editcontent">  
  <wiki:Tab id="editcontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.edit")%>' accesskey="e">
  <wiki:CheckLock mode="locked" id="lock">
    <div class="error">
      <fmt:message key="edit.locked">
        <fmt:param><c:out value="${lock.locker}"/></fmt:param>
        <fmt:param><c:out value="${lock.timeLeft}"/></fmt:param>
      </fmt:message>
    </div>
  </wiki:CheckLock>
  
  <wiki:CheckVersion mode="notlatest">
    <div class="warning">
      <fmt:message key="edit.restoring">
        <fmt:param><wiki:PageVersion/></fmt:param>
      </fmt:message>
    </div>
  </wiki:CheckVersion>
    
  <wiki:Editor />
    
</wiki:Tab>
  
  <wiki:PageExists>  

  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a">
    <wiki:Include page="AttachmentTab.jsp"/>
  </wiki:Tab>

  <wiki:Tab id="info" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab")%>'
           url="<%=c.getURL(WikiContext.INFO, c.getPage().getName())%>"
           accesskey="i" >
  </wiki:Tab>

  </wiki:PageExists>  
    
  <wiki:Tab id="edithelp" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.help")%>' accesskey="h" >
  <wiki:InsertPage page="EditPageHelp" />
  <wiki:NoSuchPage page="EditPageHelp">
    <div class="error">
      <fmt:message key="comment.edithelpmissing">
        <fmt:param><wiki:EditLink page="EditPageHelp">EditPageHelp</wiki:EditLink></fmt:param>
      </fmt:message>
    </div>
  </wiki:NoSuchPage>  
  </wiki:Tab>

</wiki:TabbedSection>