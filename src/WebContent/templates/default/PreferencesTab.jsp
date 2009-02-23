<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %> 
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>

<h3><fmt:message key="prefs.heading"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message></h3>

<c:if test="${param.tab eq 'prefs'}">
  <div class="formhelp">
    <wiki:Messages div="error" topic="prefs" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix.prefs")%>' />
  </div>
</c:if>

<s:form beanclass="org.apache.wiki.action.UserPreferencesActionBean" class="wikiform" id="setCookie" method="post" acceptcharset="UTF-8">
<table>
  <tr>
  <td><s:label for="assertedName" /></td>
  <td> 
    <s:text id="assertedName" name="assertedName" size="20"><wiki:UserProfile property='wikiname' /></s:text>
    <s:errors field="assertedName" />
  </td>
  </tr>
  <wiki:UserCheck status="anonymous">
  <tr>
  <td>&nbsp;</td>
  <td>
  <div class="formhelp">
    <fmt:message key="prefs.assertedname.description">
      <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
      <fmt:param>
        <a href="<wiki:Link jsp='Login.jsp' format='url'></wiki:Link>">
          <fmt:message key="prefs.assertedname.create" />
        </a>
      </fmt:param>
    </fmt:message>
  </div>
  </td>
  </tr>
  </wiki:UserCheck>

  <tr>
  <td><s:label for="editor" /></td>
  <td>
    <select id="editor" name="editor">
      <wiki:EditorIterator id="edt">
        <option <%=edt.isSelected()%>value="<%=edt.getName()%>"><%=edt.getName()%></option>
      </wiki:EditorIterator>
    </select>
    <s:errors field="editor" />
  </td>
  </tr>
  
  <tr>
  <td><s:label for="sectionEditing" /></td>
  <td>
    <s:checkbox id="sectionEditing" name="sectionEditing" checked="true" />
    <s:errors field="sectionEditing" />
    <fmt:message key="prefs.user.sectionediting.text" />
  </td>
  </tr>
  
  <tr>
  <td><s:label for="skin" /></td>
  <td>
    <s:select id="skin" name="skin">
      <s:options-collection collection="${skins}" />
    </s:select>
    <s:errors field="skin" />
  </td>
  </tr>

  <c:if test='${not empty locales}'>
    <tr>
      <td><s:label for="locale" /></td>
      <td>
        <s:select name="locale">
          <s:options-map map="${locales}" />
        </s:select>
        <s:errors field="locale" />
      </td>
    </tr>
  </c:if>

  <tr>
    <td><s:label for="orientation" /></td>
    <td>
      <s:select id="orientation" name="orientation">
        <s:options-enumeration enum="org.apache.wiki.preferences.Preferences.Orientation" label="name" />
      </s:select>
      <s:errors field="orientation" />
    </td>
  </tr>

  <tr>
    <td><s:label for="timeFormat" /></td>
    <td>
      <s:select id="timeFormat" name="timeFormat">
        <s:options-map map="${timeformats}" />
      </s:select>
      <s:errors field="timeFormat" />
    </td>
  </tr>

  <tr>
    <td><s:label for="timeZone" /></td>
    <td>
      <s:select id="timeZone" name="timeZone">
        <s:options-map map="${timezones}" />
      </s:select>
      <s:errors field="timeZone" />
    </td>
  </tr>

  <%--
  <tr>
  <td><label for="showQuickLinks">Show Quick Links</label></td>
  <td>
    <s:checkbox id="showQuickLinks" name="showQuickLinks" checked="true" />
         <span class="quicklinks"><span 
               class='quick2Top'><a href='#wikibody' title='Go to Top' >&laquo;</a></span><span 
               class='quick2Prev'><a href='#' title='Go to Previous Section'>&lsaquo;</a></span><span 
               class='quick2Edit'><a href='#' title='Edit this section'>&bull;</a></span><span 
               class='quick2Next'><a href='#' title='Go to Next Section'>&rsaquo;</a></span><span 
               class='quick2Bottom'><a href='#footer' title='Go to Bottom' >&raquo;</a></span></span>
  </td>
  </tr>

  <tr>
  <td><label for="showCalendar">Show Calendar</label></td>
  <td>
    <s:checkbox id="showCalendar" name="showCalendar" checked="true" />
  </td>
  </tr>
  --%>
  <tr>
    <td>&nbsp;</td>
    <td>
      <s:submit name="save" accesskey="s" />
      <s:hidden name="redirect"><wiki:Variable var='redirect' default='' /></s:hidden>
      <div class="formhelp"><fmt:message key='prefs.cookies' /></div>
    </td>
  </tr>

</table>
</s:form>
  
<!-- Clearing the 'asserted name' and other prefs in the cookie -->
<%--wiki:UserCheck status="asserted"--%>

<h3><fmt:message key='prefs.clear.heading' /></h3>

<s:form beanclass="org.apache.wiki.action.UserPreferencesActionBean" id="clearCookie" method="post" acceptcharset="UTF-8">
  <div>
    <s:submit name="clearAssertedName" />
  </div>
  <div class="formhelp"><fmt:message key="prefs.clear.description" /></div>
</s:form>
