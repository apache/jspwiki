<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.Collection" %>

<%-- FIXME: Get rid of the scriptlets. --%>
<%
    Collection list = (Collection)pageContext.getAttribute( "searchresults",
                                                             PageContext.REQUEST_SCOPE );

    String query = (String)pageContext.getAttribute( "query",
                                                     PageContext.REQUEST_SCOPE );
    if( query == null ) query = "";
%>

      <H2>Find pages</H2>

      <% if( list != null ) 
      {
      %>
          <H4>Search results for '<%=query%>'</H4>

          <P>
          <I>Found <%=list.size()%> hits, here are the top 20.</I>
          </P>

          <table border="0" cellpadding="4">

          <tr>
             <th width="30%" align="left">Page</th>
             <th align="left">Score</th>
          </tr>          
          <% if( list.size() > 0 ) { %>
              <wiki:SearchResultIterator list="<%=list%>" id="searchref" maxItems="20">
                  <TR>
                      <TD WIDTH="30%"><wiki:LinkTo><wiki:PageName/></wiki:LinkTo></TD>
                      <TD><%=searchref.getScore()%></TD>
                  </TR>
              </wiki:SearchResultIterator>
          <% } else { %>
              <TR>
                  <TD width="30%"><B>No results</B></TD>
              </TR>
          <% } %>

          </table>
          <P>
          <A HREF="http://www.google.com/search?q=<%=query%>" TARGET="_blank">Try this same search on Google!</A>
          </P>
          <P><HR></P>
      <%
      }
      %>

      <P>

      <FORM action="<wiki:Variable var="jspwiki.baseURL"/>Search.jsp"
            ACCEPT-CHARSET="ISO-8859-1,UTF-8">

      Enter your query here:<BR>
      <INPUT type="text" name="query" size="40" value="<%=query%>">

      <P>
      <input type="submit" name="ok" value="Find!" />
      </FORM>

      <P>
      Use '+' to require a word, '-' to forbid a word.  For example:

      <pre>
          +java -emacs jsp
      </pre>

      finds pages that MUST include the word "java", and MAY NOT include
      the word "emacs".  Also, pages that contain the word "jsp" are
      ranked before the pages that don't.
      <P>
      All searches are case insensitive.  If a page contains both
      forbidden and required keywords, it is not shown.
