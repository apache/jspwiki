<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.net.URLEncoder" %>

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

      <h2>Find pages</h2>

      <wiki:SearchResults>
          <h4>Search results for '<%=query%>'</h4>

          <p>
          <i>Found <wiki:SearchResultsSize/> hits, here are the results from <%=startVal+1%> to <%=endVal%>.</i>
          </p>

          <div class="zebra-table">
          <div class="graphBars">
          <table border="0" cellpadding="4" width="80%">

          <tr>
             <th align="left">Page</th>
             <th align="left">Score</th>
          </tr>

          <wiki:SearchResultIterator id="searchref" start="<%=Integer.toString(startVal)%>" maxItems="20">
              <tr>
                  <td><wiki:LinkTo><wiki:PageName/></wiki:LinkTo>
<%
    String[] contexts = searchref.getContexts();
    if ((contexts != null) && (contexts.length > 0)) {
      out.println("<br />");
      out.println("<div class=\"fragment\">");
      for (int i = 0; i < contexts.length; i++) {
        if (i > 0)
          out.println("<span class=\"fragment_ellipsis\"> ... </span>");
        out.println(contexts[i]);
      }
      out.println("</div>");
    }
%>

</td>
                  <td class="gBar"><%=searchref.getScore()%></td>
              </tr>
          </wiki:SearchResultIterator>

          <wiki:IfNoSearchResults>
              <tr>
                  <td colspan="2"><b>No results</b></td>
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
              Get previous <%=prevSize%> results
          </wiki:Link>.
          <% } %>
          
          <% if( endVal < list.size() ) { %>
          <wiki:Link jsp="Search.jsp">
              <wiki:Param name="query" value="<%=URLEncoder.encode(query)%>"/>
              <wiki:Param name="start" value="<%=Integer.toString(endVal)%>"/>
              Get next <%=nextSize%> results
          </wiki:Link>.
          <% } %>
          </p>
          <p>
          <a href="http://www.google.com/search?q=<%=query%>" target="_blank">Try this same search on Google!</a>
          </p>
          <hr />
      </wiki:SearchResults>

      <form action="<wiki:Link format="url" jsp="Search.jsp"/>"
            accept-charset="<wiki:ContentEncoding/>">

      <p>
      Enter your query here:<br />
      <input type="text" name="query" size="40" value="<%=query%>" /></p>

      <p>
      <input type="submit" name="ok" value="Find!" />
      <input type="submit" name="go" value="Go!" /></p>
      </form>

      <wiki:InsertPage page="SearchPageHelp"/>

