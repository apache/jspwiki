<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

      <table width="100%" cellspacing="0" cellpadding="0" border="0">
         <tr>
            <td align="left">
                <h1 class="pagename">Adding comment to <wiki:PageName/></h1></td>
            <td align="right">
                <%@ include file="SearchBox.jsp" %>
            </td>
         </tr>
      </table>

      <hr />

      <wiki:InsertPage/>

      <h3>Please enter your comments below:</h3>

      <wiki:Editor name="commentForm">

        <textarea class="editor" wrap="virtual" name="text" rows="15" cols="60"></textarea>

        <p>
        <label for="authorname">Your name</label>
        <input type="text" name="author" id="authorname" value="<wiki:UserName/>" />
        <label for="rememberme">Remember me?</label>
        <input type="checkbox" name="remember" id="rememberme" />
        </p>

        <p>
        <input type="submit" name="ok" value="Save" />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input type="submit" name="preview" value="Preview" />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input type="submit" name="cancel" value="Cancel" />
        </p>
      </wiki:Editor>

      <wiki:NoSuchPage page="EditPageHelp">
         <div class="error">
         Ho hum, it seems that the EditPageHelp<wiki:EditLink page="EditPageHelp">?</wiki:EditLink>
         page is missing.  Someone must've done something to the installation...
         </div>
      </wiki:NoSuchPage>

      <div id="editpagehelp">
         <wiki:InsertPage page="EditPageHelp" />
      </div>
