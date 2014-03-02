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
  pageContext.setAttribute( "hasMultipleEditors", c.getEngine().getEditorManager().getEditorList().length > 1 );

%>

<h3><fmt:message key="prefs.heading" /></h3>

<c:if test="${param.tab eq 'prefs'}" >
  <div class="formhelp">
    <wiki:Messages div="error" topic="prefs" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix.prefs")%>'/>
  </div>
</c:if>

<form action="<wiki:Link jsp='UserPreferences.jsp' format='url'><wiki:Param name='tab' value='prefs'/></wiki:Link>"
       class=""
          id="setCookie"
      method="post" accept-charset="<wiki:ContentEncoding />" >


  <div class="form-group">
  <label class="control-label form-col-20" for="assertedName"><fmt:message key="prefs.assertedname"/></label>
  <input class="form-control form-col-50" type="text" id="assertedName" name="assertedName" size="20" 
          autofocus="autofocus"
         value="<wiki:UserProfile property='wikiname' />" />
  <%-- CHECK THIS
  <input type="text" id="assertedName" name="assertedName" size="20" value="<wiki:UserProfile property='loginname'/>" />
  --%>  
  </div>

  <wiki:UserCheck status="anonymous">
  <div class="help-block form-col-offset-20">
    <fmt:message key="prefs.assertedname.description">
      <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
      <fmt:param>
        <a href="<wiki:Link jsp='Login.jsp' format='url'><wiki:Param name='tab' value='register'/></wiki:Link>">
          <fmt:message key="prefs.assertedname.create"/>
        </a>
      </fmt:param>
    </fmt:message>
  </div>  
  </wiki:UserCheck>

  <c:if test='${hasMultipleEditors}'>
  <div class="form-group">
  <label class="control-label form-col-20" for="editor"><fmt:message key="edit.chooseeditor"/></label>
  
    <select class="" id="editor" name="editor" data-pref="editor">
      <wiki:EditorIterator id="edt">
        <option <%=edt.isSelected()%> value="<%=edt.getName()%>"><%=edt.getName()%></option>
      </wiki:EditorIterator>
  </select>
  
  </div>
  </c:if>

  <div class="form-group">
  <label class="btn btn-default form-col-offset-20" for="prefSectionEditing">
    <input class="" id="prefSectionEditing" name="prefSectionEditing"  data-pref="SectionEditing"
         type="checkbox" ${prefs.SectionEditing=='on' ? 'checked="checked"' : ''} >
      <fmt:message key="prefs.user.sectionediting"/>
  </label>
  <fmt:message key="prefs.user.sectionediting.text"/>
  
  </div>

  <c:if test='${not empty skins}'>
  <div class="form-group">
  <label class="control-label form-col-20" for="prefSkin"><fmt:message key="prefs.user.skin"/></label>
  
  <select class="" id="prefSkin" name="prefSkin" data-pref="SkinName">
    <c:forEach items="${skins}" var="i">
      <option value='${i}' ${prefs.SkinName==i ? 'selected="selected"' : ''} >${i}</option>
    </c:forEach>
  </select>
  
  </div>
  </c:if>


  <c:if test='${not empty languages}'>
  <c:set var="prefLanguage" ><c:out value="${prefs.Language}" default="<%=request.getLocale().toString()%>" /></c:set>
  <div class="form-group">
  <label class="control-label form-col-20" for="prefLanguage"><fmt:message key="prefs.user.language"/></label>
  
  <select class="" id="prefLanguage" name="prefLanguage" data-pref="Language">
    <c:forEach items='${languages}' var='lg'>
      <option value="<c:out value='${lg.key}'/>" ${fn:startsWith(prefLanguage,lg.key) ? 'selected="selected"' : ''} >${lg.value}</option>
    </c:forEach>
  </select>
  
  </div>
  </c:if>

  <div class="form-group">
  <label class="control-label form-col-20" for="prefOrientation"><fmt:message key="prefs.user.orientation"/></label>
  
  <select class="" id="prefOrientation" name="prefOrientation" data-pref="Orientation">
      <option value='fav-left' ${prefs.Orientation=='fav-left' ? 'selected="selected"' : ''} ><fmt:message key="prefs.user.orientation.left"/></option>
      <option value='fav-right' ${prefs.Orientation=='fav-right' ? 'selected="selected"' : ''} ><fmt:message key="prefs.user.orientation.right"/></option>
  </select>
  
  </div>

  <div class="form-group">
  <label class="control-label form-col-20" for="prefLayout"><fmt:message key="prefs.user.layout"/></label>  
  <select class="" id="prefLayout" name="prefLayout" data-pref="Layout">
      <option value='fluid' ${prefs.Layout=='fluid' ? 'selected="selected"' : ''} ><fmt:message key="prefs.user.layout.fluid"/></option>
      <option value='fixed' ${prefs.Layout=='fixed' ? 'selected="selected"' : ''} ><fmt:message key="prefs.user.layout.fixed"/></option>
  </select>
  </div>

  <div class="form-group">
  <label class="control-label form-col-20" for="prefTimeFormat"><fmt:message key="prefs.user.timeformat"/></label>
  
  <select class="" id="prefTimeFormat" name="prefTimeFormat"  data-pref="DateFormat">
    <c:forEach items='${timeformats}' var='tf' >
      <option value='<c:out value="${tf.key}"/>' ${prefs.DateFormat==tf.key ? 'selected="selected"' : ''} >${tf.value}</option>
    </c:forEach>
  </select>
  
  </div>

  <div class="form-group">
  <label class="control-label form-col-20" for="prefTimeZone"><fmt:message key="prefs.user.timezone"/></label>
  
  <select class="" id='prefTimeZone' name='prefTimeZone'  data-pref="TimeZone">
    <c:forEach items='${timezones}' var='tz'>
      <option value='<c:out value="${tz.key}"/>' ${prefs.TimeZone==tz.key ? 'selected="selected"' : ''} >${tz.value}</option>
    </c:forEach>
  </select>
  
  </div>


  <div class="form-group">
  
    <input class="btn btn-primary form-col-offset-20" type="submit" name="ok" value="<fmt:message key='prefs.save.prefs.submit'/>"
      accesskey="s" />
    <input type="hidden" name="redirect" value="<wiki:Variable var='redirect' default='' />" />
    <input type="hidden" name="action" value="setAssertedName" />
    <p class="help-block form-col-offset-20"><fmt:message key='prefs.cookies'/></p>
  
  </div>

</form>

<%-- Clearing the 'asserted name' and other prefs in the cookie --%>
<%--wiki:UserCheck status="asserted"--%>
<%--
<h3><fmt:message key='prefs.clear.heading'/></h3>
--%>
<form action="<wiki:Link jsp='UserPreferences.jsp' format='url'><wiki:Param name='tab' value='prefs'/></wiki:Link>"
       class=""
          id="clearCookie"
      method="post" accept-charset="<wiki:ContentEncoding />" >
  <input class="btn btn-danger form-col-offset-20" type="submit" name="ok" value="<fmt:message key='prefs.clear.submit'/>" />
  <input type="hidden" name="action" value="clearAssertedName" />
  <p class="help-block form-col-offset-20"><fmt:message key="prefs.clear.description" /></p>

</form>
<%--/wiki:UserCheck--%>
