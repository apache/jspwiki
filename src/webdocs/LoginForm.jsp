<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.action.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="/WEB-INF/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.LoginActionBean" />
<%! 
    /**
     * This page contains the logic for finding and including
       the correct login form, which is usually loaded from
       the template directory's LoginContent.jsp page.
       It should not be requested directly by users. If
       container-managed authentication is in force, the container
       will prevent direct access to it.
     */
    Logger log = LoggerFactory.getLogger("JSPWiki"); 

%>
<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Retrieve the Login page context, then go and find the login form

    WikiActionBean wikiContext = WikiActionBeanFactory.findActionBean( request );
    wikiContext.setVariable( "contentTemplate", "LoginContent.jsp" );
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );
                                                            
    log.debug("Login template content is: " + contentPage);
    
%><wiki:Include page="<%=contentPage%>" />