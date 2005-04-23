<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%-- Inserts page content for preview. --%>

   <div class="previewnote">
      <b>This is a PREVIEW!  Hit "Keep Editing" to go back to the editor,
      or hit "Save" if you're happy with what you see.</b>
   </div>

   <p><hr /></p>

   <div class="previewcontent">
      <wiki:Translate><%=pageContext.getAttribute("usertext",PageContext.REQUEST_SCOPE)%></wiki:Translate>
   </div>

   <br clear="all" />

   <p><hr /></p>

   <div class="previewnote">
      <b>This is a PREVIEW!  Hit "Keep Editing" to go back to the editor,
      or hit "Save" if you're happy with what you see.</b>
   </div>

   <p><hr /></p>

   <wiki:Editor>
     <textarea rows="4" cols="20" readonly="true" style="display:none" name="text"><%=pageContext.getAttribute("usertext", PageContext.REQUEST_SCOPE) %></textarea>

     <div id="previewsavebutton" align="center">
        <input type="submit" name="edit" value="Keep editing" />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input type="submit" name="ok" value="Save" />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input type="submit" name="cancel" value="Cancel" />
     </div>
    </wiki:Editor>