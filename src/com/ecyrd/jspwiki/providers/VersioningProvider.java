/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.providers;

/**
 *  This is a provider interface which providers can implement, if they
 *  support fast checks of versions.
 *  <p>
 *  Note that this interface is pretty much a hack to support certain functionality
 *  before a complete refactoring of the complete provider interface.  Please
 *  don't bug me too much about it...
 *  
 *  @author jalkanen
 *
 *  @since 2.3.29
 */
public interface VersioningProvider
{
    /**
     *  Return true, if page with a particular version exists.
     */

    public boolean pageExists( String page, int version );
}
