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
        <a href="Register.jsp">Register</a> or 
        <wiki:UserCheck status="customAuth">
          <a href="Login.jsp">Log in</a>
        </wiki:UserCheck>
        <wiki:UserCheck status="containerAuth">
          <a href="LoginRedirect.jsp">Log in</a>
        </wiki:UserCheck>
      </p>
    </wiki:UserCheck>

    <wiki:UserCheck status="asserted">
      <p>
        <a href="Register.jsp">Register</a> or 
        <wiki:UserCheck status="customAuth">
          <a href="Login.jsp">Log in</a>
        </wiki:UserCheck>
        <wiki:UserCheck status="containerAuth">
          <a href="LoginRedirect.jsp">Log in</a>
        </wiki:UserCheck>
      </p>
    </wiki:UserCheck>

    <wiki:UserCheck status="authenticated">
      <p>
        <wiki:UserProfile property="new">
          <a href="Register.jsp">Register</a>
        </wiki:UserProfile>
        <wiki:UserProfile property="exists">
          <wiki:LinkTo page="UserPreferences">Set your preferences</wiki:LinkTo>
        </wiki:UserProfile>
        <br/>
        <a href="Logout.jsp">Log out</a>
      </p>
    </wiki:UserCheck>
</div>

<!-- End of automatically generated page -->

