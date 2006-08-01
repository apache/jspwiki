<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
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
    if(!wikiContext.hasAccess( response )) return;
    String pagereq = wikiContext.getName();

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "UploadTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />

