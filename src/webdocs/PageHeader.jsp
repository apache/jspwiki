<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

      <TABLE WIDTH="100%" CELLSPACING="0" CELLPADDING="0" BORDER="0">
         <TR>
            <TD align="left"><H1 CLASS="pagename"><%=pagereq%></H1></TD>
            <TD align="right">
              <FORM action="Search.jsp">
               <A HREF="Search.jsp">Search Wiki:</A>
               <INPUT type="text" name="query" size="15">
               <INPUT type="submit" name="ok" value="Find!">
              </FORM>
            </TD>
         </TR>
      </TABLE>

      <HR><BR>
