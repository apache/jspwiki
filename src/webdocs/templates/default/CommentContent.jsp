<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<div class="tabmenu">
  <span><a id="menu-pagecontent" onclick="TabbedSection.onclick('pagecontent')" accesskey="d"><u>D</u>iscussion Page</a></span>
  <span><a class="activetab" id="menu-addcomment" onclick="TabbedSection.onclick('addcomment')" accesskey="c">Add a <u>c</u>omment</a></span>
  <span><a id="menu-edithelp" onclick="TabbedSection.onclick('edithelp')"  accesskey="h"><u>H</u>elp</a></span>
</div>

<div class="tabs">
<div id="pagecontent" style="display:none;">
  <wiki:InsertPage/>
</div>

<div id="addcomment">

  <wiki:Include page="editors/plain.jsp"/>

</div>

<div id="edithelp" style="display:none">

      <wiki:NoSuchPage page="EditPageHelp">
         <div class="error">
         Ho hum, it seems that the EditPageHelp<wiki:EditLink page="EditPageHelp">?</wiki:EditLink>
         page is missing.  Someone must've done something to the installation...
         </div>
      </wiki:NoSuchPage>

      <wiki:InsertPage page="EditPageHelp" />
</div>
</div>
