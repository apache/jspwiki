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
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.ui.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="java.net.URLEncoder" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>

<wiki:TabbedSection>
<wiki:Tab id="findcontent" titleKey="find.tab" accesskey="s">

<s:form beanclass="org.apache.wiki.action.SearchActionBean" class="wikiform"
    id="searchform2" acceptcharset="UTF-8">
    
  <h4><fmt:message key="find.input" /></h4>
  <p>
    <s:text name="query" id="query2" size="32" />
    <s:checkbox name="details" id="details" />
    <fmt:message key="find.details" />

    <select name="scope" id="scope"> 
      <option value="" <c:if test="${empty param.scope}">selected="selected"</c:if> ><fmt:message key='find.scope.all' /></option>
      <option value="author:" <c:if test='${param.scope eq "author:"}'>selected="selected"</c:if> ><fmt:message key='find.scope.authors' /></option>
      <option value="name:" <c:if test='${param.scope eq "name:"}'>selected="selected"</c:if> ><fmt:message key='find.scope.pagename' /></option>
      <option value="contents:" <c:if test='${param.scope eq "contents:"}'>selected="selected"</c:if> ><fmt:message key='find.scope.content' /></option>
      <option value="attachment:" <c:if test='${param.scope eq "attachment:"}'>selected="selected"</c:if> ><fmt:message key='find.scope.attach' /></option>       
    </select>

    <s:submit name="search" id="ok" value="<fmt:message key='find.submit.find' />" />
    <s:submit name="go" id="go" value="<fmt:message key='find.submit.go' />" />
    <s:hidden name="start" id="start" value="0" />
    <s:hidden name="maxItems" id="maxitems" value="20" />

    <span id="spin" class="spin" style="position:absolute;display:none;"></span>
  </p>
</s:form>

<div id="searchResult2">
  <wiki:SearchResults>

    <h4><fmt:message key="find.heading.results"><fmt:param><c:out value="${wikiActionBean.query}" /></fmt:param></fmt:message></h4>
    <p>
      <fmt:message key="find.externalsearch" />
      <a class="external" href="http://www.google.com/search?q=<c:out value='${wikiActionBean.query}' />" title="Google Search '<c:out value='${wikiActionBean.query}' />'" target="_blank">Google</a><img class="outlink" src="images/out.png" alt="" />
      |     
      <a class="external" href="http://en.wikipedia.org/wiki/Special:Search?search=<c:out value='${wikiActionBean.query}' />" title="Wikipedia Search '<c:out value='${wikiActionBean.query}' />'" target="_blank">Wikipedia</a><img class="outlink" src="images/out.png" alt="" />
    </p>

    <wiki:SetPagination start="${wikiActionBean.start}" total="${wikiActionBean.resultsCount}" pagesize="20" maxlinks="9" fmtkey="info.pagination" onclick="$('start').value=%s; SearchBox.runfullsearch();" />

    <div class="graphBars">
      <div class="zebra-table">
        <table class="wikitable">
    
          <tr>
             <th align="left"><fmt:message key="find.results.page" /></th>
             <th align="left"><fmt:message key="find.results.score" /></th>
          </tr>
    
          <wiki:SearchResultIterator id="searchref" start="${wikiActionBean.start}" maxItems="${wikiActionBean.maxItems}">
          <tr>
            <td><wiki:LinkTo><wiki:PageName/></wiki:LinkTo></td>
            <td><span class="gBar"><%= searchref.getScore() %></span></td>
          </tr>
    
          <c:if test="${wikiActionBean.details == 'true'}">
  <%
            String[] contexts = searchref.getContexts();
            if( (contexts != null) && (contexts.length > 0) ) 
            {
  %>  
          <tr class="odd">
            <td colspan="2">
              <div class="fragment">
  <%
              for (int i = 0; i < contexts.length; i++) 
              {
  %>
                <%= (i > 0 ) ? "<span class='fragment_ellipsis'> ... </span>" : ""  %>
                <%= contexts[i]  %>
  <%
              }
  %>
               </div>
             </td>
           </tr>
  <% 
            }
  %>
          </c:if><%-- details --%>
        </wiki:SearchResultIterator>
    
        <wiki:IfNoSearchResults>
          <tr>
            <td class="nosearchresult" colspan="2"><fmt:message key="find.noresults" /></td>
          </tr>
        </wiki:IfNoSearchResults>
    
        </table>
      </div>
    </div>
    ${pagination}

  </wiki:SearchResults>
</div>

</wiki:Tab>

<wiki:PageExists page="SearchPageHelp">
<wiki:Tab id="findhelp" titleKey="find.tab.help" accesskey="h">
  <wiki:InsertPage page="SearchPageHelp" />
</wiki:Tab>
</wiki:PageExists>

</wiki:TabbedSection>