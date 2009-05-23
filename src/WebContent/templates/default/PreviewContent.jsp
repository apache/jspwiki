<%-- 
    JSPWiki - a JSP-based WikiWiki clone.

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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.filters.SpamFilter" %>
<%@ page import="org.apache.wiki.ui.EditorManager" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>

<wiki:TabbedSection>
  <wiki:Tab id="previewcontent" titleKey="preview.tab">
  
    <div class="information">
      <fmt:message key="preview.info" />
    </div>
    <s:errors />
    <s:messages />

    <s:form beanclass="org.apache.wiki.action.EditActionBean" method="post" acceptcharset="UTF-8" 
      class="wikiform" id="editform" enctype="application/x-www-form-urlencoded">
    
      <p>
        <%-- Edit.jsp & Comment.jsp rely on these being found.  So be careful, if you make changes. --%>
        <s:hidden name="author" />
        <s:hidden name="changenote" />
        <s:hidden name="link" />
        <s:hidden name="page" value="${wikiActionBean.page.name}" />
        <s:hidden name="remember" />
        <s:hidden name="startTime" />
        <s:hidden name="text" />
        <%-- Spam detection fields --%>
        <wiki:SpamProtect />
      </p>
      <div id="submitbuttons">
        <c:set var="editTitle"><fmt:message key="editor.preview.edit.title"/></c:set>
        <s:submit name="edit" accesskey="e" title="${editTitle}"><fmt:message key="editor.preview.edit.submit" /></s:submit>
        <c:set var="saveTitle"><fmt:message key='editor.preview.save.title'/></c:set>
        <s:submit name="save" accesskey="s" title="${saveTitle}" />
        <c:set var="cancelTitle"><fmt:message key='editor.preview.cancel.title'/></c:set>
        <s:submit name="cancel" accesskey="q" title="${cancelTitle}" />
      </div>
      
    </s:form>
  
    <div class="previewcontent">
      <wiki:Translate>${wikiActionBean.text}</wiki:Translate>
    </div>
  
    <div class="information">
      <fmt:message key="preview.info" />
    </div>
  
  </wiki:Tab>
</wiki:TabbedSection>