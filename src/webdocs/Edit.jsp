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
    String pagereq = request.getParameter("page");

    if( pagereq == null )
    {
        throw new ServletException("No page defined");
    }

    String action = request.getParameter("action");

    if( action != null && action.equals("save") )
    {
        log.info("Saving page "+pagereq);

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

            // FIXME: This is put into session, but it is probably
            //        a better idea if it's stored in request, but since
            //        the request can be long, we need to POST it.

            request.getSession().setAttribute("usertext",request.getParameter("text"));

            response.sendRedirect("PageModified.jsp?page="+pagereq);
            return;
        }

        wiki.saveText( pagereq, request.getParameter("text") );

        response.sendRedirect("Wiki.jsp?page="+pagereq);
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
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY BGCOLOR="#FFD0FF">

<TABLE BORDER="0" CELLSPACING="8">

  <TR>
    <TD WIDTH="15%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <A HREF="Wiki.jsp?page=TextFormattingRules">Help on editing</A>
       </P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>
    <TD WIDTH="85%" VALIGN="top">
      <H1>Edit page <%=pagereq%></H1>

      <FORM action="Edit.jsp?page=<%=pagereq%>&action=save" method="POST">

      <INPUT type="hidden" name="page" value="<%=pagereq%>">
      <INPUT type="hidden" name="action" value="save">
      <INPUT type="hidden" name="edittime" value="<%=lastchange%>">

      <TEXTAREA name="text" rows="25" cols="80"><%=wiki.getText(pagereq)%></TEXTAREA>

      <P>
      <input type="submit" name="ok" value="Save" />
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      <A HREF="Wiki.jsp?page=<%=pagereq%>">Cancel</A>
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
          <td>__text__</td><td>Makes text <B>bold</B>.</td>
        </tr>

        <tr>
          <td>''text''</td><td>Makes text in <I>italics</I>.</td>
        </tr>

        <tr>
          <td>{{text}}</td><td>Makes text in <TT>monospaced font</TT>.</TD>
        </tr>

      </table>

      Don't try to use HTML, since it just won't work.

      </FORM>

    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>