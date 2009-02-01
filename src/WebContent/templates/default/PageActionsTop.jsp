<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<div id="actionsTop" class="pageactions"> 
  <ul>
  
    <wiki:CheckRequestContext context='view|info|diff|upload|rename'>
      <wiki:Permission permission="edit">
      	<li>
          <wiki:PageType type="page">
            <c:set var="editTitle"><fmt:message key="actions.edit.title" /></c:set>
            <stripes:link
              beanclass="org.apache.wiki.action.EditActionBean" event="edit"
              accesskey="e" class="action edit"
              title="${editTitle}">
              <fmt:message key='actions.edit' />
              <stripes:param name="page" value="${wikiContext.page.name}" />
            </stripes:link>
          </wiki:PageType>
          <wiki:PageType type="attachment">
            <c:set var="editParentTitle"><fmt:message key="actions.editparent.title" /></c:set>
            <stripes:link
              beanclass="org.apache.wiki.action.EditActionBean" event="edit"
              accesskey="e" class="action edit"
              title="${editParentTitle}">
              <fmt:message key='actions.edit' />
              <stripes:param name="page"><wiki:ParentPageName/></stripes:param>
            </stripes:link>
          </wiki:PageType>
        </li>
      </wiki:Permission>
    </wiki:CheckRequestContext>

    <%-- converted to popup menu by jspwiki-common.js--%>
    <li id="morebutton">
      <stripes:link
        beanclass="org.apache.wiki.action.ViewActionBean"
        class="action more">
        <stripes:param name="page" value="MoreMenu" />
        <fmt:message key="actions.more" />
      </stripes:link>
    </li>

  </ul>
</div>
