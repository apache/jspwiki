<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="java.net.URLEncoder" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs['Language']}" />
<fmt:setBundle basename="templates.default"/>

<wiki:TabbedSection>
<wiki:Tab id="findcontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "find.tab")%>' accesskey="s">

<form action="<wiki:Link format='url' jsp='Search.jsp'/>"
       class="wikiform"
          id="searchform2"
         accept-charset="<wiki:ContentEncoding/>">

  <h4><fmt:message key="find.input" /></h4>
  <p>
    <input type="text"
           name="query" id="query2" 
          value="<c:out value='${query}'/>" 
           size="32" />

    <input type="checkbox" name="details" id="details" <c:if test='${param.details == "on"}'>checked='checked'</c:if> />
    <fmt:message key="find.details" />

    <select name="scope" id="scope" > 
      <option value="" <c:if test="${empty param.scope}">selected="selected"</c:if> ><fmt:message key='find.scope.all' /></option>
      <option value="author:" <c:if test='${param.scope eq "author:"}'>selected="selected"</c:if> ><fmt:message key='find.scope.authors' /></option>
      <option value="name:" <c:if test='${param.scope eq "name:"}'>selected="selected"</c:if> ><fmt:message key='find.scope.pagename' /></option>
      <option value="contents:" <c:if test='${param.scope eq "contents:"}'>selected="selected"</c:if> ><fmt:message key='find.scope.content' /></option>
      <option value="attachment:" <c:if test='${param.scope eq "attachment:"}'>selected="selected"</c:if> ><fmt:message key='find.scope.attach' /></option>       
    </select>

	<input type="submit" name="ok" id="ok" value="<fmt:message key="find.submit.find"/>" />
	<input type="submit" name="go" id="go" value="<fmt:message key="find.submit.go"/>" />
    <input type="hidden" name="start" id="start" value="0" />
    <input type="hidden" name="maxitems" id="maxitems" value="20" />

    <span id="spin" class="spin" style="position:absolute;display:none;"></span>
  </p>
</form>

<div id="searchResult2" ><wiki:Include page="AJAXSearch.jsp"/></div>

</wiki:Tab>

<wiki:PageExists page="SearchPageHelp">
<wiki:Tab id="findhelp" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "find.tab.help")%>' accesskey="h">
  <wiki:InsertPage page="SearchPageHelp"/>
</wiki:Tab>
</wiki:PageExists>

</wiki:TabbedSection>