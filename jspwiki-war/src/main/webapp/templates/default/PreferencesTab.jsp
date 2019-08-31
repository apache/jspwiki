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
  WikiContext c = WikiContext.findContext( pageContext );
  TemplateManager t = c.getEngine().getTemplateManager();
%>
<c:set var="skins"       value="<%= t.listSkins(pageContext, c.getTemplate() ) %>" />
<c:set var="languages"   value="<%= t.listLanguages(pageContext) %>" />
<c:set var="timezones"   value="<%= t.listTimeZones(pageContext) %>" />
<c:set var="timeformats" value="<%= t.listTimeFormats(pageContext) %>" />
<c:set var="editors"     value="<%= c.getEngine().getEditorManager().getEditorList() %>" />
<c:set var="redirect"><wiki:Variable var='redirect' default='<%=c.getEngine().getFrontPage() %>' /></c:set>

<form action="<wiki:Link jsp='UserPreferences.jsp' format='url'><wiki:Param name='tab' value='prefs'/></wiki:Link>"
          id="preferences"  <%-- used by Prefs.js to set/reset the userpreferences cookie --%>
      method="post" accept-charset="<wiki:ContentEncoding />" >

  <input type="hidden" name="redirect" value="${redirect}" />

  <div class="form-group ">

    <span class="form-col-20 control-label"></span>

    <span class="dropdown" style="display:inline-block" >
      <button class="btn btn-success" type="submit" name="action" value="setAssertedName">
        <fmt:message key='prefs.save.prefs.submit'/>
      </button>
      <ul class="dropdown-menu" data-hover-parent=".dropdown">
        <li class="dropdown-header"><fmt:message key='prefs.cookies'/></li>
      </ul>
    </span>

    <span class="dropdown" style="display:inline-block" >
      <button class="btn btn-default" type="submit" name="action" value="clearAssertedName"
       <%--<wiki:UserCheck status="anonymous">disabled</wiki:UserCheck>--%>
       ><span class="icon-trash-o"></span> <fmt:message key='prefs.clear.submit'/></button>
        <ul class="dropdown-menu" data-hover-parent=".dropdown">
          <li class="dropdown-header"><fmt:message key="prefs.clear.description" /></li>
        </ul>
    </span>

    <wiki:Link cssClass="btn btn-danger pull-right"  page="${redirect}" >
      <fmt:message key='prefs.cancel.submit'/>
    </wiki:Link>

  </div>

  <c:if test="${param.tab eq 'prefs'}" >
  <div>
    <span class="form-col-20 control-label"></span>
    <fmt:message key="prefs.errorprefix.prefs" var="msg"/>
    <wiki:Messages div="alert alert-danger form-col-50" topic="prefs" prefix="${msg}" />
  </div>
  </c:if>

  <div class="form-group">
    <label class="control-label form-col-20" for="assertedName"><fmt:message key="prefs.assertedname"/></label>
    <span class="dropdown form-col-50">
    <input class="form-control" type="text" id="assertedName" name="assertedName" size="20"
       autofocus="autofocus"
           value="<wiki:UserProfile property='wikiname' />" />
    <%-- CHECK THIS
    <input type="text" id="assertedName" name="assertedName" size="20" value="<wiki:UserProfile property='loginname'/>" />
    --%>
    <wiki:UserCheck status="anonymous">
      <ul class="dropdown-menu" data-hover-parent=".dropdown">
      <li class="dropdown-header">
        <fmt:message key="prefs.assertedname.description">
          <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
          <fmt:param>
            <a href="<wiki:Link jsp='Login.jsp' format='url'><wiki:Param name='tab' value='register'/></wiki:Link>">
            <fmt:message key="prefs.assertedname.create"/>
            </a>
          </fmt:param>
        </fmt:message>
      </li>
      </ul>
    </wiki:UserCheck>
    </span>
  </div>

  <c:if test='${fn:length(editors)>1}'>
  <div class="form-group">
    <label class="control-label form-col-20" for="editor"><fmt:message key="edit.chooseeditor"/></label>
    <select class="" id="editor" name="editor" data-pref="editor">
      <%-- no need to use EditorIterator tags--%>
      <c:forEach items="${editors}" var="edt">
        <option value='${edt}' ${prefs.editor==edt ? 'selected="selected"' : ''} >${edt}</option>
      </c:forEach>
    </select>
  </div>
  </c:if>

  <div class="form-group form-inline">
    <label class="control-label form-col-20" for="prefSectionEditing"><fmt:message key="prefs.user.sectionediting"/></label>
    <label class="form-control form-switch">
      <input class="" id="prefSectionEditing" name="prefSectionEditing"  data-pref="SectionEditing"
         type="checkbox" ${prefs.SectionEditing ? 'checked="checked"' : ''} >
      <fmt:message key="prefs.user.sectionediting.text"/>
    </label>
  </div>

  <div class="form-group form-inline ">
    <label class="control-label form-col-20" for="prefAppearance"><fmt:message key="prefs.user.appearance"/></label>
    <label class="form-control form-switch xpref-appearance">
      <!--<fmt:message key="prefs.user.appearance.light"/>-->
      <input id="prefAppearance" name="prefAppearance"  data-pref="Appearance"
           type="checkbox" class="" value="on" ${prefs.Appearance ? 'checked="checked"' : ''} >
      <fmt:message key="prefs.user.appearance.dark"/>
    </label>
  </div>

  <c:if test='${not empty skins}'>
  <div class="form-group">
    <label class="control-label form-col-20" for="prefSkin"><fmt:message key="prefs.user.skin"/></label>
    <select id="prefSkin" name="prefSkin" data-pref="SkinName">
      <c:forEach items="${skins}" var="i">
        <option value='${i}' ${prefs.SkinName==i ? 'selected="selected"' : ''} >${i}</option>
      </c:forEach>
    </select>
  </div>
  </c:if>

  <c:if test='${not empty languages}'>
  <div class="form-group">
    <label class="control-label form-col-20" for="prefLanguage"><fmt:message key="prefs.user.language"/></label>
    <select id="prefLanguage" name="prefLanguage" data-pref="Language">
      <c:forEach items='${languages}' var='lg'>
        <option value="<c:out value='${lg.key}'/>" ${fn:startsWith(prefs.Language,lg.key) ? 'selected="selected"' : ''} >${lg.value}</option>
      </c:forEach>
    </select>
  </div>
  </c:if>

  <div class="form-group">
    <label class="control-label form-col-20" for="prefOrientation"><fmt:message key="prefs.user.layout"/></label>
    <div class="btn-group" data-toggle="buttons">
      <label class="btn btn-default" >
        <input type="radio" data-pref="Layout"
                            name="prefLayout" ${prefs.Layout!='fixed' ? "checked='checked'" : ""} value="fluid"><fmt:message key='prefs.user.layout.fluid' />
      </label>
      <label class="btn btn-default">
        <input type="radio" data-pref="Layout"
                            name="prefLayout" ${prefs.Layout=='fixed' ? "checked='checked'" : ""} value="fixed"><fmt:message key='prefs.user.layout.fixed' />
      </label>
    </div>

    <div class="btn-group" data-toggle="buttons">
      <label class="btn btn-default">
        <input type="radio" data-pref="Orientation"
                            name="prefOrientation" ${prefs.Orientation=='fav-left' ? "checked='checked'" : ""} value="fav-left"><fmt:message key='prefs.user.orientation.left' />
      </label>
      <label class="btn btn-default">
        <input type="radio" data-pref="Orientation"
                            name="prefOrientation" ${prefs.Orientation=='fav-right' ? "checked='checked'" : ""} value="fav-right"><fmt:message key='prefs.user.orientation.right' />
      </label>
    </div>
  </div>


  <div class="form-group">
    <label class="control-label form-col-20" for="prefTimeFormat"><fmt:message key="prefs.user.timeformat"/></label>
    <select id="prefTimeFormat" name="prefTimeFormat"  data-pref="DateFormat">
      <c:forEach items='${timeformats}' var='tf' >
        <option value='<c:out value="${tf.key}"/>' ${prefs.DateFormat==tf.key ? 'selected="selected"' : ''} >${tf.value}</option>
      </c:forEach>
    </select>
  </div>

  <div class="form-group">
    <label class="control-label form-col-20" for="prefTimeZone"><fmt:message key="prefs.user.timezone"/></label>
    <select id="prefTimeZone" name="prefTimeZone"  data-pref="TimeZone">
      <c:forEach items="${timezones}" var="tz">
        <option value='<c:out value="${tz.key}"/>' ${prefs.TimeZone==tz.key ? 'selected="selected"' : ''} >${tz.value}</option>
      </c:forEach>
    </select>
  </div>

  <hr />

  <div class="form-group table-striped-bordered-condensed-fit-sort">
    <label id="pref-user-pagecookies" class="control-label form-col-20" style="vertical-align:top;">
      <fmt:message key="prefs.user.pagecookies"/>
    </label>
    <table class="wikitable" style="display:inline-block;" aria-describedby="pref-user-pagecookies">
    <tr>
      <th scope="col"><fmt:message key="prefs.user.pagecookies.type"/></th>
      <th scope="col"><fmt:message key="prefs.user.pagecookies.page"/></th>
      <th scope="col"><fmt:message key="prefs.user.pagecookies.actions"/></th>
    </tr>
    <c:forEach var="aCookie" items="${pageContext.request.cookies}" >
      <c:if test="${fn:startsWith(aCookie.name,'JSPWiki.') }">
        <c:set var="cookiePieces" value="${fn:split(aCookie.name, '.')}" />
        <c:set var="cookieType" value="${cookiePieces[1]}" />
        <c:set var="cookiePage" value="${fn:replace(cookiePieces[2], '%20', ' ')}" />
        <tr>
          <td>${cookieType}</td>
          <td><wiki:Link cssClass="slimbox" page="${cookiePage}">${cookiePage}</wiki:Link></td>
          <td><div class="btn btn-xs btn-danger" data-delete-cookie="${aCookie.name}"><fmt:message key="prefs.user.pagecookie.delete"/></div></td>
        </tr>
      </c:if>
    </c:forEach>
    </table>
  </div>

</form>
