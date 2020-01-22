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
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.util.HttpUtil" %>
<%@ page import="org.apache.wiki.api.exceptions.RedirectException" %>
<%@ page import="org.apache.wiki.filters.SpamFilter" %>
<%@ page import="org.apache.wiki.htmltowiki.HtmlStringToWikiTranslator" %>
<%@ page import="org.apache.wiki.pages.PageLock" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.ui.EditorManager" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="org.apache.wiki.workflow.DecisionRequiredException" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>

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
    WikiContext wikiContext = new WikiContext( wiki, request, WikiContext.EDIT );
    if( !wiki.getAuthorizationManager().hasAccess( wikiContext, response ) ) {
        return;
    }
    if( wikiContext.getCommand().getTarget() == null ) {
        response.sendRedirect( wikiContext.getURL( wikiContext.getRequestContext(), wikiContext.getName() ) );
        return;
    }
    String pagereq = wikiContext.getName();

    WikiSession wikiSession = wikiContext.getWikiSession();
    String user = wikiSession.getUserPrincipal().getName();
    String action  = request.getParameter("action");
    String ok      = request.getParameter("ok");
    String preview = request.getParameter("preview");
    String cancel  = request.getParameter("cancel");
    String append  = request.getParameter("append");
    String edit    = request.getParameter("edit");
    String author  = TextUtil.replaceEntities( findParam( pageContext, "author" ) );
    String changenote = findParam( pageContext, "changenote" );
    String text    = EditorManager.getEditedText( pageContext );
    String link    = TextUtil.replaceEntities( findParam( pageContext, "link") );
    String spamhash = findParam( pageContext, SpamFilter.getHashFieldName(request) );
    String captcha = (String)session.getAttribute("captcha");

    if ( !wikiSession.isAuthenticated() && wikiSession.isAnonymous() && author != null ) {
        user  = TextUtil.replaceEntities( findParam( pageContext, "author" ) );
    }

    //
    //  WYSIWYG editor sends us its greetings
    //
    String htmlText = findParam( pageContext, "htmlPageText" );
    if( htmlText != null && cancel == null ) {
        text = new HtmlStringToWikiTranslator().translate(htmlText,wikiContext);
    }

    WikiPage wikipage = wikiContext.getPage();
    WikiPage latestversion = wiki.getPageManager().getPage( pagereq );

    if( latestversion == null ) {
        latestversion = wikiContext.getPage();
    }

    //
    //  Set the response type before we branch.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    response.setHeader( "Cache-control", "max-age=0" );
    response.setDateHeader( "Expires", new Date().getTime() );
    response.setDateHeader( "Last-Modified", new Date().getTime() );

    //log.debug("Request character encoding="+request.getCharacterEncoding());
    //log.debug("Request content type+"+request.getContentType());
    log.debug("preview="+preview+", ok="+ok);

    if( ok != null || captcha != null ) {
        log.info("Saving page "+pagereq+". User="+user+", host="+HttpUtil.getRemoteAddress(request) );

        //
        //  Check for session expiry
        //

        if( !SpamFilter.checkHash(wikiContext,pageContext) ) {
            return;
        }

        WikiPage modifiedPage = (WikiPage)wikiContext.getPage().clone();

        //  FIXME: I am not entirely sure if the JSP page is the
        //  best place to check for concurrent changes.  It certainly
        //  is the best place to show errors, though.

        String h = SpamFilter.getSpamHash( latestversion, request );

        if( !h.equals(spamhash) ) {
            //
            // Someone changed the page while we were editing it!
            //

            log.info("Page changed, warning user.");

            session.setAttribute( EditorManager.REQ_EDITEDTEXT, EditorManager.getEditedText(pageContext) );
            response.sendRedirect( wiki.getURL(WikiContext.CONFLICT, pagereq, null ) );
            return;
        }

        //
        //  We expire ALL locks at this moment, simply because someone has already broken it.
        //
        PageLock lock = wiki.getPageManager().getCurrentLock( wikipage );
        wiki.getPageManager().unlockPage( lock );
        session.removeAttribute( "lock-"+pagereq );

        //
        //  Set author information and other metadata
        //
        modifiedPage.setAuthor( user );

        if( changenote == null ) {
            changenote = (String) session.getAttribute("changenote");
        }

        session.removeAttribute("changenote");

        if( changenote != null && changenote.length() > 0 ) {
            modifiedPage.setAttribute( WikiPage.CHANGENOTE, changenote );
        } else {
            modifiedPage.removeAttribute( WikiPage.CHANGENOTE );
        }

        //
        //  Figure out the actual page text
        //
        if( text == null ) {
            throw new ServletException( "No parameter text set!" );
        }

        //
        //  If this is an append, then we just append it to the page.
        //  If it is a full edit, then we will replace the previous contents.
        //
        try {
            wikiContext.setPage( modifiedPage );

            if( captcha != null ) {
                wikiContext.setVariable( "captcha", Boolean.TRUE );
                session.removeAttribute( "captcha" );
            }

            if( append != null ) {
                StringBuffer pageText = new StringBuffer(wiki.getPageManager().getText( pagereq ));
                pageText.append( text );
                wiki.getPageManager().saveText( wikiContext, pageText.toString() );
            } else {
                wiki.getPageManager().saveText( wikiContext, text );
            }
        } catch( DecisionRequiredException ex ) {
        	String redirect = wikiContext.getURL(WikiContext.VIEW,"ApprovalRequiredForPageChanges");
            response.sendRedirect( redirect );
            return;
        } catch( RedirectException ex ) {
            // FIXME: Cut-n-paste code.
            wikiContext.getWikiSession().addMessage( ex.getMessage() ); // FIXME: should work, but doesn't
            session.setAttribute( "message", ex.getMessage() );
            session.setAttribute(EditorManager.REQ_EDITEDTEXT, EditorManager.getEditedText(pageContext));
            session.setAttribute("author",user);
            session.setAttribute("link",link != null ? link : "" );
            if( htmlText != null ) session.setAttribute( EditorManager.REQ_EDITEDTEXT, text );

            session.setAttribute("changenote", changenote != null ? changenote : "" );
            session.setAttribute(SpamFilter.getHashFieldName(request), spamhash);
            response.sendRedirect( ex.getRedirect() );
            return;
        }

        response.sendRedirect(wikiContext.getViewURL(pagereq));
        return;
    } else if( preview != null ) {
        log.debug("Previewing "+pagereq);
        session.setAttribute(EditorManager.REQ_EDITEDTEXT, EditorManager.getEditedText(pageContext));
        session.setAttribute("author",user);
        session.setAttribute("link",link != null ? link : "" );

        if( htmlText != null ) {
            session.setAttribute( EditorManager.REQ_EDITEDTEXT, text );
        }

        session.setAttribute("changenote", changenote != null ? changenote : "" );
        response.sendRedirect( wiki.getURL( WikiContext.PREVIEW, pagereq, null ) );
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

    session.removeAttribute( EditorManager.REQ_EDITEDTEXT );

    log.info("Editing page "+pagereq+". User="+user+", host="+HttpUtil.getRemoteAddress(request) );


    //
    //  Determine and store the date the latest version was changed.  Since
    //  the newest version is the one that is changed, we need to track
    //  that instead of the edited version.
    //
    String lastchange = SpamFilter.getSpamHash( latestversion, request );

    pageContext.setAttribute( "lastchange", lastchange, PageContext.REQUEST_SCOPE );

    //
    //  Attempt to lock the page.
    //
    PageLock lock = wiki.getPageManager().lockPage( wikipage, user );
    if( lock != null ) {
        session.setAttribute( "lock-"+pagereq, lock );
    }

    String contentPage = wiki.getTemplateManager().findJSP( pageContext, wikiContext.getTemplate(), "EditTemplate.jsp" );

%><wiki:Include page="<%=contentPage%>" />