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
import java.net.URI;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.login.CookieAssertionLoginModule;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.lock.PageLock;
import org.apache.wiki.filters.RedirectException;
import org.apache.wiki.htmltowiki.HtmlStringToWikiTranslator;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.preferences.Preferences.TimeFormat;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.SpamProtect;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;
import org.apache.wiki.ui.stripes.WikiRequestContext;
import org.apache.wiki.util.HttpUtil;
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

    private String m_changenote = null;

    private boolean m_append = false;

    private boolean m_captcha = false;

    private Map<String,Boolean> m_options = new HashMap<String,Boolean>();

    private String m_conflictText = null;

    private String m_htmlPageText = null;

    private String m_email = null;

    private long m_startTime = -1;

    /**
     * Event handler method that cancels any locks the user possesses for the
     * current wiki page, and redirects the user to the {@link ViewActionBean}
     * "view" handler.
     * 
     * @return the redirect
     */
    @DontValidate
    @HandlesEvent( "cancel" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.EDIT_ACTION )
    @WikiRequestContext( "cancel" )
    public Resolution cancel()
    {
        WikiPage page = getPage();
        String pagereq = page.getName();
        log.debug( "Cancelled editing " + pagereq );

        // Cancel page lock
        HttpSession session = getContext().getRequest().getSession();
        WikiEngine engine = getContext().getEngine();
        PageLock lock = (PageLock) session.getAttribute( LOCK_PREFIX + pagereq );
        engine.getContentManager().unlockPage( lock );
        session.removeAttribute( LOCK_PREFIX + pagereq );
        return new RedirectResolution( ViewActionBean.class ).addParameter( "page", pagereq );
    }

    @HandlesEvent( "comment" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.COMMENT_ACTION )
    @WikiRequestContext( "comment" )
    public Resolution comment() throws ProviderException
    {
        WikiActionBeanContext wikiContext = getContext();
        HttpServletRequest request = wikiContext.getRequest();
        HttpSession session = request.getSession();
        Principal user = wikiContext.getCurrentUser();
        WikiPage page = getPage();
        String pageName = page.getName();

        log.info("Commenting page "+pageName+". User="+request.getRemoteUser()+", host="+request.getRemoteAddr() );
        
        // Set the editing start time (will be written to the JSPs as encrypted parameter)
        setStartTime( System.currentTimeMillis() );

        // If page is locked, make sure we tell the user
        List<Message> messages = wikiContext.getMessages();
        WikiEngine engine = wikiContext.getEngine();
        ContentManager mgr = engine.getContentManager();
        PageLock lock = mgr.getCurrentLock( page );
        if( lock != null )
        {
            messages.add( new LocalizableMessage( "edit.locked", lock.getLocker(), lock.getTimeLeft() ) );
        }

        // If user is not editing the latest one, tell user also
        ValidationErrors errors = getContext().getValidationErrors();
        WikiPage latest;
        try
        {
            latest = engine.getPage( page.getName() );
        }
        catch( PageNotFoundException e )
        {
            latest = page;
        }
        if( latest.getVersion() != page.getVersion() )
        {
            errors.addGlobalError( new LocalizableError( "edit.restoring", page.getVersion() ) );
        }

        // Attempt to lock the page.
        lock = mgr.lockPage( page, user.getName() );
        if( lock != null )
        {
            session.setAttribute( LOCK_PREFIX + pageName, lock );
        }

        // The comment field is initialized with nothing
        setText( "" );

        return new ForwardResolution( "/Comment.jsp" );
    }

    /**
     * Event that diffs the current state of the edited page and forwards the
     * user to the diff JSP.
     * 
     * @return a forward resolution back to the preview page.
     */
    @HandlesEvent( "diff" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "diff" )
    public Resolution diff()
    {
        return new ForwardResolution( "/Diff.jsp" );
    }

    @DefaultHandler
    @DontValidate
    @HandlesEvent( "edit" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.EDIT_ACTION )
    @WikiRequestContext( "edit" )
    public Resolution edit() throws ProviderException
    {
        WikiActionBeanContext wikiContext = getContext();
        HttpServletRequest request = wikiContext.getRequest();
        HttpSession session = request.getSession();
        Principal user = wikiContext.getCurrentUser();
        WikiPage page = getPage();
        String pageName = page.getName();

        log.info( "Editing page " + pageName + ". User=" + user.getName() + ", host=" + request.getRemoteAddr() );

        // Set the editing start time (will be written to the JSPs as encrypted parameter)
        setStartTime( System.currentTimeMillis() );
        
        // If page is locked, make sure we tell the user
        List<Message> messages = wikiContext.getMessages();
        WikiEngine engine = wikiContext.getEngine();
        ContentManager mgr = engine.getContentManager();
        PageLock lock = mgr.getCurrentLock( page );
        if( lock != null )
        {
            messages.add( new LocalizableMessage( "edit.locked", lock.getLocker(), lock.getTimeLeft() ) );
        }

        // If user is not editing the latest one, tell user also
        ValidationErrors errors = getContext().getValidationErrors();
        WikiPage latest;
        try
        {
            latest = engine.getPage( page.getName() );
        }
        catch( PageNotFoundException e )
        {
            latest = page;
        }
        if( latest.getVersion() != page.getVersion() )
        {
            errors.addGlobalError( new LocalizableError( "edit.restoring", page.getVersion() ) );
        }

        // Attempt to lock the page.
        lock = mgr.lockPage( page, user.getName() );
        if( lock != null )
        {
            session.setAttribute( LOCK_PREFIX + pageName, lock );
        }

        // Load the page text
        setText( engine.getPureText( page ) );

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
        return m_changenote;
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
     * Returns the email.
     * 
     * @return the email
     */
    public String getEmail()
    {
        return m_email;
    }

    /**
     * Returns the optional settings for the editor. Each setting name is a key in the map,
     * with the vale expressed as a Boolean.
     * @return the options map
     */
    public Map<String,Boolean> getOptions()
    {
        return m_options;
    }

    /**
     * Returns the time the user started editing the page.
     * 
     * @return the start time
     */
    public long getStartTime()
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
     * <p>Initializes default values that must be set in order for events to work
     * properly. This method before after binding and validation of the
     * ActionBean's other properties, to make sure that the values we want are
     * bound. The values set includes:</p>
     * <ul>
     * <li>the {@code author} property, which
     * is set to the value passed in the request parameter {@code author}
     * or {@code Anonymous Coward} if the user is anonymous. In all other cases,
     * the author is always set to the name of the Principal returned by
     * {@link WikiSession#getUserPrincipal()}.</li>
     * <li>the {@code email} property, which for authenticated users is set
     * to their user profile's e-mail address</li>
     * </ul>
     */
    @After( stages = LifecycleStage.BindingAndValidation )
    public void initDefaultValues()
    {
        // Set author: prefer authenticated/asserted principals first
        WikiSession wikiSession = getContext().getWikiSession();
        if( m_author == null && wikiSession.isAnonymous() )
        {
            Principal author = wikiSession.getUserPrincipal();
            if ( author instanceof WikiPrincipal &&
                WikiPrincipal.IP_ADDRESS.equals( ((WikiPrincipal)author).getType() ) )
            {
                setAuthor( "Anonymous Coward" );
            }
        }
        else
        {
            setAuthor( wikiSession.getUserPrincipal().getName() );
        }
        
        // Set email if user is authenticated
        if ( wikiSession.isAuthenticated() )
        {
            UserManager mgr = getContext().getEngine().getUserManager();
            UserProfile profile = mgr.getUserProfile( wikiSession );
            if ( profile.getEmail() != null )
            {
                setEmail( profile.getEmail() );
            }
        }
    }

    /**
     * Event that the user to the preview display JSP.
     * 
     * @return a forward resolution back to the preview page.
     */
    @HandlesEvent( "preview" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "preview" )
    public Resolution preview()
    {
        log.debug( "Previewing " + getPage().getName() );
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
        ValidationErrors errors = getContext().getValidationErrors();
        WikiPage page = getPage();
        boolean exists = getContext().getEngine().pageExists( page );
        long lastModified = exists ? page.getLastModified().getTime() : -1;
        if( exists && m_startTime < lastModified )
        {
            // Retrieve and escape the conflicting text
            String conflictText = page.getContentAsString();
            conflictText = StringEscapeUtils.escapeXml( conflictText );
            conflictText = TextUtil.replaceString( conflictText, "\n", "<br />" );
            setConflictText( conflictText );

            // Create a validation error
            errors.add( "text", new LocalizableError( "edit.conflict") );
        }
        
        // Is the user trying to edit a special page? Tsk, tsk.
        URI uri = getContext().getEngine().getSpecialPageReference( page.getName() );
        if( uri != null )
        {
            errors.add( "page", new LocalizableError( "edit.specialpage" ) );
        }
    }

    @HandlesEvent( "save" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.EDIT_ACTION )
    @WikiRequestContext( "save" )
    @SpamProtect( content = "text" )
    public Resolution save() throws WikiException
    {
        WikiSession wikiSession = getContext().getWikiSession();
        HttpServletRequest request = getContext().getHttpRequest();
        HttpSession session = request.getSession();
        WikiContext wikiContext = getContext();
        WikiPage page = getPage();
        WikiEngine engine = getContext().getEngine();
        String pagereq = page.getName();

        log.info( "Saving page " + page.getName() + ". UserPrincipal=" + wikiSession.getUserPrincipal().getName() + ", Author="
                  + m_author + ", Host=" + getContext().getRequest().getRemoteAddr() );

        // Set author information and other metadata
        WikiPage modifiedPage = (WikiPage)page.clone();
        modifiedPage.setAuthor( m_author );
        if( m_changenote != null )
        {
            modifiedPage.setAttribute( WikiPage.CHANGENOTE, m_changenote );
        }

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

            // If this is an append, add a separation line and the author's details
            if( m_append )
            {
                StringBuffer pageText = new StringBuffer( engine.getText( pagereq ) );
                if( pageText.length() > 0 )
                {
                    pageText.append( "\n\n----\n\n" );
                }
                pageText.append( m_text );
                if( m_author != null && m_author.length() > 0 )
                {
                    String signature = m_author;
                    if( m_email != null && m_email.length() > 0 )
                    {
                        String link = HttpUtil.guessValidURI( m_email );
                        signature = "["+m_author+"|"+link+"]";
                    }
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat fmt = Preferences.getDateFormat( wikiContext ,  TimeFormat.DATETIME);
                    pageText.append("\n\n--"+signature+", "+fmt.format(cal.getTime()));
                }
                engine.saveText( wikiContext, pageText.toString() );
            }
            else
            {
                engine.saveText( wikiContext, m_text );
            }
            
            //  We expire ALL locks at this moment, simply because someone has
            //  already broken it.
            PageLock lock = (PageLock) session.getAttribute( LOCK_PREFIX + pagereq );
            engine.getContentManager().unlockPage( lock );
            session.removeAttribute( LOCK_PREFIX +page.getName() );
        }
        catch( DecisionRequiredException ex )
        {
            PageLock lock = (PageLock) session.getAttribute( LOCK_PREFIX + pagereq );
            engine.getContentManager().unlockPage( lock );
            session.removeAttribute( LOCK_PREFIX +page.getName() );
            return new RedirectResolution( ViewActionBean.class, "view" ).addParameter( "page", "ApprovalRequiredForPageChanges" );
        }
        catch( RedirectException ex )
        {
            throw new WikiException("Strange redirection: " + ex.getMessage(), ex);
        }

        return new RedirectResolution( ViewActionBean.class, "view" ).addParameter( "page", pagereq );
    }

    /**
     * Sets a flag indicating that new page text should be appended to the old
     * text.
     * 
     * @param append <code>true</code> if text should be appended; <code>false</code>
     *            otherwise (the default).
     */
    @Validate( required = false )
    public void setAppend( boolean append )
    {
        m_append = append;
    }

    /**
     * If the WikiSession is anonymous or asserted, sets the author and
     * causes the "assertion cookie" to be set in the HTTP response. If the
     * user is authenticated, this method does nothing.
     * 
     * @param author the author
     */
    @Validate( required = false )
    public void setAuthor( String author )
    {
        WikiSession session = getContext().getWikiSession();
        if ( !session.isAuthenticated() )
        {
            m_author = TextUtil.replaceEntities( author );
            HttpServletResponse response = getContext().getResponse();
            CookieAssertionLoginModule.setUserCookie( response, m_author );
        }
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
        m_changenote = changenote;
    }

    /**
     * Sets the HTML page text, which will be translated into wiki text by
     * {@link HtmlStringToWikiTranslator}. Calling this method causes
     * {@link #setText(String)} to be called, with the translated text supplied.
     * 
     * @param html the HTML to translate
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
     * Sets the e-mail address for the user, and causes a Cookie called
     * {@code link} to be written to the HTTP response. If the user
     * is authenticated, this method will check to see if {@code email} is
     * different from the one in the user's UserProfile. If it is,
     * it will add a message to the user indicating that it is different
     * and can be changed in their user profile if desired.
     * 
     * @param email the email address
     */
    @Validate( required = false, converter = EmailTypeConverter.class )
    public void setEmail( String email )
    {
        m_email = email;
        Cookie linkcookie = new Cookie("link", email );
        linkcookie.setMaxAge(1001*24*60*60);
        getContext().getResponse().addCookie( linkcookie );
        
        // If authenticated, is the e-mail different than the one on file?
        WikiSession session = getContext().getWikiSession();
        if ( session.isAuthenticated() )
        {
            WikiEngine engine = getContext().getEngine();
            UserProfile profile = engine.getUserManager().getUserProfile( session );
            if ( email.equals( profile.getEmail() ) )
            {
                Message message = new LocalizableMessage( "changed.email" );
                getContext().getMessages().add( message );
            }
        }
    }

    /**
     * Sets the optional settings for the editor. Each setting name is a key in the map,
     * with the vale expressed as a Boolean. Common option names include:
     * {@code remember}, {@code livePreview}, {@code tabCompletion}, and {@code smartPairs}.
     */
    public void setOptions( Map<String,Boolean> options )
    {
        m_options = options;
    }

    /**
     * Sets the start time the user began editing. Note that this parameter,
     * when it is written to the page, will be encrypted so that it cannot be
     * tampered with by the user. When the <code>save</code> event is
     * executed, it will be decrypted and used to detect edit conflicts. This
     * value is initialized to the current time when the
     * {@link #edit()} or {@link #comment()} methods fire.
     * 
     * @param time the start time
     */
    @Validate( required = true, encrypted = true )
    public void setStartTime( long time )
    {
        m_startTime = time;
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
