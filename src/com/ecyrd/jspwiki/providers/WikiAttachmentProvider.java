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
package com.ecyrd.jspwiki.providers;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;

/**
 *  Defines an attachment provider - a class which is capable of saving
 *  binary data as attachments.
 *  <P>
 *  The difference between this class and WikiPageProvider is that there
 *  PageProviders handle Unicode text, whereas we handle binary data.
 *  While there are quite a lot of similarities in how we handle
 *  things, many providers can really use just one.  In addition,
 *  since binary files can be really large, we rely on
 *  Input/OutputStreams.
 *
 *  @author Erik Bunn
 *  @author Janne Jalkanen
 */
public interface WikiAttachmentProvider
    extends WikiProvider
{
    /**
     *  Put new attachment data.
     */
    public void putAttachmentData( Attachment att, InputStream data )
        throws ProviderException,
               IOException;

    /**
     *  Get attachment data.
     */

    public InputStream getAttachmentData( Attachment att )
        throws ProviderException,
               IOException;

    /**
     *  Lists all attachments attached to a page.
     *
     *  @return A collection of Attachment objects.  May be empty, but never null.
     */

    public Collection listAttachments( WikiPage page )
        throws ProviderException;

    /**
     * Finds attachments based on the query.
     */
    public Collection findAttachments( QueryItem[] query );

    /**
     *  Lists changed attachments since given date.  Can also be used to fetch
     *  a list of all pages.
     *  <P>
     *  This is different from WikiPageProvider, where you basically get a list
     *  of all pages, then sort them locally.  However, since some providers
     *  can be more efficient in locating recently changed files (like any database) 
     *  than our non-optimized Java
     *  code, it makes more sense to fetch the whole list this way.
     *  <P>
     *  To get all files, call this with Date(0L);
     *
     *  @param timestamp List all files from this date onward.
     *  @return A List of Attachment objects, in most-recently-changed first order.
     */
    public List listAllChanged( Date timestamp )
        throws ProviderException;

    /**
     *  Returns info about an attachment.
     */
    public Attachment getAttachmentInfo( WikiPage page, String name, int version )
        throws ProviderException;

    /**
     *  Returns version history.  Each element should be
     *  an Attachment.
     */
    public Collection getVersionHistory( Attachment att );
}


