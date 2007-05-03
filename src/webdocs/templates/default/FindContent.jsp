<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="java.net.URLEncoder" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>

<%-- Make AJAX driven --%>
<%-- FIXME: Get rid of the scriptlets. --%>
<%
    String query = (String)pageContext.getAttribute( "query",
                                                     PageContext.REQUEST_SCOPE );
    if( query == null ) query = "";
    
    String start = (String)request.getParameter("start");
    
    int startVal = 0;
    
    try
    {
        startVal = Integer.parseInt(start);
    } catch(Exception e) {}
    
    if( startVal < 0 ) startVal = 0;
    
    int endVal = startVal + 20;
    
    Collection list = (Collection)pageContext.getAttribute( "searchresults",
                                                             PageContext.REQUEST_SCOPE );
                                                             
    int prevSize = 0, nextSize = 0;
    
    if( list != null )
    {
      if( endVal > list.size() ) endVal = list.size();
      prevSize = Math.max( startVal, 20 );
      nextSize = Math.min(list.size() - endVal, 20);
    }
%>

<wiki:TabbedSection>
<wiki:Tab id="findquery" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "find.tab")%>" accesskey="s">

<form action="<wiki:Link format='url' jsp='Search.jsp'/>"
       class="wikiform"
        name="searchform2" id="searchform2"
         accept-charset="<wiki:ContentEncoding/>">

      <p><label for="query"><fmt:message key="find.input" /></label></p>
      <input type="text" name="query" id="query2" size="40" value="<%=query%>" />
      <input type="submit" name="ok" value="<fmt:message key="find.submit.find"/>" />
      <input type="submit" name="go" value="<fmt:message key="find.submit.go"/>" />
      
      </form>

      <wiki:SearchResults>
          <h4><fmt:message key="find.heading.results"><fmt:param><%=query%></fmt:param></fmt:message></h4>

          <p>
          <i><fmt:message key="find.resultsstart">
             <fmt:param><wiki:SearchResultsSize/></fmt:param>
             <fmt:param><%=startVal+1%></fmt:param>
             <fmt:param><%=endVal%></fmt:param>
             </fmt:message>
          </p>

          <div class="zebra-table">
          <div class="graphBars">
          <table class="wikitable" xwidth="80%">

          <tr>
             <th align="left"><fmt:message key="find.results.page"/></th>
             <th align="left"><fmt:message key="find.results.score"/></th>
          </tr>

          <wiki:SearchResultIterator id="searchref" start="<%=Integer.toString(startVal)%>" maxItems="20">
              <tr>
                  <td><wiki:LinkTo><wiki:PageName/></wiki:LinkTo></td>
                  <td><span class="gBar"><%=searchref.getScore()%></span></td>
              </tr>
<%
    String[] contexts = searchref.getContexts();
    if ((contexts != null) && (contexts.length > 0)) {
%>
            <tr>
            <td colspan="2"><div class="fragment">
<%    for (int i = 0; i < contexts.length; i++) {
        if (i > 0)
          out.println("<span class=\"fragment_ellipsis\"> ... </span>");
        out.println(contexts[i]);
      }
%>
            </div></td>
          </tr>
<%
     }
%>

          </wiki:SearchResultIterator>

          <wiki:IfNoSearchResults>
              <tr>
                  <td colspan="2"><b><fmt:message key="find.noresults"/></b></td>
              </tr>
          </wiki:IfNoSearchResults>

          </table>
          </div>
          </div>
          <p>
          <% if( startVal > 0 ) { %>
          <wiki:Link jsp="Search.jsp">
              <wiki:Param name="query" value="<%=URLEncoder.encode(query)%>"/>
              <wiki:Param name="start" value="<%=Integer.toString(startVal-prevSize)%>"/>
              <fmt:message key="find.getprevious">
                 <fmt:param><%=prevSize%></fmt:param>
              </fmt:message>
          </wiki:Link>.
          <% } %>
          
          <% if( endVal < list.size() ) { %>
          <wiki:Link jsp="Search.jsp">
              <wiki:Param name="query" value="<%=URLEncoder.encode(query)%>"/>
              <wiki:Param name="start" value="<%=Integer.toString(endVal)%>"/>
              <fmt:message key="find.getnext">
                 <fmt:param><%=nextSize%></fmt:param>
              </fmt:message>
          </wiki:Link>.
          <% } %>
          </p>
          <p>
          <a href="http://www.google.com/search?q=<%=query%>" target="_blank"><fmt:message key="find.googleit"/></a>
          </p>
      </wiki:SearchResults>
</wiki:Tab>

<wiki:PageExists page="SearchPageHelp">
<wiki:Tab id="findhelp"  title="Help" accesskey="h">
  <wiki:InsertPage page="SearchPageHelp"/>
</wiki:Tab>
</wiki:PageExists>

</wiki:TabbedSection>