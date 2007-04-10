<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>

<wiki:TabbedSection defaultTab="addcomment">
  <wiki:Tab id="pagecontent" title="Discussion page">
    <wiki:InsertPage/>
  </wiki:Tab>

  <wiki:Tab id="addcomment" title="Add comment">
    <wiki:Include page="editors/plain.jsp"/>
  </wiki:Tab>

  <wiki:Tab id="edithelp" title="Help">
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
