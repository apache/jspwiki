package com.ecyrd.jspwiki.auth;

import java.util.Properties;
import com.ecyrd.jspwiki.NoRequiredPropertyException;

/**
 *  Defines the interface for connecting to different authentication
 *  services.
 *
 *  @since 2.1.11.
 *  @author Erik Bunn
 */
public interface WikiAuthenticator
{
    /**
     * Initializes the WikiAuthenticator based on values from a Properties
     * object.
     */
    public void initialize( Properties props )
        throws NoRequiredPropertyException;

    /**
     * Authenticates a user, using the name and password present in the
     * parameter.
     *
     * @return true, if this is a valid UserProfile, false otherwise.
     */
    public boolean authenticate( UserProfile wup );
}
