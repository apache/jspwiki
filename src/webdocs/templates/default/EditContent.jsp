<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

      <table width="100%" cellspacing="0" cellpadding="0" border="0">
         <tr>
            <td align="left">
                <h1 class="pagename">Edit <wiki:PageName/></h1></td>
            <td align="right">
                <%@ include file="SearchBox.jsp" %>
            </td>
         </tr>
      </table>

      <hr />

      <wiki:CheckVersion mode="notlatest">
         <p class="versionnote">You are about to restore version <wiki:PageVersion/>.
         Click on "Save" to restore.  You may also edit the page before restoring it.
         </p>
      </wiki:CheckVersion>

      <wiki:CheckLock mode="locked" id="lock">
         <p class="locknote">User '<%=lock.getLocker()%>' has started to edit this page, but has not yet
         saved.  I won't stop you from editing this page anyway, BUT be aware that
         the other person might be quite annoyed.  It would be courteous to wait for his lock
         to expire or until he stops editing the page.  The lock expires in 
         <%=lock.getTimeLeft()%> minutes.
         </p>
      </wiki:CheckLock>

      <wiki:Editor />

      <wiki:NoSuchPage page="EditPageHelp">
         <div class="error">
         Ho hum, it seems that the EditPageHelp<wiki:EditLink page="EditPageHelp">?</wiki:EditLink>
         page is missing.  Someone must've done something to the installation...
         <br /><br />
         You can copy the text from the <a href="http://www.jspwiki.org/Wiki.jsp?page=EditPageHelp">EditPageHelp page on jspwiki.org</a>.
         </div>
      </wiki:NoSuchPage>

      <div id="editpagehelp">
         <wiki:InsertPage page="EditPageHelp" />
      </div>
