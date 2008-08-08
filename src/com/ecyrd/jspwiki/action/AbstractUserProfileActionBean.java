package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.UserManager;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;

/**
 * <p>
 * Abstract WikiActionBean that provides the scaffolding needed to create and
 * validate a user profile. Subclasses {@link LoginActionBean} and
 * {@link UserProfileActionBean} rely on the fields and event handlers in this
 * class. This abstract class contains no handler methods; subclasses must
 * implement them.
 * </p>
 * 
 * @author Andrew Jaquith
 */
public abstract class AbstractUserProfileActionBean extends AbstractActionBean
{
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

    /**
     * Pre-action that loads the UserProfile before user-supplied parameters are
     * bound to the ActionBean.
     * 
     * @return <code>null</code>, always
     */
    @Before( stages = LifecycleStage.BindingAndValidation )
    public Resolution initUserProfile()
    {
        // Retrieve the user profile
        WikiEngine engine = getEngine();
        WikiSession session = getWikiSession();
        UserManager manager = engine.getUserManager();
        m_profile = manager.getUserProfile( session );

        // Null out the password, so that we don't re-encrypt it by accident
        m_profile.setPassword( null );
        return null;
    }

    public void setPasswordAgain( String password )
    {
        m_passwordAgain = password;
    }

    @ValidateNestedProperties( { @Validate( field = "loginName", required = true, maxlength = 100 ),
                                @Validate( field = "fullname", required = true, maxlength = 100 ),
                                @Validate( field = "password", required = false, minlength = 8, maxlength = 100 ),
                                @Validate( field = "email", required = false, converter = EmailTypeConverter.class ) } )
    public void setProfile( UserProfile profile )
    {
        m_profile = profile;
    }

    /**
     * Abstract "save profile" methods that subclasses must implement.
     * @return
     */
    @HandlesEvent( "saveProfile" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${engine.applicationName}", actions = WikiPermission.EDIT_PROFILE_ACTION )
    public abstract Resolution save();
    
    /**
     * After all fields validate correctly, this method verifies that the user
     * has the correct permissions to save the UserProfile, and checks that the
     * supplied full name or login name don't collide with those used by
     * somebody else.
     * 
     * @param errors the current validation errors for this ActionBean
     */
    @ValidationMethod( on="saveProfile",  when = ValidationState.NO_ERRORS )
    public void validateNoCollision( ValidationErrors errors )
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
                errors.add( "profile.loginName", new LocalizableError( "nameCollision" ) );
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
                errors.add( "profile.fullname", new LocalizableError( "nameCollision" ) );
            }
        }
        catch( NoSuchPrincipalException e )
        {
        }
    }

    /**
     * After all fields validate correctly, this method validates that the user
     * account is not spam.
     * 
     * @param errors the current validation errors for this ActionBean
     */
    @ValidationMethod( on="saveProfile", when = ValidationState.NO_ERRORS )
    public void validateNotSpam( ValidationErrors errors )
    {
        // TODO: code this up!
    }

    /**
     * If the user profile is new, this method verifies that the user has
     * supplied matching passwords.
     * 
     * @param errors the current validation errors for this ActionBean
     */
    @ValidationMethod( on="saveProfile", when = ValidationState.ALWAYS )
    public void validatePasswords( ValidationErrors errors )
    {
        // All new profiles must have a supplied password
        if( m_profile.isNew() )
        {
            if( m_profile.getPassword() == null )
            {
                errors.add( "profile.password", new ScopedLocalizableError( "validation.required", "valueNotPresent" ) );
            }
        }

        // If a password was supplied, the same value must also have been passed
        // to passwordAgain
        if( m_profile.getPassword() != null )
        {
            if( !m_profile.getPassword().equals( m_passwordAgain ) )
            {
                errors.add( "profile.password", new ScopedLocalizableError( "validation.required", "noPasswordMatch" ) );
            }
        }
    }
}
