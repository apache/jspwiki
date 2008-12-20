<%@ page errorPage="/Error.jsp" %>
<%@ page import="java.util.*" %>
<%@ page import="java.lang.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.util.jar.*" %>

<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="com.ecyrd.jspwiki.preferences.*" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %> 

<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<%@ page import="com.ecyrd.jspwiki.action.WikiContextFactory" %>

<h3><fmt:message key="prefs.heading"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message></h3>

<c:if test="${param.tab eq 'prefs'}">
  <div class="formhelp">
    <wiki:Messages div="error" topic="prefs" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix.prefs")%>' />
  </div>
</c:if>

<stripes:form beanclass="com.ecyrd.jspwiki.action.UserPreferencesActionBean" class="wikiform" id="setCookie" method="post" acceptcharset="UTF-8">
<table>
  <tr>
  <td><stripes:label for="assertedName" name="prefs.assertedname" /></td>
  <td> 
    <stripes:text id="assertedName" name="assertedName" size="20"><wiki:UserProfile property='wikiname' /></stripes:text>
    <stripes:errors field="assertedName" />
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
  <td><stripes:label for="editor" name="edit.chooseeditor" /></td>
  <td>
    <select id="editor" name="editor">
      <wiki:EditorIterator id="edt">
        <option <%=edt.isSelected()%>value="<%=edt.getName()%>"><%=edt.getName()%></option>
      </wiki:EditorIterator>
    </select>
    <stripes:errors field="editor" />
  </td>
  </tr>
  
  <tr>
  <td><stripes:label for="sectionEditing" name="prefs.user.sectionediting" /></td>
  <td>
    <stripes:checkbox id="sectionEditing" name="sectionEditing" checked="true" />
    <stripes:errors field="sectionEditing" />
    <fmt:message key="prefs.user.sectionediting.text" />
  </td>
  </tr>
  
  <tr>
  <td><stripes:label for="skin" name="prefs.user.skin" /></td>
  <td>
    <stripes:select id="skin" name="skin">
      <stripes:options-collection collection="${skins}" />
    </stripes:select>
    <stripes:errors field="skin" />
  </td>
  </tr>

  <c:if test='${not empty locales}'>
    <tr>
      <td><stripes:label for="locale" name="prefs.user.language" /></td>
      <td>
        <stripes:select name="locale">
          <stripes:options-map map="${locales}" />
        </stripes:select>
        <stripes:errors field="language" />
      </td>
    </tr>
  </c:if>

  <tr>
    <td><stripes:label for="orientation" name="prefs.user.orientation" /></td>
    <td>
      <stripes:select id="orientation" name="orientation">
        <stripes:options-enumeration enum="com.ecyrd.jspwiki.preferences.Preferences.Orientation" />
      </stripes:select>
      <stripes:errors field="orientation" />
    </td>
  </tr>

  <tr>
    <td><stripes:label for="timeFormat" name="prefs.user.timeformat" /></td>
    <td>
      <stripes:select id="timeFormat" name="timeFormat">
        <stripes:options-map map="${timeformats}" />
      </stripes:select>
      <stripes:errors field="timeFormat" />
    </td>
  </tr>

  <tr>
    <td><stripes:label for="timeZone" name="prefs.user.timezone" /></td>
    <td>
      <stripes:select id="timeZone" name="timeZone">
        <stripes:options-map map="${timezones}" />
      </stripes:select>
      <stripes:errors field="timeZone" />
    </td>
  </tr>

  <%--
  <tr>
  <td><label for="showQuickLinks">Show Quick Links</label></td>
  <td>
    <stripes:checkbox id="showQuickLinks" name="showQuickLinks" checked="true" />
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
    <stripes:checkbox id="showCalendar" name="showCalendar" checked="true" />
  </td>
  </tr>
  --%>
  <tr>
    <td>&nbsp;</td>
    <td>
      <stripes:submit name="save" accesskey="s"><fmt:message key='prefs.save.prefs.submit' /></stripes:submit>
      <stripes:hidden name="redirect"><wiki:Variable var='redirect' default='' /></stripes:hidden>
      <div class="formhelp"><fmt:message key='prefs.cookies' /></div>
    </td>
  </tr>

</table>
</stripes:form>
  
<!-- Clearing the 'asserted name' and other prefs in the cookie -->
<%--wiki:UserCheck status="asserted"--%>

<h3><fmt:message key='prefs.clear.heading' /></h3>

<stripes:form beanclass="com.ecyrd.jspwiki.action.UserPreferencesActionBean" id="clearCookie" method="post" acceptcharset="UTF-8">
  <div>
    <stripes:submit name="clearAssertedName"><fmt:message key='prefs.clear.submit' /></stripes:submit>
  </div>
  <div class="formhelp"><fmt:message key="prefs.clear.description" /></div>
</stripes:form>
