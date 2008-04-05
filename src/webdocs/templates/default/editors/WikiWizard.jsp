<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ page import="java.io.Serializable"%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki"%>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.*" %>
<%@ page import="com.ecyrd.jspwiki.attachment.*" %>
<%@ page import="com.ecyrd.jspwiki.providers.*" %>
<%@ page import="com.ecyrd.jspwiki.filters.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${prefs['Language']}" />
<fmt:setBundle basename="templates.default"/>
<%--
    This provides the WikiWizard editor for JSPWiki.
--%>

<noscript>
  <div class="error"><fmt:message key="editor.wikwizard.noscript" /></div>
</noscript>

<%  
  WikiContext context = WikiContext.findContext( pageContext );
  String usertext = EditorManager.getEditedText( pageContext );    

  TemplateManager.addResourceRequest( context, "script", "scripts/wikiwizard-jspwiki.js" );
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
      action="<wiki:CheckRequestContext context='edit'><wiki:EditLink format='url'/></wiki:CheckRequestContext><wiki:CheckRequestContext context='comment'><wiki:CommentLink format='url'/></wiki:CheckRequestContext>" 
      name="editform" id="editform" 
      enctype="application/x-www-form-urlencoded">

        <%-- Edit.jsp relies on these being found.  So be careful, if you make changes. --%>
        <input name="page" type="hidden" value="<wiki:Variable var="pagename"/>" />
        <input name="action" type="hidden" value="save" />
        <input name="<%=SpamFilter.getHashFieldName(request)%>" type="hidden" value="<c:out value='${lastchange}' />" />
    

  <wiki:CheckRequestContext context="edit">
    <p>
    <label for="changenote"><fmt:message key='editor.plain.changenote'/></label>
	<%-- smaller size to fit on one line ;-) --%>
    <input type="text" id="changenote" name="changenote" size="60" maxlength="80" value="<c:out value='${changenote}'/>"/>
    </p>
  </wiki:CheckRequestContext>

<textarea style='visibility:hidden;width:100%;height:1px;'  
          class='editor' id='editorarea' 
           name='<%=EditorManager.REQ_EDITEDTEXT%>' 
           rows='10' cols='80'><%=TextUtil.replaceEntities(usertext)%></textarea>

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
attString = TextUtil.replaceEntities( attString );

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
        mayscript
        scriptable='true'>
        
	<param name="attachments" value="<%=attString%>" />
	<param name="attachpermission" value="<wiki:Permission permission="upload">true</wiki:Permission>" />
	<param name="attachmaxsize" value="<%=maxSize%>" />
	<param name="attachURL" value="<wiki:Link format="url" jsp="attach" absolute="true" />" />
	<param name="user" value="<wiki:UserName />" /> 
	<param name="breadcrumbs" value="<%=bcString%>" />
	<param name="encoding" value="<wiki:ContentEncoding />" />
	<param name="page" value="<wiki:PageName />" />
	<param name="pageexists" value="<wiki:PageExists>true</wiki:PageExists>" />
	<param name="lang" value='<%=TextUtil.replaceEntities( context.getHttpRequest().getHeader("Accept-Language") )%>' />
	<fmt:message key="editor.wikiwizard.noapplet"/>
</applet>

  <wiki:CheckRequestContext context="comment">
  <fieldset>
	<legend><fmt:message key="editor.commentsignature"/></legend>
    <p>
    <label for="authorname" accesskey="n"><fmt:message key="editor.plain.name"/></label></td>
    <input type="text" name="author" id="authorname" value="<c:out value='${sessionScope.author}' />" />
    <input type="checkbox" name="remember" id="rememberme" <%=TextUtil.isPositive((String)session.getAttribute("remember")) ? "checked='checked'" : ""%>"/>
    <label for="rememberme"><fmt:message key="editor.plain.remember"/></label>
    </p>
	<%--FIXME: seems not to read the email of the user, but some session parameter --%>
    <p>
    <label for="link" accesskey="m"><fmt:message key="editor.plain.email"/></label>
    <input type="text" name="link" id="link" size="24" value="<c:out value='${sessionScope.link}' />" />
    </p>
  </fieldset>
  </wiki:CheckRequestContext>

  <div style='display:none'>
    <input name='ok' type='submit' value='Save' />
    <input name='preview' type='submit' value='Preview' />
    <input name='cancel' type='submit' value='Cancel' />
  </div>
</div>

</form>