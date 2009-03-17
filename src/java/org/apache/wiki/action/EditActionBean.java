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
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.wiki.*;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.filters.RedirectException;
import org.apache.wiki.filters.SpamProtect;
import org.apache.wiki.htmltowiki.HtmlStringToWikiTranslator;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;
import org.apache.wiki.ui.stripes.WikiRequestContext;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.workflow.DecisionRequiredException;
import org.jdom.JDOMException;

/**
 * ActionBean that manages how users edit and comment on WikiPages.
 */
@HttpCache( allow = false )
public class EditActionBean extends AbstractPageActionBean
{
    private static final String LOCK_PREFIX = "lock-";

    private static final Logger log = LoggerFactory.getLogger( EditActionBean.class );

    private String m_author = null;

    private String m_text = null;

    private String m_changeNote = null;

    private boolean m_append = false;

    private boolean m_captcha = false;

    private String m_conflictText = null;

    private boolean m_remember = true;

    private String m_htmlPageText = null;

    private boolean m_livePreview = false;

    private String m_link = null;

    private Date m_startTime = null;

    /**
     * Event handler method that cancels any locks the user possesses for the
     * current wiki page, and redirects the user to the {@link ViewActionBean}
     * "view" handler.
     * 
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
        PageLock lock = (PageLock) session.getAttribute( LOCK_PREFIX + pagereq );
        if( lock != null )
        {
            engine.getPageManager().unlockPage( lock );
            session.removeAttribute( LOCK_PREFIX + pagereq );
        }
        return new RedirectResolution( ViewActionBean.class ).addParameter( "page", pagereq );
    }

    @HandlesEvent( "comment" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.COMMENT_ACTION )
    @WikiRequestContext( "comment" )
    public Resolution comment()
    {
        // Set the editing start time
        m_startTime = new Date();

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
    public Resolution edit() throws ProviderException
    {
        WikiActionBeanContext wikiContext = getContext();
        HttpServletRequest request = wikiContext.getRequest();
        HttpSession session = request.getSession();
        Principal user = wikiContext.getCurrentUser();
        String pageName = m_page.getName();

        log.info( "Editing page " + pageName + ". User=" + user.getName() + ", host=" + request.getRemoteAddr() );

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
        WikiPage latest;
        try
        {
            latest = engine.getPage( m_page.getName() );
        }
        catch( PageNotFoundException e )
        {
            latest = m_page;
        }
        if( latest.getVersion() != m_page.getVersion() )
        {
            errors.addGlobalError( new LocalizableError( "edit.restoring", m_page.getVersion() ) );
        }

        // Attempt to lock the page.
        lock = mgr.lockPage( m_page, user.getName() );
        if( lock != null )
        {
            session.setAttribute( LOCK_PREFIX + pageName, lock );
        }

        // Load the page text
        m_text = engine.getPureText( m_page );

        // Set the editing start time
        m_startTime = new Date();

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
     * Returns the conflicting text for this page.
     * 
     * @return the text
     */
    public String getConflictText()
    {
        return m_conflictText;
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
     * Returns the time the user started editing the page.
     * 
     * @return the start time
     */
    public Date getStartTime()
    {
        return m_startTime;
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
     * Initializes default values that must be set in order for events to work
     * properly. This method before after binding and validation of the
     * ActionBean's other properties, to make sure that the values we want are
     * bound. The values set includes the <code>author</code> property, which
     * is set to the value passed in the request parameter <code>author</code>
     * if the user is anonymous. In all other cases, the author is always set to
     * the name of the Principal returned by
     * {@link WikiSession#getUserPrincipal()}.
     */
    @After( stages = LifecycleStage.BindingAndValidation )
    public void initDefaultValues()
    {
        // Set author: prefer authenticated/asserted principals first
        WikiSession wikiSession = getContext().getWikiSession();
        if( m_author == null || !wikiSession.isAnonymous() )
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

    /**
     * Validation method that checks to see if another user has modified the
     * page since the page editing action started. This method fires only when
     * the <code>save</code> event is executed. The algorithm for detecting
     * conflicts is simple: if the last-modified time on the current WikiPage
     * (via {@link #getPage()}) is later than the start time of the editing
     * session ({@link #getStartTime()}, it's a conflict. In that case, this
     * method adds a validation error and calls {@link #setConflictText(String)}
     * with the text of the page as modified by the other user.
     */
    @ValidationMethod( on = "save", when = ValidationState.NO_ERRORS )
    public void validateNoConflicts() throws ProviderException
    {
        if( m_startTime.before( m_page.getLastModified() ) )
        {
            // Retrieve and escape the conflicting text
            String conflictText = m_page.getContentAsString();
            conflictText = StringEscapeUtils.escapeXml( conflictText );
            conflictText = TextUtil.replaceString( conflictText, "\n", "<br />" );
            m_conflictText = conflictText;

            // Create a validation error
            ValidationErrors errors = getContext().getValidationErrors();
            errors.add( "text", new LocalizableError( "edit.conflict" ) );
        }
    }

    @HandlesEvent( "save" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.EDIT_ACTION )
    @WikiRequestContext( "save" )
    @SpamProtect
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
            session.removeAttribute( LOCK_PREFIX +m_page.getName() );
        }
        catch( DecisionRequiredException ex )
        {
            session.removeAttribute( LOCK_PREFIX +m_page.getName() );
            return new RedirectResolution( ViewActionBean.class, "view" ).addParameter( "page", "ApprovalRequiredForPageChanges" );
        }
        catch( RedirectException ex )
        {
            // Should work, but doesn't
            wikiContext.getWikiSession().addMessage( ex.getMessage() ); // FIXME:
            session.setAttribute( "message", ex.getMessage() );
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
     * Sets the conflicting text for this page, which was edited by another
     * user. This property should not normally be set unless another user has
     * saved the page since the start of the editing session.
     * 
     * @param conflictText
     */
    @Validate( required = false )
    public void setConflictText( String conflictText )
    {
        m_conflictText = conflictText;
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
     * Sets the start time the user began editing. Note that this parameter,
     * when it is written to the page, will be encrypted so that it cannot be
     * tampered with by the user. When the <code>save</code> event is
     * executed, it will be decrypted and used to detect edit conflicts. This
     * value is initialized to the current time when the
     * {@link #initDefaultValues()} method fires.
     * 
     * @param date the start time
     */
    @Validate( required = true, encrypted = true )
    public void setStartTime( Date date )
    {
        m_startTime = date;
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
