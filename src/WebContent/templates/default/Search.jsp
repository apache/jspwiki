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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.ui.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page errorPage="/Error.jsp" %>
<s:layout-render name="${templates['layout/DefaultLayout.jsp']}">
  <s:layout-component name="content">
    <wiki:TabbedSection>
      
      <%-- Search tab --%>
      <wiki:Tab id="findcontent" titleKey="find.tab" accesskey="s">
        <s:form beanclass="org.apache.wiki.action.SearchActionBean" class="wikiform"
            id="searchform2" acceptcharset="UTF-8">
            
          <h4><fmt:message key="find.input" /></h4>
          <p>
            <%-- Search query --%>
            <s:text name="query" id="query2" size="32" />
            <s:checkbox name="details" id="details" />
            <fmt:message key="find.details" />
        
            <%-- Search scope --%>
            <s:select id="scope" name="${param.scope}"> 
              <s:options-enumeration enum="org.apache.wiki.action.SearchActionBean.SearchScope" label="name" />
            </s:select>
        
            <%-- Submit buttons --%>
            <s:submit name="search" id="ok" />    
            <s:submit name="go" id="go" />
            <s:hidden name="start" id="start" value="0" />
            <s:hidden name="maxItems" id="maxitems" value="20" />
        
            <span id="spin" class="spin" style="position:absolute;display:none;"></span>
          </p>
        </s:form>
        
        <div id="searchResult2">
          <wiki:SearchResults>
        
            <h4><fmt:message key="find.heading.results">
              <fmt:param><c:out value="${wikiActionBean.query}" /></fmt:param>
            </fmt:message></h4>
            <p>
              <fmt:message key="find.externalsearch" />
              <a class="external" href="http://www.google.com/search?q=<c:out value='${wikiActionBean.query}' />" title="Google Search '<c:out value='${wikiActionBean.query}' />'" target="_blank">Google</a><img class="outlink" src="${templates['images/out.png']}" alt="" />
              |     
              <a class="external" href="http://en.wikipedia.org/wiki/Special:Search?search=<c:out value='${wikiActionBean.query}' />" title="Wikipedia Search '<c:out value='${wikiActionBean.query}' />'" target="_blank">Wikipedia</a><img class="outlink" src="${templates['images/out.png']}" alt="" />
            </p>
        
            <wiki:SetPagination start="${wikiActionBean.start}" total="${fn:length(wikiActionBean.results)}"
              pagesize="20" maxlinks="9" fmtkey="info.pagination" onclick="$('start').value=%s; SearchBox.runfullsearch();" />
        
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
              
                    <c:if test="${wikiActionBean.details == 'true'}"><%
          
                      String[] contexts = searchref.getContexts();
                      if( (contexts != null) && (contexts.length > 0) ) 
                      { %>
                      <tr class="odd">
                        <td colspan="2">
                          <div class="pre"><%
                          
                          for (int i = 0; i < contexts.length; i++) 
                          { %>
                            <%= (i > 0 ) ? "<span class='ellipsis'> ... </span>" : "" %><%= contexts[i] %>
                       <% } %>
                          </div>
                        </td>
                      </tr> <%
                      } %>
                    </c:if><%-- details --%>
                  </wiki:SearchResultIterator>
                
                  <%-- If no results --%>
                  <c:if test="${fn:length(wikiActionBean.results) == 0}">
                    <tr>
                      <td class="nosearchresult" colspan="2"><fmt:message key="find.noresults" /></td>
                    </tr>
                  </c:if>
            
                </table>
              </div>
            </div>
            ${pagination}
        
          </wiki:SearchResults>
        </div>
      </wiki:Tab>
        
      <%-- Help tab --%>
      <wiki:PageExists page="SearchPageHelp">
        <wiki:Tab id="findhelp" titleKey="find.tab.help" accesskey="h">
          <wiki:InsertPage page="SearchPageHelp" />
        </wiki:Tab>
      </wiki:PageExists>

    </wiki:TabbedSection>
  </s:layout-component>
</s:layout-render>

