package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
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

/**
 * @author Andrew Jaquith
 */
@UrlBinding("/UserProfile.jsp")
public class UserProfileActionBean extends AbstractActionBean
{
    private UserProfile m_profile = null;
    private String m_passwordAgain = null;

    public String getPasswordAgain()
    {
        return m_passwordAgain;
    }
    
    public UserProfile getProfile() 
    {
        return m_profile;
    }
    
    /**
     * Before user-supplied parameters are bound to the ActionBean, this method loads the
     * UserProfile.
     * @return <code>null</code>, always
     */
    @Before(stages=LifecycleStage.BindingAndValidation)
    public Resolution init()
    {
        // Retrieve the user profile
        WikiEngine engine = getEngine();
        WikiSession session = getWikiSession();
        UserManager manager = engine.getUserManager();
        m_profile = manager.getUserProfile(session);
        
        // Null out the password, so that we don't re-encrypt it by accident
        m_profile.setPassword( null );
        return null;
    }

    /**
     * Saves the user profile to the wiki's UserDatabase.
     * 
     * @return a {@link net.sourceforge.stripes.action.ForwardResolution} back
     *         to itself if the save activity returned errors, or
     *         <code>null</code> otherwise
     */
    @HandlesEvent("save")
    @HandlerPermission(permissionClass=WikiPermission.class, target="${engine.applicationName}", actions=WikiPermission.EDIT_PROFILE_ACTION)
    public Resolution save()
    {
        Resolution r = null;
        WikiActionBeanContext context = getContext();
        try
        {
            // Save the profile
            WikiEngine engine = getEngine();
            engine.getUserManager().setUserProfile( getWikiSession(), m_profile);
            CookieAssertionLoginModule.setUserCookie(context.getResponse(), m_profile.getFullname());
            r = new RedirectResolution("/");
        }
        catch (Exception e)
        {
            ValidationErrors errors = context.getValidationErrors();
            errors.addGlobalError( new LocalizableError( "saveError", e.getMessage() ) );
            r = new ForwardResolution(UserProfileActionBean.class);
        }
        return r;
    }

    /**
     * <p>
     * Default handler method that views the user's profile. This method 
     * loads the current user's UserProfile and sets the
     * ActionBean's e-mail and fullName properties. It will override
     * user-supplied values that should not change, namely the
     * <code>loginName</code> and <code>password</code> fields
     * if AuthenticationManager is using container-managed authentication
     * and accounts aren't shared with JSPWiki. It will also
     * set the <code>e-mail</code> field if a value was not supplied by the
     * user as a request parameter. If a profile does not already exist, 
     * this method does nothing, with
     * the caveat that if the user is authenticated via the container, the
     * container identifer (the value returned by the <code>getRemoteUser</code>
     * or the <code>getUserPrincipal</code>).
     * </p>
     */

    public void setPasswordAgain(String password)
    {
        m_passwordAgain = password;
    }
    
    @ValidateNestedProperties( {
                               @Validate(field="loginName", required=true, maxlength=100), 
                               @Validate(field="fullname", required=true, maxlength=100),
                               @Validate(field="password", required = false, minlength = 8, maxlength = 100),
                               @Validate(field="email", required = false, converter = EmailTypeConverter.class) } )
    public void setProfile(UserProfile profile)
    {
        m_profile = profile;
    }
    
    /**
     * If the user profile is new, this method verifies that the user
     * has supplied matching passwords.
     * @param errors
     *            the current validation errors for this ActionBean
     */
    @ValidationMethod(when = ValidationState.ALWAYS)
    public void validatePasswords(ValidationErrors errors)
    {
        // All new profiles must have a supplied password
        if ( m_profile.isNew() )
        {
            if ( m_profile.getPassword() == null )
            {
                errors.add( "profile.password", new ScopedLocalizableError( "validation.required", "valueNotPresent" ) );
            }
        }
        
        // If a password was supplied, the same value must also have been passed to passwordAgain
        if ( m_profile.getPassword() != null )
        {
            if ( !m_profile.getPassword().equals( m_passwordAgain ) )
            {
                errors.add( "profile.password", new ScopedLocalizableError( "validation.required", "noPasswordMatch" ) );
            }
        }
    }


    /**
     * After all fields validate correctly, this method verifies that the user
     * has the correct permissions to save the UserProfile, and checks that the
     * supplied full name or login name don't collide with those used by
     * somebody else.
     * 
     * @param errors
     *            the current validation errors for this ActionBean
     */
    @ValidationMethod(when = ValidationState.NO_ERRORS)
    public void validateNoCollision(ValidationErrors errors)
    {
        WikiEngine engine = getEngine();
        WikiSession session = getWikiSession();
        UserManager manager = getEngine().getUserManager();
        UserDatabase database = manager.getUserDatabase();

        // Locate the old user profile
        UserProfile oldProfile = manager.getUserProfile( session );

        // If container authenticated, override the login name and password
        // The login name should always be the one provided by the container
        AuthenticationManager authMgr = engine.getAuthenticationManager();
        if ( authMgr.isContainerAuthenticated() )
        {
            m_profile.setLoginName( oldProfile.getLoginName() );
            m_profile.setPassword( null );
        }
        
        // User profiles that may already have fullname or loginname
        UserProfile otherProfile;
        try
        {
            otherProfile = database.findByLoginName( m_profile.getLoginName() );
            if ( otherProfile != null && !otherProfile.equals( oldProfile ) )
            {
                errors.add("profile.loginName", new LocalizableError( "nameCollision" ) );
            }
        }
        catch (NoSuchPrincipalException e)
        {
        }
        try
        {
            otherProfile = database.findByFullName( m_profile.getFullname() );
            if (otherProfile != null && !otherProfile.equals(oldProfile))
            {
                errors.add("profile.fullname", new LocalizableError( "nameCollision" ) );
            }
        }
        catch (NoSuchPrincipalException e)
        {
        }
    }
    
    /**
     * Default handler that forwards the user back to itself.
     * @return the resolution
     */
    @HandlesEvent("view")
    @DefaultHandler
    @WikiRequestContext("profile")
    public Resolution view()
    {
        return new ForwardResolution(ViewActionBean.class);
    }
    
}
