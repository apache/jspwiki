package com.ecyrd.jspwiki;

import java.util.Properties;
import java.util.Collection;
import java.util.Date;

/**
 *  Each Wiki page provider should implement this interface.
 */
public interface WikiPageProvider
{
    /**
     *  Initializes the page provider.
     */
    public void initialize( Properties properties ) throws NoRequiredPropertyException;

    /**
     *  Return the page text for page "page".
     */
    public String getPageText( String page );

    /**
     *  Attempts to save the page text for page "page".
     */
    public void putPageText( String page, String text );

    /**
     *  Return true, if page exists.
     */

    public boolean pageExists( String page );

    /**
     *  Finds pages based on the query.
     */
    public Collection findPages( QueryItem[] query );

    /**
     *  Returns info about the page.
     */
    public WikiPage getPageInfo( String page );

    /**
     *  Returns all pages.  Each element in the returned
     *  Collection should be a WikiPage.
     */

    public Collection getAllPages();

    /**
     *  Returns version history.  Each element should be
     *  a WikiPage.
     */

    public Collection getVersionHistory( String page );

    /**
     *  Gets a specific version out of the repository.
     */

    public String getPageText( String page, int version );
}


