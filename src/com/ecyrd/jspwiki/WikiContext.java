/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.validation.Validate;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.action.*;
import com.ecyrd.jspwiki.auth.permissions.AllPermission;

/**
 *  <p>Provides state information throughout the processing of a page.  A
 *  WikiContext is born when the JSP pages that are the main entry
 *  points, are invoked.  The JSPWiki engine creates the new
 *  WikiContext, which basically holds information about the page, the
 *  handling engine, and in which context (view, edit, etc) the
 *  call was done.</p>
 *  <p>A WikiContext also provides request-specific variables, which can
 *  be used to communicate between plugins on the same page, or
 *  between different instances of the same plugin.  A WikiContext
 *  variable is valid until the processing of the page has ended.  For
 *  an example, please see the Counter plugin.</p>
 *  <p>When a WikiContext is created, it automatically associates a
 *  {@link WikiSession} object with the user's HttpSession. The
 *  WikiSession contains information about the user's authentication
 *  status, and is consulted by {@link #getCurrentUser()}.
 *  object</p>
 *  <p>Do not cache the page object that you get from the WikiContext; always
 *  use getPage()!</p>
 *
 *  @see com.ecyrd.jspwiki.plugin.Counter
 *
 *  @author Janne Jalkanen
 *  @author Andrew R. Jaquith
 */
public abstract class WikiContext extends AbstractActionBean
    implements Cloneable
{
    private    WikiPage   m_page = null;
    private    WikiPage   m_realPage = null;

    /** User is administering JSPWiki (Install, SecurityConfig). @deprecated use ActionBean  */
    public static final String    INSTALL  = InstallActionBean.class.getAnnotation(WikiRequestContext.class).value();
    
    /** The VIEW context - the user just wants to view the page
        contents. */
    public static final String    VIEW     = ViewActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User wants to view or administer workflows. */
    public static final String    WORKFLOW = WorkflowActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** The EDIT context - the user is editing the page. */
    public static final String    EDIT     = EditActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User is preparing for a login/authentication. */
    public static final String    LOGIN    = LoginActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User is preparing to log out. */
    public static final String    LOGOUT   = "logout";

    /** JSPWiki wants to display a message. */
    public static final String    MESSAGE  = MessageActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User is viewing a DIFF between the two versions of the page. */
    public static final String    DIFF     = DiffActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User is viewing page history. */
    public static final String    INFO     = PageInfoActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User is previewing the changes he just made. */
    public static final String    PREVIEW  = PreviewActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User has an internal conflict, and does quite not know what to
        do. Please provide some counseling. */
    public static final String    CONFLICT = PageModifiedActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** An error has been encountered and the user needs to be informed. */
    public static final String    ERROR    = ErrorActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User is uploading something. */
    public static final String    UPLOAD   = UploadActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User is commenting something. */
    public static final String    COMMENT  = CommentActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User is searching for content. */
    public static final String    FIND     = SearchActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User wishes to create a new group */
    public static final String    CREATE_GROUP = "createGroup";
    
    /** User is deleting an existing group. */
    public static final String    DELETE_GROUP = "deleteGroup";
    
    /** User is editing an existing group. */
    public static final String    EDIT_GROUP = GroupActionBean.class.getAnnotation(WikiRequestContext.class).value();
    
    /** User is viewing an existing group */
    public static final String    VIEW_GROUP = GroupActionBean.class.getAnnotation(WikiRequestContext.class).value();
    
    /** User is editing preferences */
    public static final String    PREFS    = UserPreferencesActionBean.class.getAnnotation(WikiRequestContext.class).value();
    
    /** User is renaming a page. */
    public static final String    RENAME   = RenameActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User is deleting a page or an attachment. */
    public static final String    DELETE   = DeleteActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** User is downloading an attachment. */
    public static final String    ATTACH   = AttachActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** RSS feed is being generated. */
    public static final String    RSS      = RSSActionBean.class.getAnnotation(WikiRequestContext.class).value();

    /** This is not a JSPWiki context, use it to access static files. */
    public static final String    NONE     = "none";  
    
    /** Same as NONE; this is just a clarification. */
    public static final String    OTHER    = "other";

    /** User is doing administrative things. */
    public static final String    ADMIN    = AdminActionBean.class.getAnnotation(WikiRequestContext.class).value();

    private static final Logger   log      = Logger.getLogger( WikiContext.class );

    /**
     * Creates a new WikiContext, without a WikiEngine, Request or WikiPage.
     */
    public WikiContext()
    {
        super();
    }

    /**
     *  Sometimes you may want to render the page using some other page's context.
     *  In those cases, it is highly recommended that you set the setRealPage()
     *  to point at the real page you are rendering.  Please see InsertPageTag
     *  for an example.
     *  <p>
     *  Also, if your plugin e.g. does some variable setting, be aware that if it
     *  is embedded in the LeftMenu or some other page added with InsertPageTag,
     *  you should consider what you want to do - do you wish to really reference
     *  the "master" page or the included page.
     *
     *  @param page  The real page which is being rendered.
     *  @return The previous real page
     *  @since 2.3.14
     *  @see com.ecyrd.jspwiki.tags.InsertPageTag
     */
    public WikiPage setRealPage( WikiPage page )
    {
        WikiPage old = m_realPage;
        m_realPage = page;
        return old;
    }

    /**
     *  Gets a reference to the real page whose content is currently being rendered.
     *  If your plugin e.g. does some variable setting, be aware that if it
     *  is embedded in the LeftMenu or some other page added with InsertPageTag,
     *  you should consider what you want to do - do you wish to really reference
     *  the "master" page or the included page.
     *  <p>
     *  For example, in the default template, there is a page called "LeftMenu".
     *  Whenever you access a page, e.g. "Main", the master page will be Main, and
     *  that's what the getPage() will return - regardless of whether your plugin
     *  resides on the LeftMenu or on the Main page.  However, getRealPage()
     *  will return "LeftMenu".
     *
     *  @return A reference to the real page.
     *  @see com.ecyrd.jspwiki.tags.InsertPageTag
     *  @see com.ecyrd.jspwiki.parser.JSPWikiMarkupParser
     */
    public WikiPage getRealPage()
    {
        return m_realPage;
    }

    /**
     *  Returns the page that is being handled. If the page had not
     *  been previously set, try to set it to the WikiEngine's
     *  front page. It is possible that this method will return
     *  <code>null</code>, so calling classes should check the 
     *  return value.
     */
    public WikiPage getPage()
    {
        return m_page;
    }

    /**
     *  Sets the page that is being handled. Calling this
     *  method also re-sets the "real page" to the same value.
     *
     *  @param page The wikipage
     *  @since 2.1.37.
     */
    @Validate(required = true)
    public void setPage( WikiPage page )
    {
        m_page = page;
        m_realPage = m_page;
    }

    /**
     * If the WikiPage contains a template attribute ({@link WikiEngine#PROP_TEMPLATEDIR}),
     * this method returns its value; otherwise, it returns superclass value via
     * {@link AbstractActionBean#getTemplate()}.
     */
    @Override
    public String getTemplate()
    {
        if ( m_page != null )
        {
            String template = (String) m_page.getAttribute(WikiEngine.PROP_TEMPLATEDIR);
            if ( template != null )
            {
                return template;
            }
        }
        return super.getTemplate();
    }

    /**
     *  This method will safely return any HTTP parameters that 
     *  might have been defined.  You should use this method instead
     *  of peeking directly into the result of getHttpRequest(), since
     *  this method is smart enough to do all of the right things,
     *  figure out UTF-8 encoded parameters, etc.
     *
     *  @since 2.0.13.
     *  @param paramName Parameter name to look for.
     *  @return HTTP parameter, or null, if no such parameter existed.
     */
    public String getHttpParameter( String paramName )
    {
        String result = null;

        if( getContext() != null )
        {
            result = getContext().getRequest().getParameter( paramName );
        }

        return result;
    }

    /**
     *  If the request did originate from a HTTP request,
     *  then the HTTP request can be fetched here.  However, it the request
     *  did NOT originate from a HTTP request, then this method will
     *  return null, and YOU SHOULD CHECK FOR IT!
     *
     *  @return Null, if no HTTP request was done.
     *  @deprecated use the method {@link #getContext()} to obtain the ActionBeanContext,
     *  and call {@link com.ecyrd.jspwiki.action.WikiActionBeanContext#getRequest()} method.
     *  @since 2.0.13.
     */
    public HttpServletRequest getHttpRequest()
    {
        return getContext().getRequest();
    }

    /**
     * Returns the name of the WikiPage associated with this wiki context.
     * @return the page name
     */
    public String getName()
    {
        return m_page != null ? m_page.getName() : "<no page>";
    }

    /**
     * Returns the URL for viewing a named wiki page, without parameters. 
     * The URL will be encoded via the HttpServletResponse object, 
     * which means that any outbound filters will be able to transform 
     * it as needed. The URL returned will be absolute if the WikiEngine
     * was configured to return absolute URLs; otherwise, the URL will be
     * relative to the webapp context root.
     * @param page the wiki page; if <code>null</code>, the front page will be used
     * @return the URL
     */
    public String getViewURL( String page )
    {
        boolean absolute = "absolute".equals(getEngine().getVariable( this, WikiEngine.PROP_REFSTYLE ));
        return getContext().getURL( ViewActionBean.class, page, null, absolute );
    }

    /**
     *  Returns a shallow clone of the WikiContext.
     *
     *  @since 2.1.37.
     *  //TODO: this could be a problem...
     */
    public Object clone()
    {
        try
        {
            // super.clone() must always be called to make sure that inherited objects
            // get the right type
            WikiContext copy = (WikiContext)super.clone();

            copy.m_variableMap    = m_variableMap;
            copy.m_page           = m_page;
            copy.m_realPage       = m_realPage;
            WikiActionBeanContext context = getContext();
            copy.setContext( context );
            String template = getTemplate();
            copy.setTemplate( template );
            return copy;
        }
        catch( CloneNotSupportedException e ){} // Never happens

        return null;
    }

    /**
     *  Returns true, if the current user has administrative permissions (i.e. the omnipotent
     *  AllPermission).
     *
     *  @since 2.4.46
     *  @return true, if the user has all permissions.
     */
    public boolean hasAdminPermissions()
    {
        boolean admin = false;
        WikiEngine engine = getEngine();
        admin = engine.getAuthorizationManager().checkPermission( getWikiSession(), 
                                                                    new AllPermission(engine.getApplicationName()) );
        return admin;
    }

}
