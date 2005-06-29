<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<div align="center">
    <a href="<wiki:LinkTo page="SystemInfo" format="url"/>" onmouseover="document.jspwiki_logo.src='<wiki:BaseURL/>images/jspwiki_logo.png'" onmouseout="document.jspwiki_logo.src='<wiki:BaseURL/>images/jspwiki_logo_s.png'"><img src="<wiki:BaseURL/>images/jspwiki_logo_s.png" border="0" name="jspwiki_logo" alt="JSPWiki logo"/></a><br />
</div>

<!-- LeftMenu is automatically generated from a Wiki page called "LeftMenu" -->

<wiki:InsertPage page="LeftMenu" />
<wiki:NoSuchPage page="LeftMenu">
    <hr />
    <p align="center">
    <i>No LeftMenu!</i><br />
    <wiki:EditLink page="LeftMenu">Please make one.</wiki:EditLink><br />
    </p>
    <hr />
</wiki:NoSuchPage>

<div align="center" class="username">
    <wiki:UserCheck status="asserted">
        <b>G'day,</b><br />
        <wiki:Translate>[<wiki:UserName />]</wiki:Translate>
    </wiki:UserCheck>

    <wiki:UserCheck status="authenticated">
        <b>G'day,</b><br />
        <wiki:Translate>[<wiki:UserName />]</wiki:Translate>
    </wiki:UserCheck>
</div>

<div>
    <wiki:UserCheck status="anonymous">
        <p>
        <wiki:LinkTo page="UserPreferences">Set your user name</wiki:LinkTo>
        <br/>or <a href="Login.jsp">Log in</a>
        </p>
    </wiki:UserCheck>
    
    <wiki:UserCheck status="asserted">
      <p>
      <wiki:LinkTo page="UserPreferences">Set your preferences</wiki:LinkTo>
      <br/>
      <a href="Logout.jsp">Log out</a>
      </p>
    </wiki:UserCheck>

    <wiki:UserCheck status="authenticated">
      <p>
      <wiki:LinkTo page="UserPreferences">Set your preferences</wiki:LinkTo>
      <br/>
      <a href="Logout.jsp">Log out</a>
      </p>
    </wiki:UserCheck>
</div>

<!-- End of automatically generated page -->

