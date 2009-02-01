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

import java.util.List;

import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.ui.stripes.WikiRequestContext;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.EmailTypeConverter;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;


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

        return new RedirectResolution( AdministerProfilesActionBean.class );
    }
}
