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

        <wiki:EditorArea/>

        <table border="0">
          <tr>
            <td><label for="authorname">Your name</label></td><td><input type="text" name="author" id="authorname" value="<wiki:UserName/>" /></td>
            <td><label for="rememberme">Remember me?</label></td><td><input type="checkbox" name="remember" id="rememberme" /></td>
          </tr>
          <tr>
            <td><label for="link">Homepage or email</label></td><td colspan="3"><input type="text" name="link" id="link" value="<%=pageContext.getAttribute("link",PageContext.REQUEST_SCOPE)%>" /></td>
          </tr>
        </table>

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
