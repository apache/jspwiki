<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>

<%! 
    Category log = Category.getInstance("JSPWikiSearch");
    WikiEngine wiki = new WikiEngine(); 
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

<HTML>

<HEAD>
  <TITLE>JSPWiki Search</TITLE>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8">

  <TR>
    <TD WIDTH="15%" VALIGN="top">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <A HREF="Wiki.jsp?page=HelpOnSearching">Help on searching</A>
       </P>
    </TD>
    <TD WIDTH="85%" VALIGN="top">
      <H1>Find pages</H1>

      <% if( list != null ) 
      {
      %>
          <H4>Search results for '<%=query%>'</H4>

          <table border="0" cellpadding="4">
          <%
          if( list.size() > 0 )
          {
              for( Iterator i = list.iterator(); i.hasNext(); )
              {
                  String pageref = (String) i.next();
                  %>
                  <TR>
                      <TD WIDTH="30%"><A HREF="Wiki.jsp?page=<%=pageref%>"><%=pageref%></A></TD>
                      <TD>100%</TD>
                  </TR>
                  <%
              }
           }
           else
           {
              %>
              <TR>
                  <TD><B>No results</B></TD>
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

      <INPUT type="text" name="query">

      <P>
      <input type="submit" name="ok" value="Find!" />
      </FORM>

    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>