<%@ page import="java.util.*,com.ecyrd.jspwiki.*" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="java.text.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.progress.*" %>
<%@ page import="com.ecyrd.jspwiki.util.*" %>
<%@ taglib uri="/WEB-INF/oscache.tld" prefix="oscache" %>
<%!
    Logger log = Logger.getLogger("JSPWiki");
%>
<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, "view" );
    if(!wikiContext.hasAccess( response )) return;

    String progressId = request.getParameter("id");

    ProgressManager pm = wiki.getProgressManager();

    response.setContentType("text/html; charset=UTF-8");

    int progress = 0;

    try
    {
       progress = pm.getProgress( progressId );
    }
    catch( IllegalArgumentException e )
    {
        //
        //  There is no such upload yet.  Attempt to wait for 1 second, and if there still
        //  is no such thing, just die and close.
        //
        try
        {
            Thread.sleep(1000L);

            progress = pm.getProgress( progressId );
        }
        catch( Exception ex )
        {
            %>
               <html><body onload="javascript:window.close();"></body></html>
            <%
        }
    }

    log.info("ID="+progressId+", progress="+progress);
%>
 <html>
 <head>
 <meta http-equiv="refresh" content="1">
 </head>
 <body>
 <p style="font-size:200%; background:yellow;">
 <%
    for( int i = 0; i < progress/5; i++ )
    {
        out.print('+');
    }
    for( int i = progress/5; i < 20; i++ )
    {
        out.print('-');
    }
 %>
 </p>
 </body>
 </html>