<%--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%>

<%@ page import="org.apache.log4j.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="org.apache.wiki.util.HttpUtil" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.api.exceptions.RedirectException" %>
<%@ page import="org.apache.wiki.filters.SpamFilter" %>
<%@ page import="org.apache.wiki.htmltowiki.HtmlStringToWikiTranslator" %>
<%@ page import="org.apache.wiki.ui.EditorManager" %>
<%@ page import="org.apache.wiki.util.HttpUtil" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="org.apache.wiki.pages.PageLock" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.auth.login.CookieAssertionLoginModule" %>
<%@ page import="org.apache.wiki.workflow.DecisionRequiredException" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.preferences.Preferences.TimeFormat" %>
<%@ page import="org.apache.wiki.variables.VariableManager" %>
<%@ page errorPage="/Error.jsp" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%!
    Logger log = Logger.getLogger("JSPWiki");

	String findParam( PageContext ctx, String key ) {
	    ServletRequest req = ctx.getRequest();

	    String val = req.getParameter( key );

	    if( val == null ) {
	        val = (String)ctx.findAttribute( key );
	    }

	    return val;
	}
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = new WikiContext( wiki, request, WikiContext.COMMENT );
    if( !wiki.getAuthorizationManager().hasAccess( wikiContext, response ) ) return;
    if( wikiContext.getCommand().getTarget() == null ) {
        response.sendRedirect( wikiContext.getURL( wikiContext.getRequestContext(), wikiContext.getName() ) );
        return;
    }
    String pagereq = wikiContext.getName();

    ResourceBundle rb = Preferences.getBundle( wikiContext, "CoreResources" );
    WikiSession wikiSession = wikiContext.getWikiSession();
    String storedUser = wikiSession.getUserPrincipal().getName();

    if( wikiSession.isAnonymous() ) {
        storedUser  = TextUtil.replaceEntities( request.getParameter( "author" ) );
    }

    String ok      = request.getParameter("ok");
    String preview = request.getParameter("preview");
    String cancel  = request.getParameter("cancel");
    String author  = TextUtil.replaceEntities( request.getParameter("author") );
    String link    = TextUtil.replaceEntities( request.getParameter("link") );
    String remember = TextUtil.replaceEntities( request.getParameter("remember") );
    String changenote = TextUtil.replaceEntities( request.getParameter( "changenote" ) );

    WikiPage wikipage = wikiContext.getPage();
    WikiPage latestversion = wiki.getPageManager().getPage( pagereq );

    session.removeAttribute( EditorManager.REQ_EDITEDTEXT );

    if( latestversion == null ) {
        latestversion = wikiContext.getPage();
    }

    //
    //  Setup everything for the editors and possible preview.  We store everything in the
    //  session.
    //

    if( remember == null ) {
        remember = (String)session.getAttribute("remember");
    }

    if( remember == null ) {
        remember = "false";
    } else {
        remember = "true";
    }

    session.setAttribute("remember",remember);

    if( author == null ) {
        author = storedUser;
    }
    if( author == null || author.length() == 0 ) {
        author = "AnonymousCoward";
    }

    session.setAttribute("author",author);

    if( link == null ) {
        link = HttpUtil.retrieveCookieValue( request, "link" );
        if( link == null ) link = "";
        link = TextUtil.urlDecodeUTF8(link);
    }

    session.setAttribute( "link", link );

    if( changenote != null ) {
       session.setAttribute( "changenote", changenote );
    }

    //
    //  Branch
    //
    log.debug("preview="+preview+", ok="+ok);

    if( ok != null ) {
        log.info("Saving page "+pagereq+". User="+storedUser+", host="+HttpUtil.getRemoteAddress(request) );

        //  Modifications are written here before actual saving

        WikiPage modifiedPage = (WikiPage)wikiContext.getPage().clone();

        //  FIXME: I am not entirely sure if the JSP page is the
        //  best place to check for concurrent changes.  It certainly
        //  is the best place to show errors, though.

        String spamhash = request.getParameter( SpamFilter.getHashFieldName(request) );

        if( !SpamFilter.checkHash(wikiContext,pageContext) ) {
            return;
        }

        //
        //  We expire ALL locks at this moment, simply because someone has already broken it.
        //
        PageLock lock = wiki.getPageManager().getCurrentLock( wikipage );
        wiki.getPageManager().unlockPage( lock );
        session.removeAttribute( "lock-"+pagereq );

        //
        //  Set author and changenote information
        //
        modifiedPage.setAuthor( storedUser );

        if( changenote != null ) {
            modifiedPage.setAttribute( WikiPage.CHANGENOTE, changenote );
        } else {
            modifiedPage.removeAttribute( WikiPage.CHANGENOTE );
        }

        //
        //  Build comment part
        //
        StringBuffer pageText = new StringBuffer(wiki.getPageManager().getPureText( wikipage ));

        log.debug("Page initial contents are "+pageText.length()+" chars");

        //
        //  Add a line on top only if we need to separate it from the content.
        //
        if( pageText.length() > 0 ) {
            pageText.append( "\n\n----\n\n" );
        }

        String commentText = EditorManager.getEditedText(pageContext);
        //log.info("comment text"+commentText);

        //
        //  WYSIWYG editor sends us its greetings
        //
        String htmlText = findParam( pageContext, "htmlPageText" );
        if( htmlText != null && cancel == null ) {
        	commentText = new HtmlStringToWikiTranslator().translate(htmlText,wikiContext);
        }

        pageText.append( commentText );

        log.debug("Author name ="+author);
        if( author != null && author.length() > 0 ) {
            String signature = author;

            if( link != null && link.length() > 0 ) {
                link = HttpUtil.guessValidURI( link );
                signature = "["+author+"|"+link+"]";
            }

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat fmt = Preferences.getDateFormat( wikiContext , TimeFormat.DATETIME);

            pageText.append("\n\n%%signature\n"+signature+", "+fmt.format(cal.getTime())+"\n/%");

        }

        if( TextUtil.isPositive(remember) ) {
            if( link != null ) {
                Cookie linkcookie = new Cookie("link", TextUtil.urlEncodeUTF8(link) );
                linkcookie.setMaxAge(1001*24*60*60);
                response.addCookie( linkcookie );
            }

            CookieAssertionLoginModule.setUserCookie( response, author );
        } else {
            session.removeAttribute("link");
            session.removeAttribute("author");
        }

        try {
            wikiContext.setPage( modifiedPage );
            wiki.getPageManager().saveText( wikiContext, pageText.toString() );
        } catch( DecisionRequiredException e ) {
        	String redirect = wikiContext.getURL(WikiContext.VIEW,"ApprovalRequiredForPageChanges");
            response.sendRedirect( redirect );
            return;
        } catch( RedirectException e ) {
            session.setAttribute( VariableManager.VAR_MSG, e.getMessage() );
            response.sendRedirect( e.getRedirect() );
            return;
        }
        response.sendRedirect(wikiContext.getViewURL(pagereq));
        return;
    } else if( preview != null ) {
        log.debug("Previewing "+pagereq);
        session.setAttribute(EditorManager.REQ_EDITEDTEXT, EditorManager.getEditedText(pageContext));
        response.sendRedirect( TextUtil.replaceString( wiki.getURL(WikiContext.PREVIEW, pagereq, "action=comment"),"&amp;","&") );
        return;
    } else if( cancel != null ) {
        log.debug("Cancelled editing "+pagereq);
        PageLock lock = (PageLock) session.getAttribute( "lock-"+pagereq );

        if( lock != null ) {
            wiki.getPageManager().unlockPage( lock );
            session.removeAttribute( "lock-"+pagereq );
        }
        response.sendRedirect( wikiContext.getViewURL(pagereq) );
        return;
    }

    log.info("Commenting page "+pagereq+". User="+request.getRemoteUser()+", host="+HttpUtil.getRemoteAddress(request) );

    //
    //  Determine and store the date the latest version was changed.  Since the newest version is the one that is changed,
    //  we need to track that instead of the edited version.
    //
    long lastchange = 0;

    Date d = latestversion.getLastModified();
    if( d != null ) lastchange = d.getTime();

    pageContext.setAttribute( "lastchange", Long.toString( lastchange ), PageContext.REQUEST_SCOPE );

    //  This is a hack to get the preview to work.
    // pageContext.setAttribute( "comment", Boolean.TRUE, PageContext.REQUEST_SCOPE );

    //
    //  Attempt to lock the page.
    //
    PageLock lock = wiki.getPageManager().lockPage( wikipage, storedUser );

    if( lock != null ) {
        session.setAttribute( "lock-"+pagereq, lock );
    }

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    response.setHeader( "Cache-control", "max-age=0" );
    response.setDateHeader( "Expires", new Date().getTime() );
    response.setDateHeader( "Last-Modified", new Date().getTime() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext, wikiContext.getTemplate(), "EditTemplate.jsp" );

%><wiki:Include page="<%=contentPage%>" />
