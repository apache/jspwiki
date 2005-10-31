<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
  <link rel="stylesheet" type="text/css" href="<wiki:BaseURL/>templates/<wiki:TemplateDir/>/jspwiki.css" />
  <script src="<wiki:BaseURL/>scripts/search_highlight.js" type="text/javascript"></script>
  <script src="<wiki:BaseURL/>scripts/jspwiki-common.js" type="text/javascript"></script>
  <meta http-equiv="Content-Type" content="text/html; charset=<wiki:ContentEncoding />" />
  <link rel="search" href="<wiki:LinkTo format="url" page="FindPage"/>"            title="Search <wiki:Variable var="ApplicationName" />" />
  <link rel="help"   href="<wiki:LinkTo format="url" page="TextFormattingRules"/>" title="Help" />
  <link rel="start"  href="<wiki:LinkTo format="url" page="Main"/>"                title="Front page" />
  <link rel="stylesheet" type="text/css" media="print" href="<wiki:BaseURL/>templates/<wiki:TemplateDir/>/jspwiki_print.css" />
  <link rel="alternate stylesheet" type="text/css" href="<wiki:BaseURL/>templates/<wiki:TemplateDir/>/jspwiki_print.css" title="Print friendly" />
  <link rel="alternate stylesheet" type="text/css" href="<wiki:BaseURL/>templates/<wiki:TemplateDir/>/jspwiki.css" title="Standard" />
  <link rel="icon" type="image/png" href="<wiki:BaseURL/>images/favicon.png" />
  <wiki:FeedDiscovery />

<%
  // cookie-format
  // skinname DELIM dateformat DELIM timezone DELIM editareaheight
  String DELIM  = "\u00a0"; //see brushed.js
  String prefSkinName = null;
  String prefDateFormat = "HH:mm dd-MMM-yyyy";
  String prefTimeZone = java.util.TimeZone.getDefault().getID();
  String prefEditAreaHeight = "24";

  Cookie[] cookies = request.getCookies();
  if (cookies != null)
  {
    for (int i = 0; i < cookies.length; i++)
    {
       if( "JSPWikiUserPrefs".equals( cookies[i].getName() ) )
       {
          String s = TextUtil.urlDecodeUTF8 (cookies[i].getValue() ) ;

          java.util.StringTokenizer st = new java.util.StringTokenizer (s, DELIM);

          if( st.hasMoreTokens() ) prefSkinName = st.nextToken();
          if( st.hasMoreTokens() ) prefDateFormat = st.nextToken();
          if( st.hasMoreTokens() ) prefTimeZone = st.nextToken();
          if( st.hasMoreTokens() ) prefEditAreaHeight = st.nextToken();

          break;
       }
    }
  }
  session.setAttribute("prefSkinName",       prefSkinName );
  session.setAttribute("prefDateFormat",     prefDateFormat );
  session.setAttribute("prefTimeZone",       prefTimeZone );
  session.setAttribute("prefEditAreaHeight", prefEditAreaHeight );

 %>

<%
    WikiContext context = (WikiContext) pageContext.getAttribute( "jspwiki.context",
                                                                  PageContext.REQUEST_SCOPE );
   
    TemplateManager mgr = context.getEngine().getTemplateManager();
    
    Set skins = mgr.listSkins(pageContext,context.getTemplate());

    for( Iterator i = skins.iterator(); i.hasNext(); )
    {
        String skinName = (String)i.next();
%>
        <link rel="alternate stylesheet" type="text/css" href="<wiki:BaseURL/>templates/<wiki:TemplateDir/>/skins/<%=skinName%>/skin.css" title="<%=skinName%>" /> 
<%
    }
%>

<% if(prefSkinName != null) { %>
 <link rel="stylesheet" type="text/css"
       href="<wiki:BaseURL/>templates/<wiki:TemplateDir/>/skins/<%= prefSkinName %>" />
<% } %>