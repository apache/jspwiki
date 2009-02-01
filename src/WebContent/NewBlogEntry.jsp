<%@ page import="org.apache.wiki.log.Logger" %>
<%@ page import="org.apache.wiki.log.LoggerFactory" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.plugin.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="org.apache.wiki.action.EditActionBean" event="edit" id="wikiActionBean" />
<%! 
    Logger log = LoggerFactory.getLogger("JSPWiki"); 
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context; no need to check for authorization since the 
    // redirect will take care of that
    WikiContext wikiContext = wiki.createContext( request, WikiContext.EDIT );
    String pagereq = wikiContext.getPage().getName();
    
    // Redirect if the request was for a 'special page'
    String specialpage = wiki.getSpecialPageReference( pagereq );
    if( specialpage != null )
    {
        // FIXME: Do Something Else
        response.sendRedirect( specialpage );
        return;
    }

    WeblogEntryPlugin p = new WeblogEntryPlugin();
    
    String newEntry = p.getNewEntryPage( wiki, pagereq );

    // Redirect to a new page for user to edit
    response.sendRedirect( wiki.getEditURL(newEntry) );
%>

