<%-- Assumes that a variable called 'wiki' exists --%>
<H3><A HREF="SystemInfo.jsp"><%=wiki.getApplicationName()%></A></H3>

<%!static String LEFTMENU_NAME = "LeftMenu";%>

<!-- LeftMenu is automatically generated from a Wiki page called "LeftMenu" -->

<P>
<% 
    if( wiki.pageExists( LEFTMENU_NAME ) ) 
    {
        out.println( wiki.getHTML(LEFTMENU_NAME) );
    }
    else
    {
        %>
        <HR><P>
        <P ALIGN="center">
        <I>No LeftMenu!</I><BR>
        <A HREF="Edit.jsp?page=<%=LEFTMENU_NAME%>">Please make one.</A><BR>
        </P>
        <P><HR>
        <%
    }
%>
</P>

<!-- End of automatically generated page -->

