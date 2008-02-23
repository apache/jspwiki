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

import java.io.InputStream;
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
     *  
     *  @param att Attachment object to add new data to
     *  @param data The stream from which the provider should read the data
     *  @throws IOException If writing fails
     *  @throws ProviderException If there are other errors.
     */
    public void putAttachmentData( Attachment att, InputStream data )
        throws ProviderException,
               IOException;

    /**
     *  Get attachment data.
     *  
     *  @param att The attachment
     *  @return An InputStream which you contains the raw data of the object. It's your
     *          responsibility to close it.
     *  @throws ProviderException If the attachment cannot be found
     *  @throws IOException If the attachment cannot be opened
     */

    public InputStream getAttachmentData( Attachment att )
        throws ProviderException,
               IOException;

    /**
     *  Lists all attachments attached to a page.
     *
     *  @param page The page to list the attachments from.
     *  @return A collection of Attachment objects.  May be empty, but never null.
     *  @throws ProviderException If something goes wrong when listing the attachments.
     */

    public Collection<Attachment> listAttachments( WikiPage page )
        throws ProviderException;

    /**
     * Finds attachments based on the query.
     * @param query An array of QueryItem objects to search for
     * @return A Collection of Attachment objects.  May be empty, but never null.
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
     *  @throws ProviderException If something goes wrong.
     */
    public List<Attachment> listAllChanged( Date timestamp )
        throws ProviderException;

    /**
     *  Returns info about an attachment.
     *  
     *  @param page The parent page
     *  @param name The name of the attachment
     *  @param version The version of the attachment (it's okay to use WikiPage.LATEST_VERSION to find the latest one)
     *  @return An attachment object
     *  @throws ProviderException If the attachment cannot be found or some other error occurs.
     */
    public Attachment getAttachmentInfo( WikiPage page, String name, int version )
        throws ProviderException;

    /**
     *  Returns version history.  Each element should be
     *  an Attachment.
     *  
     *  @param att The attachment for which to find the version history for.
     *  @return A List of Attachment objects.
     */
    public List getVersionHistory( Attachment att );

    /**
     *  Removes a specific version from the repository.  The implementations
     *  should really do no more security checks, since that is the domain
     *  of the AttachmentManager.  Just delete it as efficiently as you can.
     *
     *  @since 2.0.19.
     *
     *  @param att Attachment to be removed.  The version field is checked, and thus
     *             only that version is removed.
     *
     *  @throws ProviderException If the attachment cannot be removed for some reason.
     */

    public void deleteVersion( Attachment att )
        throws ProviderException;

    /**
     *  Removes an entire page from the repository.  The implementations
     *  should really do no more security checks, since that is the domain
     *  of the AttachmentManager.  Just delete it as efficiently as you can.  You should also
     *  delete any auxiliary files and directories that belong to this attachment, 
     *  IF they were created
     *  by this provider.
     *
     *  @since 2.0.17.
     *
     *  @param att Attachment to delete.
     *
     *  @throws ProviderException If the page could not be removed for some reason.
     */
    public void deleteAttachment( Attachment att )
        throws ProviderException;
   
    /**
     * Move all the attachments for a given page so that they are attached to a
     * new page.
     *
     * @param oldParent Name of the page we are to move the attachments from.
     * @param newParent Name of the page we are to move the attachments to.
     *
     * @throws ProviderException If the attachments could not be moved for some
     *                           reason.
     */
    public void moveAttachmentsForPage( String oldParent,
                                        String newParent )
        throws ProviderException;
}


