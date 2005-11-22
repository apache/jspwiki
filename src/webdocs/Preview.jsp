<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.Date" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.editor.EditorManager" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;
%>
<%
    WikiContext wikiContext = wiki.createContext( request, WikiContext.PREVIEW );
    String pagereq = wikiContext.getPage().getName();

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    pageContext.setAttribute( EditorManager.ATTR_EDITEDTEXT,
                              request.getParameter( EditorManager.REQ_EDITEDTEXT ),
                              PageContext.REQUEST_SCOPE );

    long lastchange = 0;

    Date d = wikiContext.getPage().getLastModified();
    if( d != null ) lastchange = d.getTime();

    pageContext.setAttribute( "lastchange",
                              Long.toString( lastchange ),
                              PageContext.REQUEST_SCOPE );

    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );
%>
<wiki:Include page="<%=contentPage%>" />
<%
    NDC.pop();
    NDC.remove();
%>

