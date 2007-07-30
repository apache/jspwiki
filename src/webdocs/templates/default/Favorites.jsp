<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%
  WikiContext c = WikiContext.findContext(pageContext);
  String frontpage = c.getEngine().getFrontPage(); 
%>

<div id="favorites">
  <div id="user">
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
  <wiki:UserCheck status="notAuthenticated">
  <wiki:CheckRequestContext context='!login'>
    <wiki:Permission permission="login">
        <a href="<wiki:Link jsp='Login.jsp' format='url'><wiki:Param name='redirect' value='<%=c.getPage().getName()%>'/></wiki:Link>" 
          class="action login"
          title="<fmt:message key='actions.login.title'/>"><fmt:message key="actions.login"/></a>
    </wiki:Permission>
  </wiki:CheckRequestContext>
  </wiki:UserCheck>
  
  <wiki:UserCheck status="authenticated">
      <a href="<wiki:Link jsp='Logout.jsp' format='url' />" 
        class="action logout"
        title="<fmt:message key='actions.logout.title'/>"><fmt:message key="actions.logout"/></a>
  </wiki:UserCheck>

  <wiki:CheckRequestContext context='!prefs'>
  <wiki:CheckRequestContext context='!preview'>
    <a href="<wiki:Link jsp='UserPreferences.jsp' format='url' ><wiki:Param name='redirect' value='<%=c.getPage().getName()%>'/></wiki:Link>"
      class="action prefs" accesskey="p"
      title="<fmt:message key='actions.prefs.title'/>"><fmt:message key="actions.prefs" />
    </a>
  </wiki:CheckRequestContext>
  </wiki:CheckRequestContext>

  <div style="clear:both;"></div>
  </div>
  
  <wiki:CheckRequestContext context='!login'>
  <wiki:UserCheck status="known">
  <wiki:Translate>[{TEST page='{$username}Favorites'

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
  </wiki:CheckRequestContext>
  
  <div class="wikiversion"><%=Release.APPNAME%> v<%=Release.getVersionString()%></div>
  
  <div class="rssfeed"><wiki:RSSImageLink title="<%=LocaleSupport.getLocalizedMessage(pageContext,"fav.aggregatewiki.title")%>" /></div>

</div>