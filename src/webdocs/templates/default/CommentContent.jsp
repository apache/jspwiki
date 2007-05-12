<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>

<wiki:TabbedSection defaultTab="commentcontent">
  <wiki:Tab id="pagecontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.discussionpage")%>'>
    <wiki:InsertPage/>
  </wiki:Tab>

  <wiki:Tab id="commentcontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.comment")%>'>
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
