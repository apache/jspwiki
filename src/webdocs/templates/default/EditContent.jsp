<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%
  /* see commonheader.jsp */
  String prefEditAreaHeight = (String) session.getAttribute("prefEditAreaHeight");
%>
<div class="tabmenu">
    <span><a class="activetab" id="menu-editcontent" onclick="TabbedSection.onclick('editcontent')" >Edit Page</a></span>
    <wiki:HasAttachments><span><a id="menu-attachments" onclick="TabbedSection.onclick('attachments')" >Attachments</a></span></wiki:HasAttachments>
    <span><a id="menu-edithelp" onclick="TabbedSection.onclick('edithelp')" >Help</a></span>
    <span><a id="menu-searchbarhelp" onclick="TabbedSection.onclick('searchbarhelp')" >Find And Replace Help</a></span>
</div>

<wiki:CheckLock mode="locked" id="lock">
<div id="editlocknote">
  <%-- need a cancel button here --%>
  <p class="locknote">User '<%=lock.getLocker()%>' has started to edit this page, but has not yet
  saved.  I won't stop you from editing this page anyway, BUT be aware that
  the other person might be quite annoyed.  It would be courteous to wait for the lock
  to expire or until the person stops editing the page.  The lock expires in
  <%=lock.getTimeLeft()%> minutes.
  </p>
</div>
</wiki:CheckLock>

<div class="tabs">

<div id="editcontent">
    <wiki:CheckVersion mode="notlatest">
      <p class="versionnote">You are about to restore version <wiki:PageVersion/>.
      Click on "Save" to restore.  You may also edit the page before restoring it.
      </p>
    </wiki:CheckVersion>

    <wiki:Editor />

</div>

<wiki:HasAttachments>
  <div id="attachments" style="display:none;" >
  <wiki:Include page="AttachmentTab.jsp" />
  </div>
</wiki:HasAttachments>


<div id="edithelp" style="display:none;">
  <wiki:NoSuchPage page="EditPageHelp">
    <div class="error">
    Ho hum, it seems that the EditPageHelp<wiki:EditLink page="EditPageHelp">?</wiki:EditLink>
    page is missing.  Someone must've done something to the installation...
    <br /><br />
    You can copy the text from the
    <a href="http://www.jspwiki.org/Wiki.jsp?page=EditPageHelp">EditPageHelp page on jspwiki.org</a>.
    </div>
  </wiki:NoSuchPage>

  <wiki:InsertPage page="EditPageHelp" />
</div>

<div id="searchbarhelp"  style="display:none;">
<wiki:InsertPage page="EditFindAndReplaceHelp" />
</div>

</div>
