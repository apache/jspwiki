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
    public void initialize( Properties properties );

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

    public Collection getRecentChanges();

    public Collection findPages( QueryItem[] query );

    /**
     *  Returns the date the page was last changed.
     */
    public Date pageLastChanged( String page );
}


