package com.ecyrd.jspwiki.auth;

import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.acl.AccessControlList;
import com.ecyrd.jspwiki.WikiEngine;
import java.util.Properties;

/**
 *  Provides a mean to gather permissions for different pages.  For example,
 *  a WikiPageAuthorizer could fetch the authorization data from the
 *  page in question.
 *
 *  @author Janne Jalkanen
 */
public interface WikiAuthorizer
{
    /**
     *  Initializes a WikiAuthorizer.
     *
     *  @param engine The WikiEngine that owns this authorizer.
     *  @param properties A bunch of properties.
     */
    public void initialize( WikiEngine engine, Properties properties );
   
    /**
     *  Returns the permissions for this page.
     */
    public AccessControlList getPermissions( WikiPage page );

    /**
     *  Returns the default permissions.  For example, fetch always
     *  from a page called "DefaultPermissions".
     */
    public AccessControlList getDefaultPermissions();
}
