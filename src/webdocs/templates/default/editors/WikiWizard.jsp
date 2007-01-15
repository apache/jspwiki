<script type='text/javascript' src='scripts/wikiwizard-jspwiki.js' language='Javascript'></script>
<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ page import="java.io.Serializable"%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki"%>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.*" %>
<%@ page import="com.ecyrd.jspwiki.attachment.*" %>
<%@ page import="com.ecyrd.jspwiki.providers.*" %>
<%@ page import="org.apache.commons.lang.*" %>


<%--
    This provides the WikiWizard editor for JSPWiki.
--%>

<noscript>
  <br />
  <div class="error">You need to enable Javascript in your browser to use the WikiWizard editor</div>
</noscript>

<%  WikiContext context = WikiContext.findContext( pageContext );
    String usertext = EditorManager.getEditedText( pageContext );
    
    String changenote = (String)session.getAttribute("changenote");
    changenote = changenote != null ? TextUtil.replaceEntities(changenote) : "";
 %>   
<wiki:CheckRequestContext context="edit"><%
    if( usertext == null )
    {
        usertext = context.getEngine().getPureText( context.getPage() );
    }%>
</wiki:CheckRequestContext>
<% if( usertext == null ) usertext = ""; %>

<form accept-charset="<wiki:ContentEncoding/>"
      method="post" 
      action="<wiki:CheckRequestContext context="edit"><wiki:EditLink format="url"/></wiki:CheckRequestContext><wiki:CheckRequestContext context="comment"><wiki:CommentLink format="url"/></wiki:CheckRequestContext>" 
      name="editForm" enctype="application/x-www-form-urlencoded">

    <wiki:CheckRequestContext context="edit">
        <label for="changenote">Change note</label>
        <input type="text" id="changenote" name="changenote" size="80" maxlength="80" value="<%=changenote%>"/>
    </wiki:CheckRequestContext>

    <p>
        <%-- Edit.jsp relies on these being found.  So be careful, if you make changes. --%>
        <input name="page" type="hidden" value="<wiki:Variable var="pagename"/>" />
        <input name="action" type="hidden" value="save" />
        <input name="edittime" type="hidden" value="<%=pageContext.getAttribute("lastchange",
                                                                       PageContext.REQUEST_SCOPE )%>" />
        <input name="addr" type="hidden" value="<%=request.getRemoteAddr()%>" />
                                                                       
    </p>

<textarea style='visibility:hidden;width:100%;height:1px;'  class='editor' id='editorarea' name='<%=EditorManager.REQ_EDITEDTEXT%>' rows='10' cols='80'><%=TextUtil.replaceEntities(usertext)%></textarea>

<div id="editarea">

<%
// Create attachment list
WikiEngine engine = context.getEngine();
AttachmentManager mgr = engine.getAttachmentManager();
WikiPage ourPage = context.getPage();

String attString = "";      
if (mgr.attachmentsEnabled())
{
    try
    {
        if (ourPage != null && engine.pageExists(ourPage))
        {
            Collection atts = mgr.listAttachments(ourPage);

            if (atts != null) {
                Iterator iterator;
                iterator = atts.iterator();
    
                while (iterator.hasNext())
                {
                    Attachment att = (Attachment) iterator.next();
                    attString = attString + att.getFileName() + ";";
                }
            }
        }
    }
    catch (ProviderException e)
    {
        e.printStackTrace();
    }
}

// Create breadcrumb list
String bcString = "";

BreadcrumbsTag.FixedQueue trail = (BreadcrumbsTag.FixedQueue) session.getAttribute("breadCrumbTrail");
    
if( trail != null )
{
    for( int i = 0; i < trail.size() - 1; i++ )
    {
        String curPage = (String) trail.get(i);
        bcString += curPage + ";";            
    }
}

//Get maxsize for attachment uploads
int maxSize = TextUtil.getIntegerProperty( engine.getWikiProperties(), 
                                           AttachmentManager.PROP_MAXSIZE,
                                           Integer.MAX_VALUE );
%>

<applet id='WikiWizard'
        code='org.wikiwizard.FlashSplash' 
        archive='applets/wikiwizard.jar'
        name='WikiWizard'
        width='100%'
        height='70%'
        mayscript>
        
	<param name="attachments" value="<%=attString%>" />
	<param name="attachpermission" value="<wiki:Permission permission="upload">true</wiki:Permission>" />
	<param name="attachmaxsize" value="<%=maxSize%>" />
	<param name="attachURL" value="<wiki:Link format="url" jsp="attach" absolute="true" />" />
	<param name="user" value="<wiki:UserName />" /> 
	<param name="breadcrumbs" value="<%=bcString%>" />
	<param name="encoding" value="<wiki:ContentEncoding />" />
	<param name="page" value="<wiki:PageName />" />
	<param name="pageexists" value="<wiki:PageExists>true</wiki:PageExists>" />
	<param name="lang" value="<%=context.getHttpRequest().getHeader("Accept-Language")%>" />
	Applets are currently not supported by your browser.  Please <a href="http://www.java.com/">download Java</a>, so you can use
	the WikiWizard editor.
</applet>

   <wiki:CheckRequestContext context="comment">
        <table border="0" class="small">
          <tr>
            <td><label for="authorname" accesskey="n">Your <u>n</u>ame</label></td>
            <td><input type="text" name="author" id="authorname" value="<wiki:UserName/>" /></td>
            <td><label for="rememberme">Remember me?</label>
            <input type="checkbox" name="remember" id="rememberme" /></td>
          </tr>
          <tr>
            <td><label for="link" accesskey="m">Homepage or e<u>m</u>ail</label></td>
            <td colspan="2"><input type="text" name="link" id="link" size="40" value="<%=pageContext.getAttribute("link",PageContext.REQUEST_SCOPE)%>" /></td>
          </tr>
        </table>
    </wiki:CheckRequestContext>

    <div style='display:none'>
    <p>
        <input name='ok' type='submit' value='Save' />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input name='preview' type='submit' value='Preview' />
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input name='cancel' type='submit' value='Cancel' />
    </p>
    </div>
</div>
</form>
