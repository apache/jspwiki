<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

      <%-- headerTitle is something that is set by each main JSP page.
           It tells in which context we are now (Edit, PageInfo, etc.) --%>

      <TABLE WIDTH="100%" CELLSPACING="0" CELLPADDING="0" BORDER="0">
         <TR>
            <TD align="left">
                <H1 CLASS="pagename"><%=headerTitle%><wiki:PageName/></H1></TD>
            <TD align="right">
              <FORM action="<%=wiki.getBaseURL()%>Search.jsp"
                    ACCEPT-CHARSET="ISO-8859-1,UTF-8">
               <wiki:LinkTo page="FindPage">Search Wiki:</wiki:LinkTo>
               <INPUT type="text" name="query" size="15">
               <INPUT type="submit" name="ok" value="Find!">
              </FORM>
            </TD>
         </TR>
      </TABLE>

      <HR><P>

