package com.ecyrd.jspwiki.auth;

import java.util.Properties;
import java.util.List;
import java.security.Principal;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Defines an interface for grouping users to groups, etc.
 *  @author Janne Jalkanen
 *  @since 2.2.
 */
public interface UserDatabase
{
    /**
     * Initializes the WikiPrincipalist based on values from a Properties
     * object.
     */
    public void initialize( WikiEngine engine, Properties props )
        throws NoRequiredPropertyException;

    /**
     *  Returns a list of WikiGroup objects for the given Principal.
     */
    public List getGroupsForPrincipal( Principal p )
        throws NoSuchPrincipalException;

    /**
     *  Creates a principal.  This method should return either a WikiGroup
     *  or a UserProfile (or a subclass, if you need them for your own
     *  usage. 
     *  <p>
     *  It is the responsibility of the UserDatabase to implement appropriate
     *  caching of UserProfiles and other principals. 
     *  <p>
     *  Yes, all this means that user names and user groups do actually live
     *  in the same namespace.
     *  <p>
     *  FIX: UserDatabase currently requires that getPrincipal() never return null.
     */
    public WikiPrincipal getPrincipal( String name );
}
