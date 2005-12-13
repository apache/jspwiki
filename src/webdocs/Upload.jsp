<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.attachment.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page errorPage="/Error.jsp" %>
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
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.UPLOAD );
    wikiContext.checkAccess( response );
    String pagereq = wikiContext.getPage().getName();
    NDC.push( wiki.getApplicationName() + ":" + pagereq );

    // Stash the wiki context
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "UploadTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" /><%
    // Clean up the logger and clear UI messages
    NDC.pop();
    NDC.remove();
    wikiContext.getWikiSession().clearMessages();
%>
