<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    WikiEngine wiki;
%>

<%
    // Create wiki context; authorization check not needed
    WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW );
 
    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "CookieErrorTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />
