<%@ page isErrorPage="true" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.action.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="/WEB-INF/stripes.tld" prefix="stripes" %>
<%! 
    Logger log = Logger.getLogger("JSPWiki"); 
%>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.MessageActionBean"/>
<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    MessageActionBean wikiContext = (MessageActionBean)WikiActionBeanFactory.findActionBean( request );

    request.setAttribute( "message", wikiContext.getMessage() );

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );

%><wiki:Include page="<%=contentPage%>" />