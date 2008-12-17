package com.ecyrd.jspwiki.action;

import java.security.Principal;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.util.UrlBuilder;
import net.sourceforge.stripes.validation.*;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.UserManager;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;
import com.ecyrd.jspwiki.ui.stripes.HandlerPermission;
import com.ecyrd.jspwiki.ui.stripes.WikiActionBeanContext;
import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;
import com.ecyrd.jspwiki.workflow.DecisionRequiredException;

/**
 * @author Andrew Jaquith
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
            UrlBuilder builder = new UrlBuilder( this.getContext().getLocale(), ViewActionBean.class, false );
            builder.addParameter( "page", "ApprovalRequiredForUserProfiles" );
            return new RedirectResolution( builder.toString() );
        }

        // Any other errors are either UI or config problems, so let the user
        // know about them
        catch( Exception e )
        {
            errors.addGlobalError( new LocalizableError( "saveError", e.getMessage() ) );
        }

        // If no errors, send user to front page (or redirected page)
        if( errors.size() == 0 )
        {
            // Set user cookie
            Principal principal = getContext().getWikiSession().getUserPrincipal();
            CookieAssertionLoginModule.setUserCookie( getContext().getResponse(), principal.getName() );
            UrlBuilder builder = new UrlBuilder( getContext().getLocale(), "/Wiki.jsp", false );
            if( m_redirect != null )
            {
                builder.addParameter( "page", m_redirect );
            }
            return new RedirectResolution( builder.toString() );
        }

        // Otherwise, send user to source page
        UrlBuilder builder = new UrlBuilder( this.getContext().getLocale(), context.getSourcePage(), false );
        builder.addParameter( "tab", "profile" );
        return new RedirectResolution( builder.toString() );
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
