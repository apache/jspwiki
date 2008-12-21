<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.attachment.Attachment" %>
<%@ page import="org.apache.jspwiki.api.WikiPage" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.DeleteActionBean" event="delete" id="wikiActionBean" />

<%! 
    Logger log = LoggerFactory.getLogger("JSPWiki");
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.DELETE );
    String pagereq = wikiContext.getPage().getName();

    JCRWikiPage wikipage      = wikiContext.getPage();
    JCRWikiPage latestversion = wiki.getPage( pagereq );

    String delete = request.getParameter( "delete" );
    String deleteall = request.getParameter( "delete-all" );

    if( latestversion == null )
    {
        latestversion = wikiContext.getPage();
    }

    // If deleting an attachment, go to the parent page.
    String redirTo = pagereq;
    if( wikipage instanceof Attachment ) {
        redirTo = ((Attachment)wikipage).getParentName();
    }

    if( deleteall != null )
    {
        log.info("Deleting page "+pagereq+". User="+request.getRemoteUser()+", host="+request.getRemoteAddr() );

        wiki.deletePage( pagereq );
        response.sendRedirect(wiki.getViewURL(redirTo));
        return;
    }
    else if( delete != null )
    {
        log.info("Deleting a range of pages from "+pagereq);
        
        for( Enumeration params = request.getParameterNames(); params.hasMoreElements(); )
        {
    String paramName = (String)params.nextElement();
    
    if( paramName.startsWith("delver") )
    {
        int version = Integer.parseInt( paramName.substring(7) );
        
        JCRWikiPage p = wiki.getPage( pagereq, version );
        
        log.debug("Deleting version "+version);
        wiki.deleteVersion( p );
    }
        }
        
        response.sendRedirect(wiki.getURL( WikiContext.INFO, redirTo, null, false ));
        return; 
    }

    // Set the content type and include the response content
    // FIXME: not so.
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                    wikiContext.getTemplate(),
                                                    "EditTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />

