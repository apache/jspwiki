<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%-- Inserts page content for preview. --%>

   <div class="previewnote">
      <b>This is a PREVIEW!  Hit "back" on your browser to go back to the editor,
      or hit "Save" if you're happy with what you see.</b>
   </div>

   <p><hr /></p>

   <div class="previewcontent">
      <wiki:Translate><%=pageContext.getAttribute("usertext",PageContext.REQUEST_SCOPE)%></wiki:Translate>
   </div>

   <br clear="all" />

   <p><hr /></p>

   <div class="previewnote">
      <b>This is a PREVIEW!  Hit "back" on your browser to go back to the editor,
      or hit "Save" if you're happy with what you see.</b>
   </div>

   <p><hr /></p>

   <form action="<wiki:EditLink format="url" />" method="POST" 
         ACCEPT-CHARSET="<wiki:ContentEncoding />">
   <p>

   <%-- These are required parts of this form.  If you do not include these,
        horrible things will happen.  Do not modify them either. --%>

   <input type="hidden" name="page"     value="<wiki:PageName/>" />
   <input type="hidden" name="action"   value="save" />
   <input type="hidden" name="edittime" value="<%=pageContext.getAttribute("lastchange", PageContext.REQUEST_SCOPE )%>" />
   <input type="hidden" name="text"     value="<%=pageContext.getAttribute("usertext", PageContext.REQUEST_SCOPE) %>" />

   <div id="previewsavebutton" align="center">
      <input type="submit" name="ok" value="Save" />
   </div>

   </p>
   </form>
