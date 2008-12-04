package com.ecyrd.jspwiki.action;

import java.util.List;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.EmailTypeConverter;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;

import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;

/**
 * Manages the administration of UserProfiles, from the Administer Profiles
 * page. Receives a List of UserProfiles, which may include a new profile, and
 * persists the changes. Also receives an Array of Strings (login names) for
 * UserProfiles that are to be deleted, and deletes them.
 * 
 * @author Andrew Jaquith
 */
@UrlBinding( "/AdministerProfiles.jsp" )
public class AdministerProfilesActionBean extends AbstractActionBean
{

    private String[] m_deleteLoginNames;

    private List<UserProfile> m_profiles;

    @ValidateNestedProperties( { @Validate( field = "loginName", required = true, minlength = 3, maxlength = 50 ),
                                @Validate( field = "password", required = true, minlength = 6, maxlength = 128 ),
                                @Validate( field = "fullName", required = true, minlength = 3, maxlength = 50 ),
                                @Validate( field = "wikiName", required = true, minlength = 3, maxlength = 50 ),
                                @Validate( field = "email", required = false, converter = EmailTypeConverter.class ) } )
    public String[] getDeleteLoginNames()
    {
        return m_deleteLoginNames;
    }

    public void setDeleteLoginNames( String[] deleteLoginNames )
    {
        m_deleteLoginNames = deleteLoginNames;
    }

    public List<UserProfile> getUserProfiles()
    {
        return m_profiles;
    }

    public void setUserProfiles( List<UserProfile> profiles )
    {
        this.m_profiles = profiles;
    }

    @DefaultHandler
    @HandlesEvent( "save" )
    @WikiRequestContext("adminProfiles")
    public Resolution saveChanges() throws WikiSecurityException
    {
        UserDatabase db = super.getContext().getEngine().getUserManager().getUserDatabase();

        // Apply any changes to existing profiles (and create new ones)
        for( UserProfile profile : m_profiles )
        {

            // Look up profile; create new if not found
            UserProfile existingProfile;
            try
            {
                existingProfile = db.findByLoginName( profile.getLoginName() );
            }
            catch( NoSuchPrincipalException e )
            {
                existingProfile = this.getContext().getEngine().getUserManager().getUserDatabase().newProfile();
            }

            // Make changes to things that have changed
            existingProfile.setLoginName( profile.getLoginName() );
            existingProfile.setFullname( profile.getFullname() );
            existingProfile.setEmail( profile.getEmail() );
            if( profile.getPassword() != null && profile.getPassword().length() > 0 )
            {
                existingProfile.setPassword( profile.getPassword() );
            }
            db.save( existingProfile );
        }

        // Then, if the user checked anyone off to be deleted, delete them
        if( m_deleteLoginNames != null )
        {
            for( String loginName : m_deleteLoginNames )
            {
                try
                {
                    db.deleteByLoginName( loginName );
                }
                catch( NoSuchPrincipalException e )
                {
                    throw new WikiSecurityException( e.getMessage() );
                }
            }
        }

        return new RedirectResolution( "/AdministerProfiles.jsp" );
    }
}
