<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.tags.InsertDiffTag" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    String getVersionText( int ver )
    {
        return ver > 0 ? ("version "+ver) : "current version";
    }

    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;
%>


<%
    String pagereq = wiki.safeGetParameter( request, "page" );
    String headerTitle = "";

    if( pagereq == null )
    {
        pagereq = "Main";
    }

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    String pageurl = wiki.encodeName( pagereq );    

    // If "r1" is null, then assume current version (= -1)
    // If "r2" is null, then assume the previous version (=current version-1)

    // FIXME: There is a set of unnecessary conversions here: InsertDiffTag
    //        does the String->int conversion anyway.

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
        int lastver = wiki.getVersion( pagereq );

        if( lastver > 1 )
        {
            ver2 = lastver-1;
        }
    }

    WikiPage wikipage = wiki.getPage( pagereq );

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext );

    pageContext.setAttribute( InsertDiffTag.ATTR_OLDVERSION,
                              new Integer(ver1) );
    pageContext.setAttribute( InsertDiffTag.ATTR_NEWVERSION,
                              new Integer(ver2) );

    String versionDescription1 = getVersionText( ver1 );
    String versionDescription2 = getVersionText( ver2 );

    log.debug("Request for page diff for '"+pagereq+"' from "+request.getRemoteHost()+" by "+request.getRemoteUser()+".  R1="+ver1+", R2="+ver2 );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>

<%@ include file="templates/default/DiffTemplate.jsp" %>

<%
    NDC.pop();
    NDC.remove();
%>
