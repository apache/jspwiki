<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.admin.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.TemplateManager" %>
<%@ page import="org.apache.commons.lang.time.StopWatch" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    Logger log = LoggerFactory.getLogger("JSPWiki"); 
%>
<%
    String bean = request.getParameter("bean");
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.ADMIN );
   
    
    //
    //  This is an experimental feature, so we will turn it off unless the
    //  user really wants to.
    //
    if( !TextUtil.isPositive(wiki.getWikiProperties().getProperty("jspwiki-x.adminui.enable")) )
    {
        %>
        <html>
        <body>
           <h1>Disabled</h1>
           <p>JSPWiki admin UI has been disabled.  This is an experimental feature, and is
           not guaranteed to work.  You may turn it on by specifying</p>
           <pre>
               jspwiki-x.adminui.enable=true
           </pre>
           <p>in your <tt>jspwiki.properties</tt> file.</p>
           <p>Have a nice day.  Don't forget to eat lots of fruits and vegetables.</p>
        </body>
        </html>
        <%
        return;
    }

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "admin/AdminTemplate.jsp" );
    
    pageContext.setAttribute( "engine", wiki, PageContext.REQUEST_SCOPE );
    pageContext.setAttribute( "context", wikiContext, PageContext.REQUEST_SCOPE );

    if( request.getMethod().equalsIgnoreCase("post") && bean != null )
    {
        AdminBean ab = wiki.getAdminBeanManager().findBean( bean );
        
        if( ab != null )
        {
            ab.doPost( wikiContext );
        }
        else
        {
            wikiContext.getWikiSession().addMessage( "No such bean "+bean+" was found!" );
        }
    }
    
%><wiki:Include page="<%=contentPage%>" />