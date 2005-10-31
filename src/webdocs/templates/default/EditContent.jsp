<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%
  /* see commonheader.jsp */
  String prefEditAreaHeight = (String) session.getAttribute("prefEditAreaHeight");
%>
<div class="tabmenu">
  <wiki:CheckLock mode="locked" id="lock">
  <span class="alerttab"><a id="menu-editlocknote"
           onclick="TabbedSection.onclick('editlocknote')" >Page Locked</a></span>
  </wiki:CheckLock>
  <span><a class="activetab" id="menu-editcontent"
           onclick="TabbedSection.onclick('editcontent')" >Edit Page</a></span>
  <wiki:HasAttachments>
  <span><a id="menu-attachments"
           onclick="TabbedSection.onclick('attachments')" >Attachments</a></span>
  </wiki:HasAttachments>
  <span><a id="menu-edithelp"
           onclick="TabbedSection.onclick('edithelp')" >Help</a></span>
  <span><a id="menu-searchbarhelp"
           onclick="TabbedSection.onclick('searchbarhelp')" >Find And Replace Help</a></span>
</div>

<div class="tabs">

<wiki:CheckLock mode="locked" id="lock">
<div id="editlocknote" style="display:none;">
  <%-- need a cancel button here --%>
  <p class="locknote">User '<%=lock.getLocker()%>' has started to edit this page, but has not yet
  saved.  I won't stop you from editing this page anyway, BUT be aware that
  the other person might be quite annoyed.  It would be courteous to wait for his lock
  to expire or until he stops editing the page.  The lock expires in
  <%=lock.getTimeLeft()%> minutes.
  </p>
</div>
</wiki:CheckLock>


<div id="editcontent">
    <wiki:CheckVersion mode="notlatest">
      <p class="versionnote">You are about to restore version <wiki:PageVersion/>.
      Click on "Save" to restore.  You may also edit the page before restoring it.
      </p>
    </wiki:CheckVersion>

    <wiki:Editor />

<%-- Search and replace section --%>
<form name="searchbar" id="searchbar">
  <label for="findText">Find:</label>
  <input type="text" name="findText" size="16"/>
  <label for="replaceText">Replace:</label>
  <input type="text" name="replaceText" size="16"/>

  <input type="checkbox" name="matchCase" /><label for="matchCase">Match Case</label>
  <input type="checkbox" name="regExp" /><label for="regExp">RegExp</label>
  <input type="checkbox" name="global" checked /><label for="global">Replace all</label>
  &nbsp;
  <input type="button" name="replace" value="Replace"
         onClick="Wiki.editReplace(this.form, document.getElementById('sectionTextArea') );" />

  <span id="undoHideOrShow" style="visibility:hidden;" >
  <input type="button" name="undo" value="Undo"
         onClick="Wiki.editUndo(this.form, document.getElementById('sectionTextArea') );" />
  </span>
  <input type="hidden" name="undoMemory" value="" />
</form>

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
<%-- dummy non-functional find/replace toolbar --%>
<p>
<form name="searchbar2" id="searchbar2">
  <label for="findText">Find:</label>
  <input type="text" name="findText" size="16" onFocus="this.blur();"/>
  <label for="replaceText">Replace:</label>
  <input type="text" name="replaceText" size="16" onFocus="this.blur();"/>

  <input type="checkbox" name="matchCase" onClick="this.checked=false;"/>
  <label for="matchCase">Match Case</label>
  <input type="checkbox" name="regExp" onClick="this.checked=false;"/>
  <label for="regExp">RegExp</label>
  <input type="checkbox" name="global" checked onClick="this.checked=true;"/>
  <label for="global">Replace all</label>
  &nbsp;
  <input type="button" name="replace" value="Replace"  onFocus="this.blur();" />
  <input type="button" name="undo" value="Undo"   onFocus="this.blur();" />
</form>
</p>
<hr />


<wiki:InsertPage page="EditFindAndReplaceHelp" />
</div>

</div>

<script src="<wiki:BaseURL/>templates/<wiki:TemplateDir/>/scripts/editsections.js"
        type="text/javascript" > </script>
