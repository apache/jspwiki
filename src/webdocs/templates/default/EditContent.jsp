<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.DefaultResources"/>
<%
  /* see commonheader.jsp */
  String prefEditAreaHeight = (String) session.getAttribute("prefEditAreaHeight");
%>
  
<wiki:CheckLock mode="locked" id="lock">
  <%-- need a cancel button here --%>
  <p class="error"><fmt:message key="edit.locked">
      <fmt:param><%=lock.getLocker()%></fmt:param>
      <fmt:param><%=lock.getTimeLeft()%></fmt:param>
      </fmt:message>
  </p>
</wiki:CheckLock>
  
<wiki:TabbedSection>
  
  <wiki:Tab id="editcontent" title="<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.edit")%>">
  
    <wiki:CheckVersion mode="notlatest">
      <div class="warning"><fmt:message key="edit.restoring">
        <fmt:param><wiki:PageVersion/></fmt:param>
        </fmt:message>
      </div>
    </wiki:CheckVersion>
    
    <div id="editorbar">  
    <fmt:message key="edit.chooseeditor"/>
    <select onchange="location.href=this.value">
      <wiki:EditorIterator id="editor">
        <option <%=editor.isSelected()%> value="<%=editor.getURL()%>"><%=editor.getName()%></option>
      </wiki:EditorIterator>
    </select>
    </div>
    
    <wiki:Editor />
    
  </wiki:Tab>
  
  <wiki:HasAttachments>
    <wiki:Tab id="attachments" title="<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.attachments")%>">
      <wiki:Include page="AttachmentTab.jsp" />
    </wiki:Tab>
  </wiki:HasAttachments>
  
  <wiki:Tab id="edithelp" title="<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.help")%>">
    <wiki:NoSuchPage page="EditPageHelp">
      <div class="error">
         <fmt:message key="comment.edithelpmissing">
            <fmt:param><wiki:EditLink page="EditPageHelp">EditPageHelp</wiki:EditLink></fmt:param>
         </fmt:message>
      </div>
    </wiki:NoSuchPage>
  
    <wiki:InsertPage page="EditPageHelp" />
  </wiki:Tab>
  
  <wiki:Tab id="searchbarhelp" title="<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.findreplacehelp")%>">
    <wiki:InsertPage page="EditFindAndReplaceHelp" />
  </wiki:Tab>
  
</wiki:TabbedSection>
