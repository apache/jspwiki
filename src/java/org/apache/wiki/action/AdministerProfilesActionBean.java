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
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.EmailTypeConverter;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;

import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.TemplateResolution;
import org.apache.wiki.ui.stripes.WikiRequestContext;

/**
 * Manages the administration of UserProfiles, from the Administer Profiles
 * page. Receives a List of UserProfiles, which may include a new profile, and
 * persists the changes. Also receives an Array of Strings (login names) for
 * UserProfiles that are to be deleted, and deletes them.
 */
@UrlBinding( "/admin/Users.jsp" )
public class AdministerProfilesActionBean extends AbstractActionBean
{
    private static Logger log = LoggerFactory.getLogger( AdministerProfilesActionBean.class );

    private String[] m_deleteLoginNames;

    private List<UserProfile> m_users;

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

    public List<UserProfile> getUsers()
    {
        return m_users;
    }

    public void setUsers( List<UserProfile> profiles )
    {
        this.m_users = profiles;
    }

    @HandlesEvent( "save" )
    @WikiRequestContext( "adminProfiles" )
    public Resolution saveChanges() throws WikiSecurityException
    {
        UserDatabase db = super.getContext().getEngine().getUserManager().getUserDatabase();

        // Apply any changes to existing profiles (and create new ones)
        for( UserProfile users : m_users )
        {

            // Look up profile; create new if not found
            UserProfile existingProfile;
            try
            {
                existingProfile = db.findByLoginName( users.getLoginName() );
            }
            catch( NoSuchPrincipalException e )
            {
                existingProfile = this.getContext().getEngine().getUserManager().getUserDatabase().newProfile();
            }

            // Make changes to things that have changed
            existingProfile.setLoginName( users.getLoginName() );
            existingProfile.setFullname( users.getFullname() );
            existingProfile.setEmail( users.getEmail() );
            if( users.getPassword() != null && users.getPassword().length() > 0 )
            {
                existingProfile.setPassword( users.getPassword() );
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
        return new TemplateResolution( "admin/Admin.jsp").addParameter( "tab", "users" );
    }

    /**
     * Retrieves the active set of users, then returns a TemplateResolution to
     * the display JSP {@code admin/Admin.jsp}, the {@code users} tab.
     * 
     * @return the resolution
     */
    @DefaultHandler
    @DontValidate
    @HandlesEvent( "view" )
    @HandlerPermission( permissionClass = AllPermission.class, target = "*" )
    public Resolution view() throws WikiSecurityException
    {
        // Populate the user list
        UserDatabase db = getContext().getEngine().getUserManager().getUserDatabase();
        Principal[] wikiNames = db.getWikiNames();
        m_users = new ArrayList<UserProfile>();
        for ( Principal wikiName : wikiNames )
        {
            try
            {
                UserProfile user = db.findByWikiName( wikiName.getName() );
                m_users.add( user );
            }
            catch ( NoSuchPrincipalException e )
            {
                // Should not happen
                log.error( "Could not find user with wikiName = "
                           + wikiName.getName() + ". Is the database corrupted? " );
            }
        }
        
        // Forward to the template JSP
        return new TemplateResolution( "admin/Admin.jsp").addParameter( "tab", "users" );
    }
}
