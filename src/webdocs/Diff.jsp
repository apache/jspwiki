<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.util.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.InsertDiffTag" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.EditActionBean" event="diff" id="wikiActionBean" />

<%! 
    Logger log = LoggerFactory.getLogger("JSPWiki"); 
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.DIFF );
    String pagereq = wikiContext.getPage().getName();

    WatchDog w = wiki.getCurrentWatchDog();
    try
    {
    w.enterState("Generating INFO response",60);
    
    // Notused ? 
    // String pageurl = wiki.encodeName( pagereq );

    // If "r1" is null, then assume current version (= -1)
    // If "r2" is null, then assume the previous version (=current version-1)

    // FIXME: There is a set of unnecessary conversions here: InsertDiffTag
    //        does the String->int conversion anyway.

    JCRWikiPage wikipage = wikiContext.getPage();

    String srev1 = request.getParameter("r1");
    String srev2 = request.getParameter("r2");

    int ver1 = -1, ver2 = -1;

    if( srev1 != null )
    {
        ver1 = Integer.parseInt( srev1 );
    }

    if( srev2 != null )
    {
        ver2 = Integer.parseInt( srev2 );
    }
    else
    {
        int lastver = wikipage.getVersion();

        if( lastver > 1 )
        {
    ver2 = lastver-1;
        }
    }

    pageContext.setAttribute( InsertDiffTag.ATTR_OLDVERSION,
                      new Integer(ver1),
                      PageContext.REQUEST_SCOPE );
    pageContext.setAttribute( InsertDiffTag.ATTR_NEWVERSION,
                      new Integer(ver2),
                      PageContext.REQUEST_SCOPE );

    // log.debug("Request for page diff for '"+pagereq+"' from "+request.getRemoteAddr()+" by "+request.getRemoteUser()+".  R1="+ver1+", R2="+ver2 );

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                    wikiContext.getTemplate(),
                                                    "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />
<% } finally { w.exitState(); } %>