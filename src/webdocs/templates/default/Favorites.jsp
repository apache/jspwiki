<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.DefaultResources"/>
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
        <wiki:LinkTo page="<%=myFav %>" ><fmt:message key="fav.myfavorites"/></wiki:LinkTo>
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
        <wiki:EditLink page="LeftMenu"><fmt:message key="fav.nomenu"><fmt:param>LeftMenu</fmt:param></fmt:message></wiki:EditLink>
      </div>
    </wiki:NoSuchPage>
  </div>
  
  <div class="username">
    <wiki:UserCheck status="asserted">
        <p><fmt:message key="fav.greet.asserted">
             <fmt:param><wiki:Translate>[<wiki:UserName />]</wiki:Translate></fmt:param>
             </fmt:message>
        </p>
    </wiki:UserCheck>
    <wiki:UserCheck status="authenticated">
      <p><fmt:message key="fav.greet.authenticated">
             <fmt:param><wiki:Translate>[<wiki:UserName />]</wiki:Translate></fmt:param>
             </fmt:message></p>
    </wiki:UserCheck>
  </div>
  
  <wiki:Include page="PageActions.jsp"/>
  
  <div class="leftmenufooter">
    <wiki:InsertPage page="LeftMenuFooter" />
    <wiki:NoSuchPage page="LeftMenuFooter">
      <div class="error">
        <wiki:EditLink page="LeftMenuFooter"><fmt:message key="fav.nomenu"><fmt:param>LeftMenuFooter</fmt:param></fmt:message></wiki:EditLink>
      </div>
    </wiki:NoSuchPage>
  </div>
  
  <div class="wikiversion"><%=Release.APPNAME%> v<%=Release.getVersionString()%></div>
  
  <div class="rssfeed"><wiki:RSSImageLink title="<%=LocaleSupport.getLocalizedMessage(pageContext,"fav.aggregatewiki.title")%>" /></div>

</div>
