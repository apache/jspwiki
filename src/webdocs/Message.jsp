<%@ page isErrorPage="true" %>
<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.action.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.stripes.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="/WEB-INF/stripes.tld" prefix="stripes" %>
<%! 
    Logger log = LoggerFactory.getLogger("JSPWiki"); 
%>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.MessageActionBean" />
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