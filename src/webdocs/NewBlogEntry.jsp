<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.plugin.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;

%><%

    WikiContext wikiContext = wiki.createContext( request, WikiContext.EDIT );
    String pagereq = wikiContext.getPage().getName();

    NDC.push( wiki.getApplicationName()+":"+pagereq );
    
    String specialpage = wiki.getSpecialPageReference( pagereq );

    if( specialpage != null )
    {
        // FIXME: Do Something Else
        response.sendRedirect( specialpage );
        return;
    }

    WeblogEntryPlugin p = new WeblogEntryPlugin();
    
    String newEntry = p.getNewEntryPage( wiki, pagereq );

    NDC.pop();
    NDC.remove();

    response.sendRedirect( wiki.getEditURL(newEntry) );
%>

