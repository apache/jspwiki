<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

  <%
    //  Determine the name for the user's favorites page
    WikiContext c = WikiContext.findContext( pageContext );
    String pagename = c.getName();
    String username = null;
 
    username = c.getEngine().getVariable( c, "username" );
    if( username == null ) username = "";

    String myFav = username + "Favorites";
  %>

<!--<div class="block">-->
<div>

  <wiki:UserCheck status="known">
    <wiki:PageExists page="<%=myFav %>">
    <div class="myfavorites">
      <h4 class="boxtitle">
        <wiki:LinkTo page="<%=myFav %>" >My Favorites</wiki:LinkTo>
      </h4>
      <wiki:InsertPage page="<%=myFav %>"/>
    </div>
    </wiki:PageExists>
  </wiki:UserCheck>
  
  <%-- LeftMenu is automatically generated from a Wiki page called "LeftMenu" --%>
  <div class="leftmenu">
    <wiki:InsertPage page="LeftMenu" />
    <wiki:NoSuchPage page="LeftMenu">
      <div class="error">
        <wiki:EditLink page="LeftMenu">Please make a LeftMenu.</wiki:EditLink>
      </div>
    </wiki:NoSuchPage>
  </div>
  
  <div class="username">
    <wiki:UserCheck status="asserted">
        <p>G'day
          <wiki:Translate>[<wiki:UserName />]</wiki:Translate><br />(not logged in)</p>
    </wiki:UserCheck>
    <wiki:UserCheck status="authenticated">
      <p>G'day
        <wiki:Translate>[<wiki:UserName />]</wiki:Translate><br />(authenticated)</p>
    </wiki:UserCheck>
  </div>
  
  <wiki:Include page="PageActions.jsp"/>
  
  <div class="leftmenufooter">
    <wiki:InsertPage page="LeftMenuFooter" />
    <wiki:NoSuchPage page="LeftMenuFooter">
      <div class="error">
        <wiki:EditLink page="LeftMenuFooter">Please make a LeftMenuFooter.</wiki:EditLink>
      </div>
    </wiki:NoSuchPage>
  </div>
  
  <div class="wikiversion"><%=Release.APPNAME%> v<%=Release.getVersionString()%></div>
  
  <div class="rssfeed"><wiki:RSSImageLink title="Aggregate the RSS feed of the entire wiki" /></div>

</div>
