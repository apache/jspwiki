<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<wiki:TabbedSection defaultTab="${param.tab}">
  <wiki:Tab id="viewgroup" titleKey="group.tab">
  <h3><c:out value="${wikiActionBean.name}" /></h3>
  <strips:messages />
  <wiki:Permission permission="createGroups">
    <fmt:message key="group.createsuggestion">
      <fmt:param>
        <stripes:link beanclass="com.ecyrd.jspwiki.action.GroupActionBean" event="create">
          <stripes:param name="group" value="${wikiActionBean.name}" />
          <stripes:param name="group" value="${wikiActionBean.name}" />
          <fmt:message key="group.createit" />
        </stripes:link>
      </fmt:param>
    </fmt:message>
  </wiki:Permission>

  <table class="wikitable">
    <!-- Name -->
    <tr>
      <th><fmt:message key="group.name" /></th>
      <td>
        <fmt:message key="group.groupintro">
          <fmt:param><em><c:out value="${wikiActionBean.name}" /></em></fmt:param>
        </fmt:message>
      </td>
    </tr>
    <!-- Members -->
    <tr>
      <th><fmt:message key="group.members" />
      </th>
      <td>
         <c:forEach items="${wikiActionBean.members}" var="member" varStatus="loop">
           <c:out value="${member.name}" /><br/>
         </c:forEach>
      </td>
    </tr>
    <tr>
      <td colspan="2">
        <fmt:message key="group.modifier">
          <fmt:param><c:out value="${wikiActionBean.modifier}" /></fmt:param>
          <fmt:param><c:out value="${wikiActionBean.modified}" /></fmt:param>
        </fmt:message>
      </td>
    </tr>
    <tr>
      <td colspan="2">
        <fmt:message key="group.creator">
          <fmt:param><c:out value="${wikiActionBean.creator}" /></fmt:param>
          <fmt:param><c:out value="${wikiActionBean.created}" /></fmt:param>
        </fmt:message>
      </td>
    </tr>
  </table>

  <wiki:Permission permission="deleteGroup">
    <c:set var="confirm" value="<fmt:message key='grp.deletegroup.confirm'/>" scope="page"/>
    <stripes:form beanclass="com.ecyrd.jspwiki.action.GroupActionBean" class="wikiform"
      id="deleteGroup"
      onsubmit="return( confirm('${confirm}') && Wiki.submitOnce(this) );"
      method="POST" acceptcharset="UTF-8">
      <stripes:submit name="delete"><fmt:message key="actions.deletegroup" /></stripes:submit>
    </stripes:form>
  </wiki:Permission>

</wiki:Tab>

<wiki:Permission permission="editGroup">
  <wiki:Tab id="editgroup" titleKey="actions.editgroup"
           url="<stripes:link beanclass='com.ecyrd.jspwiki.action.GroupActionBean' event='edit'><stripes:param name='group' value='${wikiActionBean.name}' /></stripes:link>"
           accesskey="e" >
  </wiki:Tab>
</wiki:Permission>

</wiki:TabbedSection>
