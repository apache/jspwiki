<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.Date" %>
<%@ page import="com.ecyrd.jspwiki.ui.EditorManager" %>
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
    WikiContext wikiContext = wiki.createContext( request, WikiContext.PREVIEW );
    if(!wikiContext.hasAccess( response )) return;
    String pagereq = wikiContext.getName();

    pageContext.setAttribute( EditorManager.ATTR_EDITEDTEXT,
                              session.getAttribute( EditorManager.REQ_EDITEDTEXT ),
                              PageContext.REQUEST_SCOPE );

    long lastchange = 0;

    Date d = wikiContext.getPage().getLastModified();
    if( d != null ) lastchange = d.getTime();

    pageContext.setAttribute( "lastchange",
                              Long.toString( lastchange ),
                              PageContext.REQUEST_SCOPE );

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />

