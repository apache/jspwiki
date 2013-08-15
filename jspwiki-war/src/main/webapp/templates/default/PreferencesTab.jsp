<%--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%>

<%@ page errorPage="/Error.jsp" %>
<%@ page import="java.util.*" %>
<%@ page import="java.lang.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.util.jar.*" %>

<%@ page import="org.apache.wiki.ui.*" %>
<%@ page import="org.apache.wiki.preferences.*" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  //FIXME: this should better move to UserPreferences.jsp but that doesn't seem to work. Ugh ?
  WikiContext c = WikiContext.findContext( pageContext );
  TemplateManager t = c.getEngine().getTemplateManager();
  pageContext.setAttribute( "skins", t.listSkins(pageContext, c.getTemplate() ) );
  pageContext.setAttribute( "languages", t.listLanguages(pageContext) );
  pageContext.setAttribute( "timeformats", t.listTimeFormats(pageContext) );
  pageContext.setAttribute( "timezones", t.listTimeZones(pageContext) );
%>

<h3><fmt:message key="prefs.heading"><fmt:param><wiki:Variable var="applicationname"/></fmt:param></fmt:message></h3>

<c:if test="${param.tab eq 'prefs'}" >
  <div class="formhelp">
    <wiki:Messages div="error" topic="prefs" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix.prefs")%>'/>
  </div>
</c:if>

<form action="<wiki:Link jsp='UserPreferences.jsp' format='url'><wiki:Param name='tab' value='prefs'/></wiki:Link>"
       class="wikiform"
          id="setCookie"
      method="post" accept-charset="<wiki:ContentEncoding />"
    onsubmit="WikiPreferences.savePrefs(); return Wiki.submitOnce(this);" >
<table>

  <tr>
  <td><label for="assertedName"><fmt:message key="prefs.assertedname"/></label></td>
  <td>
  <input type="text" id="assertedName" name="assertedName" size="20" value="<wiki:UserProfile property='wikiname' />" />
  <%-- CHECK THIS
  <input type="text" id="assertedName" name="assertedName" size="20" value="<wiki:UserProfile property='loginname'/>" />
  --%>
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
        <a href="<wiki:Link jsp='Login.jsp' format='url'><wiki:Param name='tab' value='register'/></wiki:Link>">
          <fmt:message key="prefs.assertedname.create"/>
        </a>
      </fmt:param>
    </fmt:message>
  </div>
  </td>
  </tr>
  </wiki:UserCheck>

  <tr>
  <td><label for="editor"><fmt:message key="edit.chooseeditor"/></label></td>
  <td>
    <select id="editor" name="editor">
      <wiki:EditorIterator id="edt">
        <option <%=edt.isSelected()%> value="<%=edt.getName()%>"><%=edt.getName()%></option>
      </wiki:EditorIterator>
  </select>
  </td>
  </tr>

  <tr>
  <td><label for="prefSectionEditing"><fmt:message key="prefs.user.sectionediting"/></label></td>
  <td>
  <input id="prefSectionEditing" name="prefSectionEditing"
       type="checkbox" <c:if test='${"on" == prefs.SectionEditing}'>checked="checked"</c:if> ></input>
  <fmt:message key="prefs.user.sectionediting.text"/>
  </td>
  </tr>

  <tr>
  <td><label for="prefSkin"><fmt:message key="prefs.user.skin"/></label></td>
  <td>
  <select id="prefSkin" name="prefSkin">
    <c:forEach items="${skins}" var="i">
      <option value='<c:out value='${i}'/>' <c:if test='${i == prefs.SkinName}'>selected="selected"</c:if> ><c:out value="${i}"/></option>
    </c:forEach>
  </select>
  </td>
  </tr>


  <c:if test='${not empty languages}'>
  <c:set var="prefLanguage" ><c:out value="${prefs.Language}" default="<%=request.getLocale().toString()%>" /></c:set>
  <tr>
  <td><label for="prefLanguage"><fmt:message key="prefs.user.language"/></label></td>
  <td>
  <select id="prefLanguage" name="prefLanguage">
    <c:forEach items='${languages}' var='lg'>
      <option value="<c:out value='${lg.key}'/>" <c:if test='${fn:startsWith(prefLanguage,lg.key)}'>selected="selected"</c:if> ><c:out value="${lg.value}"/></option>
    </c:forEach>
  </select>
  </td>
  </tr>
  </c:if>

  <tr>
  <td><label for="prefOrientation"><fmt:message key="prefs.user.orientation"/></label></td>
  <td>
  <select id="prefOrientation" name="prefOrientation" onclick="Wiki.changeOrientation();">
      <option value='fav-left' <c:if test='${"fav-left" == prefs.Orientation}'>selected="selected"</c:if> ><fmt:message key="prefs.user.orientation.left"/></option>
      <option value='fav-right' <c:if test='${"fav-right" == prefs.Orientation}'>selected="selected"</c:if> ><fmt:message key="prefs.user.orientation.right"/></option>
  </select>
  </td>
  </tr>

  <tr>
  <td><label for="prefTimeFormat"><fmt:message key="prefs.user.timeformat"/></label></td>
  <td>
  <select id="prefTimeFormat" name="prefTimeFormat" >
    <c:forEach items='${timeformats}' var='tf' >
      <option value='<c:out value="${tf.key}"/>' <c:if test='${tf.key == prefs.DateFormat}'>selected="selected"</c:if> ><c:out value="${tf.value}"/></option>
    </c:forEach>
  </select>
  </td>
  </tr>

  <tr>
  <td><label for="prefTimeZone"><fmt:message key="prefs.user.timezone"/></label></td>
  <td>
  <select id='prefTimeZone' name='prefTimeZone'>
    <c:forEach items='${timezones}' var='tz'>
      <option value='<c:out value="${tz.key}"/>' <c:if test='${tz.key == prefs.TimeZone}'>selected="selected"</c:if> ><c:out value="${tz.value}"/></option>
    </c:forEach>
  </select>
  </td>
  </tr>

  <%--
  <tr>
  <td><label for="prefShowQuickLinks">Show Quick Links</label></td>
  <td>
  <input class='checkbox' type='checkbox' id='prefShowQuickLinks' name='prefShowQuickLinks'
         <c:if test='${"on" == prefs.SectionEdit}'>selected="selected"</c:if> />
         <span class="quicklinks"><span
               class='quick2Top'><a href='#wikibody' title='Go to Top' >&laquo;</a></span><span
               class='quick2Prev'><a href='#' title='Go to Previous Section'>&lsaquo;</a></span><span
               class='quick2Edit'><a href='#' title='Edit this section'>&bull;</a></span><span
               class='quick2Next'><a href='#' title='Go to Next Section'>&rsaquo;</a></span><span
               class='quick2Bottom'><a href='#footer' title='Go to Bottom' >&raquo;</a></span></span>
  </td>
  </tr>

  <tr>
  <td><label for="prefShowCalendar">Show Calendar</label></td>
  <td>
    <input class='checkbox' type='checkbox' id='prefShowCalendar' name='prefShowCalendar'
            <%= (prefShowCalendar.equals("yes") ? "checked='checked'": "") %> >
  </td>
  </tr>
  --%>
 <tr>
  <td>&nbsp;</td>
  <td>
    <input type="submit" name="ok" value="<fmt:message key='prefs.save.prefs.submit'/>"
      accesskey="s" />
    <input type="hidden" name="redirect" value="<wiki:Variable var='redirect' default='' />" />
    <input type="hidden" name="action" value="setAssertedName" />
    <div class="formhelp"><fmt:message key='prefs.cookies'/></div>
  </td>
  </tr>

</table>
</form>

<!-- Clearing the 'asserted name' and other prefs in the cookie -->
<%--wiki:UserCheck status="asserted"--%>

<h3><fmt:message key='prefs.clear.heading'/></h3>

<form action="<wiki:Link jsp='UserPreferences.jsp' format='url'><wiki:Param name='tab' value='prefs'/></wiki:Link>"
       class="wikiform"
          id="clearCookie"
    onsubmit="Wiki.prefs.empty(); return Wiki.submitOnce( this );"
      method="post" accept-charset="<wiki:ContentEncoding />" >
  <div>
  <input type="submit" name="ok" value="<fmt:message key='prefs.clear.submit'/>" />
  <input type="hidden" name="action" value="clearAssertedName" />
  </div>
  <div class="formhelp"><fmt:message key="prefs.clear.description" /></div>

</form>
<%--/wiki:UserCheck--%>
