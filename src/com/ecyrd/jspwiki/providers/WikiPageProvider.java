/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.providers;

import java.util.Properties;
import java.util.List;
import java.util.Collection;
import java.util.Date;
import java.io.IOException;

import com.ecyrd.jspwiki.*;

/**
 *  Each Wiki page provider should implement this interface.
 *  <P>
 *  You can build whatever page providers based on this, just
 *  leave the unused methods do something useful.
 *  <P>
 *  WikiPageProvider uses Strings and ints to refer to pages.  This may
 *  be a bit odd, since WikiAttachmentProviders all use Attachment
 *  instead of name/version.  We will perhaps modify these in the
 *  future.  In the mean time, name/version is quite sufficient.
 *  <P>
 *  FIXME: In reality we should have an AbstractWikiPageProvider,
 *  which would provide intelligent backups for subclasses.
 *
 *  @author Janne Jalkanen
 */
public interface WikiPageProvider
    extends WikiProvider
{
    /**
     *  Attempts to save the page text for page "page".
     */
    public void putPageText( WikiPage page, String text )
        throws ProviderException;

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
    public WikiPage getPageInfo( String page, int version )
        throws ProviderException;

    /**
     *  Returns all pages.  Each element in the returned
     *  Collection should be a WikiPage.
     */

    public Collection getAllPages()
        throws ProviderException;

    /**
     *  Gets a list of recent changes.
     *  @since 1.6.4
     */

    public Collection getAllChangedSince( Date date );

    /**
     *  Gets the number of pages.
     *  @since 1.6.4
     */

    public int getPageCount()
        throws ProviderException;

    /**
     *  Returns version history.  Each element should be
     *  a WikiPage.
     *
     *  @return A collection of wiki pages.
     */

    public List getVersionHistory( String page )
        throws ProviderException;

    /**
     *  Gets a specific version out of the repository.
     *
     *  @param page Name of the page to fetch.
     *  @param version Version of the page to fetch.
     */

    public String getPageText( String page, int version )
        throws ProviderException;

    /**
     *  Removes a specific version from the repository.  The implementations
     *  should really do no more security checks, since that is the domain
     *  of the PageManager.  Just delete it as efficiently as you can.
     *
     *  @since 2.0.17.
     *
     *  @param pageName Name of the page to be removed.
     *  @param version  Version of the page to be removed.  May be LATEST_VERSION.
     *
     *  @throws ProviderException If the page cannot be removed for some reason.
     */

    public void deleteVersion( String pageName, int version )
        throws ProviderException;

    /**
     *  Removes an entire page from the repository.  The implementations
     *  should really do no more security checks, since that is the domain
     *  of the PageManager.  Just delete it as efficiently as you can.  You should also
     *  delete any auxiliary files that belong to this page, IF they were created
     *  by this provider.
     *
     *  <P>The reason why this is named differently from
     *  deleteVersion() (logically, this method should be an
     *  overloaded version) is that I want to be absolutely sure I
     *  don't accidentally use the wrong method.  With overloading
     *  something like that happens sometimes...
     *
     *  @since 2.0.17.
     *
     *  @param pageName Name of the page to be removed completely.
     *
     *  @throws ProviderException If the page could not be removed for some reason.
     */
    public void deletePage( String pageName )
        throws ProviderException;
}


