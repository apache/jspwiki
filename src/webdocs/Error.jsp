<%@ page isErrorPage="true" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Logger log = Logger.getLogger("JSPWiki"); 
    WikiEngine wiki;
%>
<%
    WikiContext wikiContext = wiki.createContext( request, 
                                                  WikiContext.ERROR );
    String pagereq = wikiContext.getName();

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    String msg = "An unknown error was caught by Error.jsp";

    Throwable realcause = null;

    if( exception != null )        
    {   
        msg = exception.getMessage();
        if( msg == null || msg.length() == 0 )
        {
            msg = "An unknown exception "+exception.getClass().getName()+" was caught by Error.jsp.";
        }

        //
        //  This allows us to get the actual cause of the exception.
        //  Note the cast; at least Tomcat has two classes called "JspException"
        //  imported in JSP pages.
        //


        if( exception instanceof javax.servlet.jsp.JspException )
        {
            log.debug("IS JSPEXCEPTION");
            realcause = ((javax.servlet.jsp.JspException)exception).getRootCause();
            log.debug("REALCAUSE="+realcause);
        }

        if( realcause == null ) realcause = exception;    
    }
    else
    {
        realcause = new Exception("Unknown general exception");
    }

    log.debug("Error.jsp exception is: ",exception);


    wikiContext.getWikiSession().addMessage( msg );
%>

   <h3>JSPWiki has detected an error</h3>

   <dl>
      <dt><b>Error Message</b></dt>
      <dd>
         <wiki:Messages div="error" />
      </dd>      
      <dt><b>Exception</b></dt>
      <dd><%=realcause.getClass().getName()%></dd>
      <dt><b>Place where detected</b></dt>
      <dd><%=FileUtil.getThrowingMethod(realcause)%></dd>
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
