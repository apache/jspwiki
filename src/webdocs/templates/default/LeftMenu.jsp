<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<div align="center">
    <a href="<wiki:LinkTo page="SystemInfo" format="url"/>" onmouseover="document.jspwiki_logo.src='images/jspwiki_logo.png'" onmouseout="document.jspwiki_logo.src='images/jspwiki_logo_s.png'"><img src="images/jspwiki_logo_s.png" border="0" name="jspwiki_logo" alt="JSPWiki logo"/></a><br />
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

    <wiki:UserCheck status="known">
        <b>G'day,</b><br />
        <wiki:Translate>[<wiki:UserName />]</wiki:Translate>
    </wiki:UserCheck>

    <wiki:UserCheck status="unknown">
        <tt>
        Set your name in<br />
        <wiki:LinkTo page="UserPreferences">UserPreferences</wiki:LinkTo>
        </tt>    
    </wiki:UserCheck>
</div>

<!-- End of automatically generated page -->

