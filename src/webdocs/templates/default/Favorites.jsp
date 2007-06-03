<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>
<%@ page import="com.ecyrd.jspwiki.*" %>

<div id="favorites">

  <div class="username">
    <wiki:UserCheck status="anonymous">
      <fmt:message key="fav.greet.anonymous" />
    </wiki:UserCheck>
    <wiki:UserCheck status="asserted">
      <fmt:message key="fav.greet.asserted">
        <fmt:param><wiki:Translate>[<wiki:UserName />]</wiki:Translate></fmt:param>
        <%--
        <fmt:param><wiki:LinkTo page='UserPreferences'><wiki:UserName/></wiki:Link></fmt:param>
        --%>
      </fmt:message>
    </wiki:UserCheck>
    <wiki:UserCheck status="authenticated">
      <fmt:message key="fav.greet.authenticated">
        <fmt:param><wiki:Translate>[<wiki:UserName />]</wiki:Translate></fmt:param>
      </fmt:message>
    </wiki:UserCheck>
  </div>
  
  <wiki:UserCheck status="known">
  <wiki:Translate>[{Test page='{$username}Favorites'

%%collapsebox-closed
! [My Favorites|{$username}Favorites]
[{InsertPage page='{$username}Favorites' }]
%% }]
  </wiki:Translate>
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