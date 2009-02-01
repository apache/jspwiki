<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ page import="org.apache.wiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<%
  WikiContext c = WikiContextFactory.findContext( pageContext );
  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";  
%>

<wiki:TabbedSection defaultTab="commentcontent">
  <wiki:Tab id="pagecontent" titleKey="comment.tab.discussionpage">
    <wiki:InsertPage/>
  </wiki:Tab>

  <wiki:Tab id="commentcontent" titleKey="comment.tab.addcomment">

  <wiki:Editor/>
  </wiki:Tab>

  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a">
    <wiki:Include page="AttachmentTab.jsp" />
  </wiki:Tab>
  
  <wiki:Tab id="info" titleKey="info.tab"
           url="<%=c.getURL(WikiContext.INFO, c.getPage().getName())%>"
           accesskey="i" >
  </wiki:Tab>
    
  <wiki:Tab id="edithelp" titleKey="edit.tab.help">
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
