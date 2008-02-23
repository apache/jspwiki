<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

<div id="favorites">

  <wiki:Include page="/WEB-INF/jsp/templates/default/UserBox.jsp" />
  
  <wiki:CheckRequestContext context='!login'>

  <wiki:UserCheck status="known">
  <wiki:Translate>[{If page='{$username}Favorites' exists='true'

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
        <wiki:EditLink page="LeftMenu">
          <fmt:message key="fav.nomenu"><fmt:param>LeftMenu</fmt:param></fmt:message>
        </wiki:EditLink>
      </div>
    </wiki:NoSuchPage>
  </div>
  
  <div class="leftmenufooter">
    <wiki:InsertPage page="LeftMenuFooter" />
    <wiki:NoSuchPage page="LeftMenuFooter">
      <div class="error">
        <wiki:EditLink page="LeftMenuFooter">
          <fmt:message key="fav.nomenu"><fmt:param>LeftMenuFooter</fmt:param></fmt:message>
        </wiki:EditLink>
      </div>
    </wiki:NoSuchPage>
  </div>

  </wiki:CheckRequestContext>
  
  <div class="wikiversion"><%=Release.APPNAME%> v<%=Release.getVersionString()%>
  <span class="rssfeed">
    <wiki:RSSImageLink title='<%=LocaleSupport.getLocalizedMessage(pageContext,"fav.aggregatewiki.title")%>' />
  </span>
  </div>  
  
</div>