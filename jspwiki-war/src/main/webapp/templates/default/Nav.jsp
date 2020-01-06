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

<%@ page import="org.apache.wiki.*" %>
<%@ page import="java.util.StringTokenizer" %>

<%@ page import="org.apache.wiki.attachment.*" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext( pageContext );

  String text = c.getEngine().getPageManager().getText( c.getPage() );
  StringTokenizer tokens = new StringTokenizer( text );
  //avg reading speeds: https://iovs.arvojournals.org/article.aspx?articleid=2166061

%>
<c:set var="attachments" value="<%= c.getEngine().getAttachmentManager().listAttachments( c.getPage() ).size() %>" />

<c:set var="wordCount" value="<%= tokens.countTokens() %>" />
<c:set var="readingTime" value="${wordCount / 228}" />


<%-- navigation bar --%>
<div class="navigation" role="navigation">

<ul class="nav nav-pills pull-left">
  <%-- toggle sidebar --%>
  <li id="menu"><a href="#"><!--&#x2261;-->&#9776;</a></li>

  <c:set var="refresh_breadCrumbTrail_attr"><wiki:Breadcrumbs /></c:set>
  <%-- don't show the breadcrumbs if it has none or only one item --%>
  <c:if test="${fn:length(breadCrumbTrail) gt 2}">
  <li id="trail" tabindex="0">
    <a href="#">
        <span>&hellip;</span>
        <span><fmt:message key="actions.trail"/></span>
        <span class="caret"></span>
    </a>
    <ul class="dropdown-menu" data-hover-parent="li">
      <%--
      <li class="dropdown-header"><fmt:message key="header.yourtrail"/></li>
      <li class="divider"></li>
      --%>
      <%--  FIXME: breadcrumbs tag returns items in wrong order: most recent item is at back of the list !!
      <li><wiki:Breadcrumbs separator="</li><li>" /></li>
      --%>
      <c:forEach items="${breadCrumbTrail}" varStatus="status" begin="2">
          <c:set var="crumb" value="${breadCrumbTrail[fn:length(breadCrumbTrail) - status.index]}" />
          <li><wiki:Translate>[${crumb}]</wiki:Translate></li>
      </c:forEach>

    </ul>
  </li>
  </c:if>

</ul>

<ul class="nav nav-pills pull-right">

    <c:set var="page">
      <wiki:PageType type="page"><wiki:PageName/></wiki:PageType>
      <wiki:PageType type="attachment"><wiki:ParentPageName/></wiki:PageType>
    </c:set>

  <%-- view --%>

  <%-- context upload -> context view&tab=attach ... --%>
  <%--
  <c:if test="${param.tab eq 'attach'}">
  <li id="view">
    <wiki:Link page="${page}" >
        <span class="icon-view-menu"></span>
        <span><fmt:message key="view.tab"/></span>
    </wiki:Link>
  </li>
  </c:if>
  --%>
  <wiki:CheckRequestContext context='info|diff|upload|rename|edit|comment|conflict'>
  <li id="view">
    <wiki:Link page="${page}" >
        <span class="icon-view-menu"></span>
        <span><fmt:message key="view.tab"/></span>
    </wiki:Link>
  </li>
  </wiki:CheckRequestContext>

  <%-- attachment   : included in the info menu
  <wiki:CheckRequestContext context='view|info|rename|diff|rename|edit|comment|conflict'>
  <wiki:PageExists>
  <c:if test="${param.tab ne 'attach'}"><!-- context upload -> context view&tab=attach ... -- >
  <li id="attach"
   class="<wiki:Permission permission='!upload'>disabled</wiki:Permission>">
    <wiki:Link page="${page}" context="upload" accessKey="a" >
      <span class="icon-paper-clip"></span>
      <span><fmt:message key='attach.tab'/></span>
      <c:if test="${attachments > 0}"><span class="badge">${attachments}</span></c:if>
    </wiki:Link>
  </li>
  </c:if>
  </wiki:PageExists>
  </wiki:CheckRequestContext>
  --%>

  <%-- info --%>
  <wiki:CheckRequestContext context='view|info|upload|rename|edit|comment|conflict'>
  <wiki:PageExists>
  <li id="info" tabindex="0" role="contentinfo">
      <a href="#" accessKey="i">
        <span class="icon-info-menu"></span>
        <span><fmt:message key='info.tab'/></span>
        <c:if test="${attachments > 0}"><span class="badge">${attachments}</span></c:if>
        <wiki:PageExists><span class="caret"></span></wiki:PageExists>
      </a>
    <ul class="dropdown-menu pull-right" data-hover-parent="li">
      <li class="dropdown-header"><fmt:message key="info.version"/> : <span class="badge"><wiki:PageVersion /></span></li>
      <li class="dropdown-header"><fmt:message key="info.date"/> :
        <span>
        <wiki:CheckVersion mode="latest">
          <wiki:DiffLink version="latest" newVersion="previous"><wiki:PageDate format='${prefs["DateFormat"]}'/></wiki:DiffLink>
        </wiki:CheckVersion>
        <wiki:CheckVersion mode="notlatest">
          <wiki:DiffLink version="current" newVersion="latest"><wiki:PageDate format='${prefs["DateFormat"]}'/></wiki:DiffLink>
        </wiki:CheckVersion>
        </span>
      </li>
      <li class="dropdown-header"><fmt:message key="info.author"/> :
		<wiki:Author format="plain"/>
      </li>
      <li class="dropdown-header">
        <wiki:RSSImageLink mode="wiki" title="<fmt:message key='info.feed'/>"/>
      </li>
      <li class="divider"></li>
      <li class="dropdown-header">
        <c:set var="disabledBtn" value=""/>
        <wiki:CheckRequestContext context='info'><c:set var="disabledBtn" value="disabled" /></wiki:CheckRequestContext>
          <wiki:Link cssClass="btn btn-xs btn-default ${disabledBtn}" context="info" tabindex="0"><fmt:message key='info.moreinfo'/></wiki:Link>
      </li>
      <li class="dropdown-header">
        <c:set var="disabledBtn" value=""/>
        <wiki:CheckRequestContext context='upload'><c:set var="disabledBtn" value="disabled" /></wiki:CheckRequestContext>
        <wiki:Permission permission='!upload'><c:set var="disabledBtn" value="disabled" /></wiki:Permission>
        <wiki:Link cssClass="btn btn-xs btn-default ${disabledBtn}" page="${page}" context="upload" tabindex="0">
          <span class="icon-paper-clip"></span>
          <fmt:message key='edit.tab.attachments'/>
          <c:if test="${attachments > 0}"><span class="badge">${attachments}</span></c:if>
        </wiki:Link>
      </li>
      <li class="divider"></li>
      <li class="dropdown-header">
        <fmt:message key="info.readingtime">
            <fmt:param><fmt:formatNumber pattern="#.#" value="${readingTime}" /></fmt:param>
            <fmt:param>${wordCount}</fmt:param>
        </fmt:message>
      </li>
      <c:set var="keywords"><wiki:Variable var='keywords' default='' /></c:set>
      <c:if test="${!empty keywords}">
      <li class="dropdown-header">
        <fmt:message key="info.keywords">
            <fmt:param>${keywords}</fmt:param>
        </fmt:message>
      </li>
      </c:if>
    </ul>
  </li>
  </wiki:PageExists>
  </wiki:CheckRequestContext>


  <%-- edit --%>
  <wiki:PageType type="page">
  <wiki:CheckRequestContext context='view|info|diff|upload|rename'>
	<li id="edit"
	  class="<wiki:Permission permission='!edit'>disabled</wiki:Permission>">
      <wiki:PageType type="page">
        <wiki:Link context="edit" accessKey="e" >
          <span class="icon-pencil"></span>
          <span><fmt:message key='actions.edit'/></span>
        </wiki:Link>
      </wiki:PageType>
      <wiki:PageType type="attachment">
        <wiki:Link context="edit" page="<wiki:ParentPageName />" accessKey="e" >
          <span class="icon-pencil"></span>
          <span><fmt:message key='actions.edit'/></span>
        </wiki:Link>
      </wiki:PageType>
    </li>
  </wiki:CheckRequestContext>
  </wiki:PageType>


  <%-- help slimbox-link --%>
  <wiki:CheckRequestContext context='find'>
  <li>
    <a class="slimbox-link" href="<wiki:Link format='url' page='SearchPageHelp' ><wiki:Param name='skin' value='reader'/></wiki:Link>">
      <span class="icon-help-menu"></span>
      <span><fmt:message key="find.tab.help" /></span>
    </a>
  </li>
  </wiki:CheckRequestContext>
  <wiki:CheckRequestContext context='edit|comment'>
  <li>
    <a class="slimbox-link" href="<wiki:Link format='url' page='EditPageHelp' ></wiki:Link>">
      <span class="icon-help-menu"></span>
      <span><fmt:message key="edit.tab.help" /></span>
    </a>
    <%--
      <wiki:NoSuchPage page="EditPageHelp">
        <div class="error">
        <fmt:message key="comment.edithelpmissing">
        <fmt:param><wiki:EditLink page="EditPageHelp">EditPageHelp</wiki:EditLink></fmt:param>
        </fmt:message>
        </div>
      </wiki:NoSuchPage>
    --%>
  </li>
  </wiki:CheckRequestContext>
  <wiki:CheckRequestContext context='login'>
  <li>
    <a class="slimbox-link" href="<wiki:Link format='url' page='LoginHelp' ><wiki:Param name='skin' value='reader'/></wiki:Link>">
      <span class="icon-help-menu"></span>
      <span><fmt:message key="login.tab.help" /></span>
    </a>
  </li>
  <%--
  <wiki:NoSuchPage page="LoginHelp">
  <div class="error">
    <fmt:message key="login.loginhelpmissing">
       <fmt:param><wiki:EditLink page="LoginHelp">LoginHelp</wiki:EditLink></fmt:param>
    </fmt:message>
  </div>
  </wiki:NoSuchPage>
  --%>
  </wiki:CheckRequestContext>



  <%-- more menu --%>
  <li id="more" tabindex="0">
    <a href="#">
        <span class="icon-ellipsis-v"></span>
        <span><fmt:message key="actions.more"/></span>
        <span class="caret"></span>
    </a>
    <ul class="dropdown-menu pull-right" data-hover-parent="li">
      <wiki:PageExists>
      <wiki:CheckRequestContext context='view|info|diff|upload|preview' >

        <%-- VIEW RAW PAGE SOURCE --%>
        <li>
          <wiki:CheckVersion mode="latest">
            <wiki:Link cssClass="slimbox-link">
              <wiki:Param name='skin' value='raw'/>
              <fmt:message key='actions.rawpage' />
            </wiki:Link>
          </wiki:CheckVersion>
          <wiki:CheckVersion mode="notlatest">
            <wiki:Link cssClass="slimbox-link" version='${param.version}'>
              <wiki:Param name='skin' value='raw'/>
              <fmt:message key='actions.rawpage' />
            </wiki:Link>
          </wiki:CheckVersion>
        </li>

        <%-- Show Reader View --%>
        <li>
          <wiki:CheckVersion mode="latest">
            <wiki:Link cssClass="interwiki">
              <wiki:Param name='skin' value='reader'/>
              <fmt:message key='actions.showreaderview' />
            </wiki:Link>
          </wiki:CheckVersion>
          <wiki:CheckVersion mode="notlatest">
            <wiki:Link cssClass="interwiki" version="${param.version}">
              <wiki:Param name='skin' value='reader'/>
              <fmt:message key='actions.showreaderview' />
            </wiki:Link>
          </wiki:CheckVersion>
        </li>

      </wiki:CheckRequestContext>
      </wiki:PageExists>


      <%-- ADD COMMENT --%>
      <wiki:CheckRequestContext context='view|info|diff|upload'>
      <wiki:PageExists>
      <wiki:Permission permission="comment">
        <wiki:PageType type="page">
          <li>
            <wiki:Link context="comment">
              <span class="icon-plus"></span> <fmt:message key="actions.comment" />
            </wiki:Link>
          </li>
        </wiki:PageType>
        <wiki:PageType type="attachment">
          <li>
            <%--
            <wiki:Link page="<wiki:ParentPageName />" context="comment" title="<fmt:message key='actions.comment.title' />">
              <fmt:message key="actions.comment" />
            </wiki:Link>
            --%>
            <wiki:LinkToParent><fmt:message key="actions.addcommenttoparent" /></wiki:LinkToParent>
	      </li>
        </wiki:PageType>
      </wiki:Permission>
      </wiki:PageExists>
      </wiki:CheckRequestContext>

      <%-- WORKFLOW --%>
      <wiki:CheckRequestContext context='!workflow'>
      <wiki:UserCheck status="authenticated">
        <li>
          <wiki:Link jsp="Workflow.jsp">
            <fmt:message key='actions.workflow' />
          </wiki:Link>
        </li>
      </wiki:UserCheck>
      </wiki:CheckRequestContext>

      <%-- GROUPS : moved to the UserBox.jsp
      <wiki:CheckRequestContext context='!creategroup' >
      <wiki:Permission permission="createGroups">
        <li>
          <wiki:Link jsp="NewGroup.jsp" title="<fmt:message key='actions.creategroup.title'/>" >
            <fmt:message key='actions.creategroup' />
          </wiki:Link>
        </li>
      </wiki:Permission>
      </wiki:CheckRequestContext>
      --%>

      <%-- divider --%>
      <wiki:PageExists page="MoreMenu">

        <wiki:CheckRequestContext context='view|info|diff|upload|createGroup'>
	      <wiki:PageExists>
            <li class="divider "></li>
          </wiki:PageExists>
        </wiki:CheckRequestContext>

        <wiki:CheckRequestContext context='prefs|edit'>
          <wiki:UserCheck status="authenticated">
            <li class="divider "></li>
          </wiki:UserCheck>
        </wiki:CheckRequestContext>

        <li class="more-menu"><wiki:InsertPage page="MoreMenu" /></li>

      </wiki:PageExists>

    </ul>
  </li>

</ul>

</div>

<%--
  <wiki:PageExists>
  <wiki:PageType type="page">
  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a">
    <wiki:Include page="AttachmentTab.jsp"/>
  </wiki:Tab>
  </wiki:PageType>

  </wiki:PageExists>
--%>
