<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="java.security.Principal" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.PagePermission" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="org.apache.commons.lang.time.StopWatch" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Logger log = Logger.getLogger("JSPWiki"); 
    WikiEngine wiki;

%><%
    StopWatch sw = new StopWatch();
    sw.start();
    WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW );
    String pagereq = wikiContext.getPage().getName();

    NDC.push( wiki.getApplicationName()+":"+pagereq );
    
    log.info("Request for page '"+pagereq+"' from "+request.getRemoteAddr()+
    		 " by "+(wikiContext.getCurrentUser() != null ? wikiContext.getCurrentUser().getName() : "unknown user") );

    String redirect = wiki.getRedirectURL( wikiContext );

    if( redirect != null )
    {
        response.sendRedirect( redirect );
        return;
    }

    AuthenticationManager authMgr = wiki.getAuthenticationManager();
    AuthorizationManager mgr = wiki.getAuthorizationManager();
    Principal currentUser  = wikiContext.getCurrentUser();
    WikiPage wikipage = wikiContext.getPage();

    if( !mgr.checkPermission( wikiContext.getWikiSession(),
                              new PagePermission( wikipage, "view" ) ) )
    {
        if( authMgr.strictLogins() )
        {
            log.info("User "+currentUser.getName()+" has no access - redirecting to login page.");
            String msg = "Unknown user or password.<br>Please try again.";
            session.setAttribute( "msg", msg );
            String pageurl = wiki.encodeName( pagereq );
            response.sendRedirect( wikiContext.getURL( WikiContext.LOGIN, pageurl ) );
            return;
        }
        else
        {
            //
            //  Do a bit of sanity check here.  FIXME: Should this be somewhere else?
            //
            if( pagereq.equals("LoginError") ) 
            {
                String msg = "Hmm. You don't have permission to view the page you asked for. "
                           + "Check your security policy to see if it is too restrictive. "
                           + "JSPWiki might also be having a problem verifying JAR signatures; "
                           + "check that your jspwiki.jks is in the same directory as the "
                           + "jspwiki.policy file, and that the JAR is, in fact, signed.";
                throw new WikiSecurityException( msg );
            }

            log.info("User "+currentUser+" has no access - displaying message.");
            response.sendRedirect( wiki.getViewURL("LoginError") );
        }
    }

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    //
    //  Alright, then start responding.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );

%><wiki:Include page="<%=contentPage%>" /><%

    sw.stop();
    if( log.isDebugEnabled() ) log.debug("Total response time from server on page "+pagereq+": "+sw);
    NDC.pop();
    NDC.remove();
%>

