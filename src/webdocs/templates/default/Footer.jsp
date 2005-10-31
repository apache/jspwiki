<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<div id="footer">

  <div class="applicationlogo" align="center">
  <a href="<wiki:LinkTo page='SystemInfo' format='url'/>"
     onmouseover="document.footer_logo.src='<wiki:BaseURL/>images/jspwiki_logo.png'"
     onmouseout="document.footer_logo.src='<wiki:BaseURL/>images/jspwiki_logo_s.png'">
     <img src="<wiki:BaseURL/>images/jspwiki_logo_s.png"
          name="footer_logo" alt="JSPWiki logo"/>
  </a>
  </div>

  <div class="companylogo">
  </div>

  <div class="copyright"><wiki:InsertPage page="CopyrightNotice"/></div>

  <div class="wikiversion">
  </div>

  <div class="rssfeed"><wiki:RSSImageLink title="Aggregate the RSS feed" /></div>

  <div style="clear:both; height:0px;" > </div>

</div>
