<%-- Assumes that a variable called 'wiki' exists --%>

<%!static String LEFTMENUFOOTER_NAME = "LeftMenuFooter";%>

<!-- LeftMenuFooter is automatically generated from a Wiki page called "LeftMenuFooter" -->

<P>
<% 
    if( wiki.pageExists( LEFTMENUFOOTER_NAME ) ) 
    {
        WikiContext context       = new WikiContext( wiki, pagereq );
        WikiPage    requestedpage = new WikiPage( LEFTMENUFOOTER_NAME );
        out.println( wiki.getHTML(context,requestedpage) );
    }
    else
    {
        %>
        <HR><P>
        <P ALIGN="center">
        <I>No LeftMenuFooter!</I><BR>
        <A HREF="Edit.jsp?page=<%=LEFTMENUFOOTER_NAME%>">Please make one.</A><BR>
        </P>
        <P><HR>
        <%
    }
%>
</P>

<!-- End of automatically generated page -->

   <BR><BR><BR>
   <DIV ALIGN="left" CLASS="small">
   <%=Release.APPNAME%> v<%=Release.VERSTR%>
   </DIV>


