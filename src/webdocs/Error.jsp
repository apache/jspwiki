<%@ page isErrorPage="true" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
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
    String skin    = wiki.safeGetParameter( request, "skin" );

    if( pagereq == null )
    {
        pagereq = wiki.getFrontPage();
    }

    if( skin == null )
    {
        skin = wiki.getTemplateDir();
    }

    NDC.push( wiki.getApplicationName() + ":" + pagereq );

    WikiContext wikiContext = new WikiContext( wiki, pagereq );
    wikiContext.setRequestContext( WikiContext.ERROR );
    wikiContext.setHttpRequest( request );

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT, wikiContext, PageContext.REQUEST_SCOPE );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    String msg = "An unknown error was caught by Error.jsp";
    if( exception != null )        
    {   
        msg = exception.getMessage();
        if( msg == null || msg.length() == 0 )
        {
            msg = "An unknown exception "+exception.getClass().getName()+" was caught by Error.jsp.";
        }
    }

    log.debug("Error.jsp exception is: ",exception);

    pageContext.setAttribute( "message", msg, PageContext.REQUEST_SCOPE );

    String contentPage = "templates/"+skin+"/ViewTemplate.jsp";
%>

   <h3>JSPWiki has detected an error</h3>

   <dl>
      <dt><b>Error Message</b></dt>
      <dd>
         <%=pageContext.getAttribute("message",PageContext.REQUEST_SCOPE)%>
      </dd>      
      <dt><b>Exception</b></dt>
      <dd><%=exception.getClass().getName()%></dd>
      <dt><b>Place where detected</b></dt>
      <dd><%=FileUtil.getThrowingMethod(exception)%></dd>
   </dl>
   <p>
   If you have changed the templates, please do check them.  This error message
   may show up because of that.  If you have not changed them, and you are
   either installing JSPWiki for the first time or have changed configuration,
   then you might want to check your configuration files.  If you are absolutely sure
   that JSPWiki was running quite okay or you can't figure out what is going
   on, then by all means, come over to <a href="http://www.jspwiki.org/">jspwiki.org</a>
   and tell us.  There is more information in the log file (like the full stack trace, 
   which you should add to any error report).
   </p>
   <p>
   And don't worry - it's just a computer program.  Nothing really
   serious is probably going on: at worst you can lose a few nights
   sleep.  It's not like it's the end of the world.
   </p>

   <br clear="all" />
<%
    NDC.pop();
    NDC.remove();
%>

