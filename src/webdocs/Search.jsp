<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    Category log = Category.getInstance("JSPWikiSearch");
    WikiEngine wiki;
%>


<%
    String query = request.getParameter("query");
    Collection list = null;

    if( query != null )
    {
        log.info("Searching for string "+query);

        list = wiki.findPages( query );

        log.info("Found "+list.size()+" pages");
    }
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%> Search</TITLE>
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8">

  <TR>
    <TD CLASS="leftmenu" WIDTH="15%" VALIGN="top" NOWRAP="nowrap">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>
    <TD CLASS="page" WIDTH="85%" VALIGN="top">
      <H1>Find pages</H1>

      <% if( list != null ) 
      {
      %>
          <H4>Search results for '<%=query%>'</H4>

          <table border="0" cellpadding="4">

          <tr>
             <th width="30%" align="left">Page</th>
             <th align="left">Score</th>
          </tr>
          <%
          if( list.size() > 0 )
          {
              for( Iterator i = list.iterator(); i.hasNext(); )
              {
                  SearchResult pageref = (SearchResult) i.next();
                  %>
                  <TR>
                      <TD WIDTH="30%"><A HREF="Wiki.jsp?page=<%=wiki.encodeName(pageref.getName())%>"><%=pageref.getName()%></A></TD>
                      <TD><%=pageref.getScore()%></TD>
                  </TR>
                  <%
              }
           }
           else
           {
              %>
              <TR>
                  <TD width="30%"><B>No results</B></TD>
              </TR>
              <%
           }
          %>
          </table>
      <%
      }
      %>

      <P>
      
      <FORM action="Search.jsp">

      <INPUT type="text" name="query" size="40">

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
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>