<%-- Assumes that a variable called 'wiki' exists --%>
<H3 CLASS="leftmenuheading"><A HREF="SystemInfo.jsp"><%=wiki.getApplicationName()%></A></H3>

<%!static String LEFTMENU_NAME = "LeftMenu";%>

<!-- LeftMenu is automatically generated from a Wiki page called "LeftMenu" -->

<P>
<% 
    WikiContext leftMenuContext = new WikiContext( wiki, pagereq );

    if( wiki.pageExists( LEFTMENU_NAME ) ) 
    { 
       WikiPage    requestedpage   = new WikiPage( LEFTMENU_NAME );
       out.println( wiki.getHTML(leftMenuContext,requestedpage) );
    }
%>

    <wiki:NoSuchPage page="LeftMenu">
        <HR><P>
        <P ALIGN="center">
        <I>No LeftMenu!</I><BR>
        <A HREF="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=LEFTMENU_NAME%>">Please make one.</A><BR>
        </P>
        <P><HR>
    </wiki:NoSuchPage>
</P>
<P>
<DIV ALIGN="center" CLASS="username">
<%
    String leftMenuUser = wiki.getUserName(request);
    if( leftMenuUser != null )
    {
        %>
        <B>G'day,</B><BR>
        <%=wiki.textToHTML( leftMenuContext, "["+leftMenuUser+"]" )%>
        <%
    }
    else
    {
        %><TT>
        Set your name in<BR>
        <%=wiki.textToHTML( leftMenuContext, "[UserPreferences]!" )%>
        </TT>
        <%
    }
%>
</DIV>

<!-- End of automatically generated page -->

