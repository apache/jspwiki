<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.Calendar,java.util.Date" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    Category log = Category.getInstance("JSPWiki");
    WikiEngine wiki;
%>


<%
    String pagereq = wiki.safeGetParameter( request, "page" );

    if( pagereq == null )
    {
        throw new ServletException("No page defined");
    }

    String pageurl = wiki.encodeName( pagereq );

    String action = request.getParameter("action");

    //
    //  Set the response type before we branch.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    log.debug("Request character encoding="+request.getCharacterEncoding());
    log.debug("Request content type+"+request.getContentType());

    if( action != null && action.equals("save") )
    {
        log.info("Saving page "+pagereq+". User="+request.getRemoteUser()+", host="+request.getRemoteHost() );

        //  FIXME: I am not entirely sure if the JSP page is the
        //  best place to check for concurrent changes.  It certainly
        //  is the best place to show errors, though.
       
        long pagedate   = Long.parseLong(request.getParameter("edittime"));
        Date change     = wiki.pageLastChanged( pagereq );

        if( change != null && change.getTime() != pagedate )
        {
            //
            // Someone changed the page while we were editing it!
            //

            log.info("Page changed, warning user.");

            pageContext.forward( "PageModified.jsp" );
            return;
        }

        wiki.saveText( pagereq,
                       wiki.safeGetParameter( request, "text" ),
                       request );

        response.sendRedirect("Wiki.jsp?page="+pageurl);
        return;
    }

    log.info("Editing page "+pagereq+". User="+request.getRemoteUser()+", host="+request.getRemoteHost() );

    //
    //  If the page does not exist, we'll get a null here.
    //
    long lastchange = 0;
    Date d = wiki.pageLastChanged( pagereq );
    if( d != null ) lastchange = d.getTime();

%>

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%> Edit: <%=pagereq%></TITLE>
  <META NAME="ROBOTS" CONTENT="NOINDEX">
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY class="edit" BGCOLOR="#FFD0FF" onLoad="document.forms[0].text.focus()">

<TABLE BORDER="0" CELLSPACING="8">

  <TR>
    <TD CLASS="leftmenu" WIDTH="15%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <A HREF="Wiki.jsp?page=TextFormattingRules">Help on editing</A>
       </P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>

    <TD CLASS="page" WIDTH="85%" VALIGN="top">
      <H1>Edit page <%=pagereq%></H1>

      <FORM action="Edit.jsp?page=<%=pageurl%>&action=save" method="POST" 
            ACCEPT-CHARSET="ISO-8859-1,UTF-8">

      <INPUT type="hidden" name="page" value="<%=pagereq%>">
      <INPUT type="hidden" name="action" value="save">
      <INPUT type="hidden" name="edittime" value="<%=lastchange%>">

      <TEXTAREA wrap="virtual" name="text" rows="25" cols="80"><%=wiki.getText(pagereq)%></TEXTAREA>

      <P>
      <input type="submit" name="ok" value="Save" />
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      <A HREF="Wiki.jsp?page=<%=pageurl%>">Cancel</A>
      <P>
      Here's a short reminder on what elements you have at your disposal:
      
      <table cellspacing = "4">
        <tr>
          <td>----</td><td>Horizontal ruler</td>
        </tr>
        <tr>
          <td>{{{, }}}</td><td>Begin/end code block.  (Recommended that you start on a new line.)</td>
        </tr>
        <tr>
          <td>\\</td><td>Forced line break</td>
        </tr>
        <tr>
          <td>[link]</td><td>Create hyperlink to "link", where "link"
          can be either an internal <A HREF="Wiki.jsp?page=WikiName">WikiName</A>
          or an external link (http://)</td>
        </tr>
        <tr>
          <td>[text|link]</td>
          <td>Create a hyperlink where the link text is different from the actual
          hyperlink link.</td>
        </tr>
        <tr>
          <td>[text|wiki:link]</td>
          <td>Create a hyperlink where the link text is different from the actual
          hyperlink link, and the hyperlink points to a named Wiki.
          This supports interWiki linking.</td>
        </tr>
        <tr>
          <td>*</td><td>Make a bulleted list (must be in first column).  Use more (**) for 
          deeper indentations.</td>
        </tr>

        <tr>
          <td>#</td><td>Make a numbered list (must be in first column). Use more (##, ###) for deeper indentations.</td>
        </tr>

        <tr>
          <td>!, !!, !!!</td>
          <td>Start a line with an exclamation mark (!) to make
          a heading.  More exclamation marks mean bigger headings.
          </td>
        </tr>

        <tr>
          <td>__text__</td><td>Makes text <B>bold</B>.</td>
        </tr>

        <tr>
          <td>''text''</td><td>Makes text in <I>italics</I>.</td>
        </tr>

        <tr>
          <td>{{text}}</td><td>Makes text in <TT>monospaced font</TT>.</TD>
        </tr>

      </table>

      <P>Don't try to use HTML, since it just won't work.</P>

      <P>To embed images just put them available on the web using one
      of the approved formats, and they will get inlined automatically.
      To see the list of approved formats, go check 
      <A HREF="Wiki.jsp?page=SystemInfo">SystemInfo</A>.</P>

      </FORM>

    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>