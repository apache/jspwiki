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

}
