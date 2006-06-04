<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%
  /* see commonheader.jsp */
  String prefEditAreaHeight = (String) session.getAttribute("prefEditAreaHeight");
%>
  
<wiki:CheckLock mode="locked" id="lock">
  <%-- need a cancel button here --%>
  <p class="error">User '<%=lock.getLocker()%>' has started to edit this page, but has not yet
    saved.  I won't stop you from editing this page anyway, BUT be aware that
    the other person might be quite annoyed.  It would be courteous to wait for the lock
    to expire or until the person stops editing the page.  The lock expires in
    <%=lock.getTimeLeft()%> minutes.
  </p>
</wiki:CheckLock>
  
<wiki:TabbedSection>
  
  <wiki:Tab id="editcontent" title="Edit Page">
  
    <wiki:CheckVersion mode="notlatest">
      <div class="warning">You are about to restore version <wiki:PageVersion/>.
        Click on "Save" to restore.  You may also edit the page before restoring it.
      </div>
    </wiki:CheckVersion>
    
    <div id="editorbar">  
    Editor:
    <select onchange="location.href=this.value">
      <wiki:EditorIterator id="editor">
        <option <%=editor.isSelected()%> value="<%=editor.getURL()%>"><%=editor.getName()%></option>
      </wiki:EditorIterator>
    </select>
    </div>
    
    <wiki:Editor />
    
  </wiki:Tab>
  
  <wiki:HasAttachments>
    <wiki:Tab id="attachments" title="Attachments">
      <wiki:Include page="AttachmentTab.jsp" />
    </wiki:Tab>
  </wiki:HasAttachments>
  
  <wiki:Tab id="edithelp" title="Help">
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
  </wiki:Tab>
  
  <wiki:Tab id="searchbarhelp" title="Find and Replace help">
    <wiki:InsertPage page="EditFindAndReplaceHelp" />
  </wiki:Tab>
  
</wiki:TabbedSection>
