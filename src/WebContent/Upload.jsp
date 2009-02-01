<%@ page import="org.apache.wiki.log.Logger" %>
<%@ page import="org.apache.wiki.log.LoggerFactory" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="org.apache.wiki.action.UploadActionBean" event="upload" id="wikiActionBean" />
<%! 
    Logger log = LoggerFactory.getLogger("JSPWiki"); 
%>

<% 
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.UPLOAD );
    String pagereq = wikiContext.getPage().getName();

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "UploadTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />

