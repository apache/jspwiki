/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
 */

package org.apache.wiki.action;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.wiki.*;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.filters.RedirectException;
import org.apache.wiki.filters.SpamFilter;
import org.apache.wiki.htmltowiki.HtmlStringToWikiTranslator;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;
import org.apache.wiki.ui.stripes.WikiRequestContext;
import org.apache.wiki.workflow.DecisionRequiredException;
import org.jdom.JDOMException;

@HttpCache( allow = false )
public class EditActionBean extends AbstractPageActionBean
{
    private static final Logger log = LoggerFactory.getLogger( EditActionBean.class );

    private String m_author = null;

    private String m_spamhash = null;

    private String m_text = null;

    private String m_changeNote = null;

    private boolean m_append = false;

    private boolean m_captcha = false;

    private boolean m_remember = true;

    private String m_htmlPageText = null;

    private boolean m_livePreview = false;

    private String m_link = null;

    /**
     * Event handler method that cancels any locks the user possesses for the current wiki page,
     * and redirects the user to the {@link ViewActionBean} "view" handler.
     * @return the redirect
     */
    @DontValidate
    @HandlesEvent( "cancel" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.EDIT_ACTION )
    @WikiRequestContext( "cancel" )
    public Resolution cancel()
    {
        String pagereq = m_page.getName();
        log.debug( "Cancelled editing " + pagereq );
        
        // Cancel page lock
        HttpSession session = getContext().getRequest().getSession();
        WikiEngine engine = getContext().getEngine();
        PageLock lock = (PageLock) session.getAttribute( "lock-" + pagereq );
        if( lock != null )
        {
            engine.getPageManager().unlockPage( lock );
            session.removeAttribute( "lock-" + pagereq );
        }
        return new RedirectResolution( ViewActionBean.class ).addParameter( "page", pagereq );
    }

    @HandlesEvent( "comment" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.COMMENT_ACTION )
    @WikiRequestContext( "comment" )
    public Resolution comment()
    {
        return null;
    }

    /**
     * Event that diffs the current state of the edited page and forwards the
     * user to the diff JSP.
     * 
     * @return a forward resolution back to the preview page.
     */
    @HandlesEvent( "diff" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "diff" )
    public Resolution diff()
    {
        return new ForwardResolution( "/Diff.jsp" );
    }

    @DefaultHandler
    @DontValidate
    @HandlesEvent( "edit" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.EDIT_ACTION )
    @WikiRequestContext( "edit" )
    public Resolution edit()
    {
        WikiActionBeanContext wikiContext = getContext();
        HttpServletRequest request = wikiContext.getRequest();
        HttpSession session = request.getSession();
        Principal user = wikiContext.getCurrentUser();
        String pagereq = m_page.getName();

        log.info( "Editing page " + pagereq + ". User=" + user.getName() + ", host=" + request.getRemoteAddr() );

        // If page is locked, make sure we tell the user
        List<Message> messages = wikiContext.getMessages();
        WikiEngine engine = wikiContext.getEngine();
        PageManager mgr = engine.getPageManager();
        PageLock lock = mgr.getCurrentLock( m_page );
        if( lock != null )
        {
            messages.add( new LocalizableMessage( "edit.locked", lock.getLocker(), lock.getTimeLeft() ) );
        }
        
        // If user is not editing the latest one, tell user also
        ValidationErrors errors = getContext().getValidationErrors();
        WikiPage latest = engine.getPage( m_page.getName() );
        if( latest.getVersion() != m_page.getVersion() )
        {
            errors.addGlobalError( new LocalizableError( "edit.restoring", m_page.getVersion() ) );
        }

        // Attempt to lock the page.
        lock = mgr.lockPage( m_page, user.getName() );
        if( lock != null )
        {
            session.setAttribute( "lock-" + pagereq, lock );
        }

        // Load the page text
        m_text = engine.getPureText( m_page );

        return new ForwardResolution( "/Edit.jsp" );
    }

    /**
     * Returns whether the edited text should be appended to the page.
     * 
     * @return <code>true</code> if text should be appended;
     *         <code>false</code> otherwise (the default).
     */
    public boolean getAppend()
    {
        return m_append;
    }

    /**
     * Returns the author.
     * 
     * @return the author
     */
    public String getAuthor()
    {
        return m_author;
    }

    /**
     * Returns whether a CAPTCHA is being used for editing.
     * 
     * @return <code>true</code> if a CAPTCHA is in use; <code>false</code>
     *         otherwise.
     */
    public boolean getCaptcha()
    {
        return m_captcha;
    }

    /*
     * Returns the changenote for this upload.
     */
    public String getChangenote()
    {
        return m_changeNote;
    }

    /**
     * Returns the HTML page text.
     * 
     * @return the HTML page text
     */
    public String getHtmlPageText()
    {
        return m_htmlPageText;
    }

    /**
     * Returns the link.
     * 
     * @return the link
     */
    public String getLink()
    {
        return m_link;
    }

    /**
     * Returns <code>true</code> if the "live preview" feature is turned on;
     * <code>false</code> otherwise
     * 
     * @return the "live preview" setting
     */
    public boolean getLivePreview()
    {
        return m_livePreview;
    }

    /**
     * Returns the flag indicating whether the author name should be remembered.
     * 
     * @return the remember-me flag
     */
    public boolean getRemember()
    {
        return m_remember;
    }

    /**
     * Returns the edited text.
     * 
     * @return the text
     */
    public String getText()
    {
        return m_text;
    }

    /**
     * Initializes default values. Also looks up the correct spam hash field, as determined by
     * {@link SpamFilter#getHashFieldName(HttpServletRequest)}.

     */
    @After( stages = LifecycleStage.BindingAndValidation )
    public void initDefaultValues()
    {
        HttpServletRequest request = getContext().getRequest();

        // Look up and set spam hash field name for this particular edit
        String hashParam = SpamFilter.getHashFieldName( request );
        m_spamhash = request.getParameter( hashParam );
        if( m_spamhash != null )
        {
            m_spamhash = m_spamhash.trim();
        }
        
        // Set author: prefer authenticated/asserted principals first
        WikiSession wikiSession = getContext().getWikiSession();
        if( wikiSession.isAsserted() || wikiSession.isAuthenticated() )
        {
            m_author = wikiSession.getUserPrincipal().getName();
        }

        // Otherwise, if author not bound, check session
        else if( m_author == null )
        {
            m_author = wikiSession.getUserPrincipal().getName();
        }
    }

    /**
     * Event that the user to the preview display JSP.
     * 
     * @return a forward resolution back to the preview page.
     */
    @HandlesEvent( "preview" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "preview" )
    public Resolution preview()
    {
        log.debug( "Previewing " + m_page.getName() );
        return new ForwardResolution( "/Preview.jsp" );
    }

    @HandlesEvent( "save" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.EDIT_ACTION )
    @WikiRequestContext( "save" )
    public Resolution save() throws WikiException
    {
        WikiSession wikiSession = getContext().getWikiSession();
        HttpServletRequest request = getContext().getHttpRequest();
        HttpSession session = request.getSession();
        WikiContext wikiContext = getContext();
        WikiEngine engine = getContext().getEngine();
        String pagereq = m_page.getName();

        log.info( "Saving page " + m_page.getName() + ". UserPrincipal=" + wikiSession.getUserPrincipal().getName() + ", Author="
                  + m_author + ", Host=" + getContext().getRequest().getRemoteAddr() );

        // Check for session expiration
        Resolution r = SpamFilter.checkHash( this );
        if( r != null )
        {
            return r;
        }

        // FIXME: I am not entirely sure if the JSP page is the
        // best place to check for concurrent changes. It certainly
        // is the best place to show errors, though.
        String h = SpamFilter.getSpamHash( m_page, request );

        // Someone changed the page while we were editing it!
        if( !h.equals( m_spamhash ) )
        {
            log.info( "Page changed, warning user." );
            return new RedirectResolution( PageModifiedActionBean.class, "conflict" ).addParameter( "page", pagereq );
        }

        //
        // We expire ALL locks at this moment, simply because someone has
        // already broken it.
        //
        PageLock lock = engine.getPageManager().getCurrentLock( m_page );
        engine.getPageManager().unlockPage( lock );
        session.removeAttribute( "lock-" + pagereq );

        // Set author information and other metadata
        WikiPage modifiedPage = (WikiPage) wikiContext.getPage().clone();
        modifiedPage.setAuthor( m_author );

        // If this is an append, add it to the page.
        // If a full edit, replace the previous contents.
        try
        {
            wikiContext.setPage( modifiedPage );

            if( m_captcha )
            {
                wikiContext.setVariable( "captcha", Boolean.TRUE );
                session.removeAttribute( "captcha" );
            }

            if( m_append )
            {
                StringBuffer pageText = new StringBuffer( engine.getText( pagereq ) );
                pageText.append( m_text );
                engine.saveText( wikiContext, pageText.toString() );
            }
            else
            {
                engine.saveText( wikiContext, m_text );
            }
        }
        catch( DecisionRequiredException ex )
        {
            return new RedirectResolution( ViewActionBean.class, "view" ).addParameter( "page", "ApprovalRequiredForPageChanges" );
        }
        catch( RedirectException ex )
        {
            // Should work, but doesn't
            wikiContext.getWikiSession().addMessage( ex.getMessage() ); // FIXME:
            session.setAttribute( "message", ex.getMessage() );
            session.setAttribute( SpamFilter.getHashFieldName( request ), m_spamhash );
            return new RedirectResolution( ex.getRedirect() ).flash( this );
        }

        return new RedirectResolution( ViewActionBean.class, "view" ).addParameter( "page", pagereq );
    }

    /**
     * Sets a flag indicating that new page text should be appended to the old
     * text.
     * 
     * @param <code>true</code> if text should be appended; <code>false</code>
     *            otherwise (the default).
     */
    @Validate( required = false )
    public void setAppend( boolean append )
    {
        m_append = append;
    }

    /**
     * Sets the author.
     * 
     * @param author the author
     */
    @Validate( required = false )
    public void setAuthor( String author )
    {
        m_author = author;
    }

    /**
     * Sets a flag indicating that CAPTCHA should be used for editing.
     * 
     * @param captcha <code>true</code> if a CAPTCHA is in use;
     *            <code>false</code> otherwise.
     */
    @Validate( required = false )
    public void setCaptcha( boolean captcha )
    {
        m_captcha = captcha;
    }

    /**
     * Sets the changenote for this upload; usually a short comment.
     * 
     * @param changenote the change note
     */
    @Validate( required = false )
    public void setChangenote( String changenote )
    {
        m_changeNote = changenote;
    }

    /**
     * Sets the HTML page text, which will be translated into wiki text by
     * {@link HtmlStringToWikiTranslator}. Calling this method causes
     * {@link #setText(String)} to be called, with the translated text supplied.
     * 
     * @param the HTML to translate
     * @throws JDOMException if the HTML cannot be translated
     * @throws IOException if the HtmlStringToWikiTranslator cannot translated
     *             the text
     */
    @Validate( required = false )
    public void setHtmlPageText( String html ) throws IOException, JDOMException
    {
        m_htmlPageText = html;
        m_text = new HtmlStringToWikiTranslator().translate( html, getContext() );
    }

    /**
     * Sets the link.
     * 
     * @param link the link
     */
    @Validate( required = false )
    public void setLink( String link )
    {
        m_link = link;
    }

    /**
     * Returns <code>true</code> if the "live preview" feature is turned on;
     * <code>false</code> otherwise
     * 
     * @param livePreview the "live preview" setting
     */
    @Validate( required = false )
    public void setLivePreview( boolean livePreview )
    {
        m_livePreview = livePreview;
    }

    /**
     * Sets the flag indicating that the author name should be remembered.
     * 
     * @param remember the remember-me flag
     */
    @Validate( required = false )
    public void setRemember( boolean remember )
    {
        m_remember = remember;
    }

    /**
     * Sets the edited text.
     * 
     * @param text the text
     */
    @Validate( required = true )
    public void setText( String text )
    {
        m_text = text;
    }

}
