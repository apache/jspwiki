<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<div id="header">

  <div class="applicationlogo" align="center">
  <a href="<wiki:LinkTo page='SystemInfo' format='url'/>"
     onmouseover="document.header_logo.src='<wiki:BaseURL/>images/jspwiki_logo.png'"
     onmouseout="document.header_logo.src='<wiki:BaseURL/>images/jspwiki_logo_s.png'" >
     <img src="<wiki:BaseURL/>images/jspwiki_logo_s.png" align="center"
          name="header_logo" alt="JSPWiki logo" border="0"/>
  </a>
  </div>

  <div class="companylogo">
  </div>

  <div class="pagename"><wiki:PageName /></div>

  <div class="searchbox"><wiki:Include page="SearchBox.jsp" /></div>

  <div class="breadcrumbs">Your trail: <wiki:Breadcrumbs /></div>

  <div style="clear:both; height:0;" > </div>

</div>