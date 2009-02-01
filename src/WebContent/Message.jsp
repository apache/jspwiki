<%@ page isErrorPage="true" %>
<%@ page import="org.apache.wiki.log.Logger" %>
<%@ page import="org.apache.wiki.log.LoggerFactory" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.action.*" %>
<%@ page import="org.apache.wiki.ui.stripes.*" %>
<%@ page import="org.apache.wiki.tags.WikiTagBase" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<%! 
    Logger log = LoggerFactory.getLogger("JSPWiki"); 
%>
<stripes:useActionBean beanclass="org.apache.wiki.action.MessageActionBean" id="wikiActionBean" />
<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    WikiContext wikiContext = WikiContextFactory.findContext( pageContext );
    MessageActionBean bean = (MessageActionBean)WikiInterceptor.findActionBean( request );

    request.setAttribute( "message", bean.getMessage() );

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );

%><wiki:Include page="<%=contentPage%>" />