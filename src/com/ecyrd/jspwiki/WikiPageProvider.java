/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.util.Properties;
import java.util.Collection;
import java.util.Date;

/**
 *  Each Wiki page provider should implement this interface.
 *  <P>
 *  You can build whatever page providers based on this, just
 *  leave the unused methods do something useful.
 *
 *  <P>
 *  FIXME: In reality we should have an AbstractWikiPageProvider,
 *  which would provide intelligent backups for subclasses.
 */
public interface WikiPageProvider
{
    /**
     *  Initializes the page provider.
     */
    public void initialize( Properties properties ) throws NoRequiredPropertyException;

    /**
     *  Return the page text for page "page".
     *
     *  @return text content, or null if the page does not exist.
     */
    public String getPageText( String page );

    /**
     *  Attempts to save the page text for page "page".
     */
    public void putPageText( WikiPage page, String text );

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
     *  @deprecated
     */

    public Collection getAllPages();

    /**
     *  Gets a list of recent changes.
     *  @since 1.6.4
     */

    public Collection getAllChangedSince( Date date );

    /**
     *  Gets the number of pages.
     *  @since 1.6.4
     */

    public int getPageCount();

    /**
     *  Returns version history.  Each element should be
     *  a WikiPage.
     */

    public Collection getVersionHistory( String page );

    /**
     *  Gets a specific version out of the repository.
     */

    public String getPageText( String page, int version );

    /**
     *  Return a valid HTML string for information.  May
     *  be anything.
     *  @since 1.6.4
     */

    public String getProviderInfo();
}


