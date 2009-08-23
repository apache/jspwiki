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

import java.security.Principal;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.login.CookieAssertionLoginModule;
import org.apache.wiki.auth.permissions.WikiPermission;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;
import org.apache.wiki.ui.stripes.WikiRequestContext;
import org.apache.wiki.workflow.DecisionRequiredException;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;


/**
 */
@UrlBinding( "/UserProfile.action" )
public class UserProfileActionBean extends AbstractActionBean
{
    Logger log = LoggerFactory.getLogger( UserProfileActionBean.class );

    private String m_redirect = null;

    protected UserProfile m_profile = null;

    protected String m_passwordAgain = null;

    public String getPasswordAgain()
    {
        return m_passwordAgain;
    }

    public UserProfile getProfile()
    {
        return m_profile;
    }

    public String getRedirect()
    {
        return m_redirect;
    }

    /**
     * Pre-action that loads the UserProfile before user-supplied parameters are
     * bound to the ActionBean. Also stashes the UserProfile as a request-scoped
     * attribute named <code>profile</code>. This attribute can be used in
     * JSP EL expressions as <code>$%7Bprofile%7D</code>.
     * 
     * @return <code>null</code>, always
     */
    @Before( stages = LifecycleStage.BindingAndValidation )
    public Resolution initUserProfile()
    {
        // Retrieve the user profile
        WikiEngine engine = getContext().getEngine();
        WikiSession session = getContext().getWikiSession();
        UserManager manager = engine.getUserManager();
        m_profile = manager.getUserProfile( session );

        // Null out the password, so that we don't re-encrypt it by accident
        m_profile.setPassword( null );

        // Stash the profile as a request attribute
        getContext().getRequest().setAttribute( "profile", m_profile );
        return null;

    }

    /**
     * Attempts to save the user profile, after all other validation routines
     * have executed. If the save executes without error, this method will
     * return a Resolution to the source page that executed the method. Error
     * conditions will also return a Resolution. For example, if an approval is
     * required to save the profile, a RedirectResolution redirecting the user
     * to the <code>ApprovalRequiredForUserProfiles</code> will be returned.
     * Other validation errors will cause redirection back to the profile tab.
     * 
     * @return a Resolution if the user is redirected for approvals or because
     *         of errors, or <code>null</code> otherwise
     */
    @HandlesEvent( "save" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.EDIT_PROFILE_ACTION )
    public Resolution save()
    {
        WikiActionBeanContext context = getContext();
        ValidationErrors errors = context.getValidationErrors();
        try
        {
            // Save the profile
            WikiEngine engine = getContext().getEngine();
            engine.getUserManager().setUserProfile( getContext().getWikiSession(), m_profile );
            CookieAssertionLoginModule.setUserCookie( context.getResponse(), m_profile.getFullname() );
        }

        // Not so fast, Swifty Lazar! What if someone must approve the profile?
        catch( DecisionRequiredException e )
        {
            return new RedirectResolution( ViewActionBean.class ).addParameter( "page", "ApprovalRequiredForUserProfiles" );
        }

        // Any other errors are either UI or config problems, so let the user
        // know about them
        catch( Exception e )
        {
            errors.addGlobalError( new LocalizableError( "profile.saveError", e.getMessage() ) );
        }

        // If no errors, send user to front page (or redirected page)
        if( errors.size() == 0 )
        {
            // Set user cookie
            Principal principal = getContext().getWikiSession().getUserPrincipal();
            CookieAssertionLoginModule.setUserCookie( getContext().getResponse(), principal.getName() );
            RedirectResolution r = new RedirectResolution( ViewActionBean.class );
            if( m_redirect != null )
            {
                r.addParameter( "page", m_redirect );
            }
            return r;
        }

        // Otherwise, send user to source page
        RedirectResolution r = new RedirectResolution( ViewActionBean.class );
        if ( m_redirect != null )
        {
            r.addParameter( "page", m_redirect );
        }
        return r;
    }

    public void setPasswordAgain( String password )
    {
        m_passwordAgain = password;
    }

    @ValidateNestedProperties( { @Validate( field = "loginName", required = true, minlength = 1, maxlength = 100 ),
                                @Validate( field = "fullname", required = true, minlength = 1, maxlength = 100 ),
                                @Validate( field = "password", minlength = 8, maxlength = 100 ),
                                @Validate( field = "email", converter = EmailTypeConverter.class ) } )
    public void setProfile( UserProfile profile )
    {
        m_profile = profile;
    }

    public void setRedirect( String redirect )
    {
        m_redirect = redirect;
        getContext().setVariable( "redirect", redirect );
    }

    /**
     * After all fields validate correctly, this method verifies that the user
     * has the correct permissions to save the UserProfile, and checks that the
     * supplied full name or login name don't collide with those used by
     * somebody else.
     * 
     * @param errors the current validation errors for this ActionBean
     */
    @ValidationMethod( when = ValidationState.NO_ERRORS )
    public void validateNoCollision( ValidationErrors errors )
    {
        WikiEngine engine = getContext().getEngine();
        WikiSession session = getContext().getWikiSession();
        UserManager manager = engine.getUserManager();
        UserDatabase database = manager.getUserDatabase();

        // Locate the old user profile
        UserProfile oldProfile = manager.getUserProfile( session );

        // If container authenticated, override the login name and password
        // The login name should always be the one provided by the container
        AuthenticationManager authMgr = engine.getAuthenticationManager();
        if( authMgr.isContainerAuthenticated() )
        {
            m_profile.setLoginName( oldProfile.getLoginName() );
            m_profile.setPassword( null );
        }

        // User profiles that may already have fullname or loginname
        UserProfile otherProfile;
        try
        {
            otherProfile = database.findByLoginName( m_profile.getLoginName() );
            if( otherProfile != null && !otherProfile.equals( oldProfile ) )
            {
                errors.add( "profile.loginName", new LocalizableError( "profile.nameCollision" ) );
            }
        }
        catch( NoSuchPrincipalException e )
        {
        }
        try
        {
            otherProfile = database.findByFullName( m_profile.getFullname() );
            if( otherProfile != null && !otherProfile.equals( oldProfile ) )
            {
                errors.add( "profile.fullname", new LocalizableError( "profile.nameCollision" ) );
            }
        }
        catch( NoSuchPrincipalException e )
        {
        }
    }

    /**
     * If the user profile is new, this method verifies that the user has
     * supplied matching passwords.
     * 
     * @param errors the current validation errors for this ActionBean
     */
    @ValidationMethod( when = ValidationState.ALWAYS )
    public void validatePasswords( ValidationErrors errors )
    {
        // All new profiles must have a supplied password
        if( m_profile.isNew() )
        {
            if( m_profile.getPassword() == null )
            {
                errors.add( "profile.password", new LocalizableError( "validation.required.valueNotPresent" ) );
            }
        }

        // If a password was supplied, the same value must also have been passed
        // to passwordAgain
        if( m_profile.getPassword() != null )
        {
            if( !m_profile.getPassword().equals( m_passwordAgain ) )
            {
                errors.add( "profile.password", new LocalizableError( "profile.noPasswordMatch" ) );
            }
        }
    }

    /**
     * Event handler that forwards the user to <code>/CreateProfile.jsp</code>.
     * 
     * @return the resolution
     */
    @HandlesEvent( "create" )
    @DontValidate
    public Resolution create()
    {
        return new ForwardResolution( "/CreateProfile.jsp" );
    }

    /**
     * Default event handler that forwards the user to
     * <code>/UserPreferences.jsp</code>.
     * 
     * @return the resolution
     */
    @HandlesEvent( "view" )
    @DefaultHandler
    @DontValidate
    @WikiRequestContext( "profile" )
    public Resolution view()
    {
        ForwardResolution r = new ForwardResolution( "/UserPreferences.jsp" );
        r.addParameter( "tab", "profile" );
        return r;
    }

}
