<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

  <%
    WikiContext c = (WikiContext) pageContext.getAttribute( "jspwiki.context",
                                                         PageContext.REQUEST_SCOPE );
    String pagename = c.getPage().getName();
    String username = null;
    String showCal  = null;

    try {
          username = c.getEngine().getVariableManager().getValue(c, "username" );
          showCal  = c.getEngine().getVariableManager().getValue(c, "showCalendar" );
        } catch( Exception  e )  { /* dont care */ }
    if( username == null ) username = "";

    String myFav = username + "Favorites";
    boolean showCalendar = ( showCal != null ) ;
  %>


<div class="applicationlogo" align="center">
  <a href="<wiki:LinkTo page='SystemInfo' format='url'/>"
     onmouseover="document.fav_logo.src='<wiki:BaseURL/>images/jspwiki_logo.png'"
     onmouseout="document.fav_logo.src='<wiki:BaseURL/>images/jspwiki_logo_s.png'">
     <img src="<wiki:BaseURL/>images/jspwiki_logo_s.png"
          name="fav_logo" alt="JSPWiki logo" border="0"/>
  </a>
</div>

<div class="companylogo">
</div>

<%-- div class='myfavorites' --%>
<wiki:UserCheck status="known">
  <wiki:PageExists page="<%= myFav %>">
  <div class="myfavorites">
    <div class="boxtitle">
      <wiki:LinkTo page="<%= myFav %>" >My Favorites</wiki:LinkTo>
    </div>
    <wiki:InsertPage page="<%= myFav %>"/>
  </div>
  </wiki:PageExists>
</wiki:UserCheck>

<%-- calendar stuff : only shown when showCalendar variable is set --%>
<% if( showCalendar ) { %>
<div class="calendar">
  <div class="boxtitle">Calendar</div>
<wiki:Calendar pageformat="<%="'"+pagename+"_blogentry_'ddMMyy'_1'"%>"
               urlformat="'Wiki.jsp?page=%p&weblog.startDate='ddMMyy'&weblog.days=1'"
               monthurlformat="'Wiki.jsp?page=%p&weblog.startDate='ddMMyy'&weblog.days=%d'" />
</div>
<% } %>

<%-- LeftMenu is automatically generated from a Wiki page called "LeftMenu" --%>
<div class="leftmenu">
  <wiki:InsertPage page="LeftMenu" />
  <wiki:NoSuchPage page="LeftMenu">
    <p align="center">
    <wiki:EditLink page="LeftMenu">Please make a LeftMenu.</wiki:EditLink>
    </p>
  </wiki:NoSuchPage>
</div>

 <wiki:Include page="PageActions.jsp"/>

<div class="leftmenufooter">
  <wiki:InsertPage page="LeftMenuFooter" />
  <wiki:NoSuchPage page="LeftMenuFooter">
    <p align="center">
    <wiki:EditLink page="LeftMenuFooter">Please make a LeftMenuFooter.</wiki:EditLink>
    </p>
  </wiki:NoSuchPage>
</div>

<div class="wikiversion"><%=Release.APPNAME%> v<%=Release.getVersionString()%></div>

<div class="rssfeed"><wiki:RSSImageLink title="Aggregate the RSS feed" /></div>

<div style="clear:both; height:0px;" > </div>
