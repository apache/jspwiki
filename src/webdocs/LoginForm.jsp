<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    /**
     * This page contains the logic for finding and including
       the correct login form, which is usually loaded from
       the template directory's LoginContent.jsp page.
       It should not be requested directly by users. If
       container-managed authentication is in force, the container
       will prevent direct access to it.
     */
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Logger log = Logger.getLogger("JSPWiki"); 
    WikiEngine wiki;
%>

<%
    // Retrieve the Login page context, then go and find the login form

    WikiContext wikiContext = (WikiContext) pageContext.getAttribute( WikiTagBase.ATTR_CONTEXT, PageContext.REQUEST_SCOPE );
    
    // If no context, it means we're using container auth.  So, create one anyway
    if ( wikiContext == null )
    {
        wikiContext = wiki.createContext( request, WikiContext.LOGIN );
        pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                                  wikiContext,
                                  PageContext.REQUEST_SCOPE );
    }
    
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );
                                                            
    log.debug("Login template content is: " + contentPage);
%>
    <wiki:Include page="<%=contentPage%>" />
