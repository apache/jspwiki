/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.providers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.search.QueryItem;

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
    void putAttachmentData( Attachment att, InputStream data )
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

    InputStream getAttachmentData( Attachment att )
        throws ProviderException,
               IOException;

    /**
     *  Lists all attachments attached to a page.
     *
     *  @param page The page to list the attachments from.
     *  @return A collection of Attachment objects.  May be empty, but never null.
     *  @throws ProviderException If something goes wrong when listing the attachments.
     */
    List< Attachment > listAttachments( WikiPage page )
        throws ProviderException;

    /**
     * Finds attachments based on the query.
     * @param query An array of QueryItem objects to search for
     * @return A Collection of Attachment objects.  May be empty, but never null.
     */
    Collection< Attachment > findAttachments( QueryItem[] query );

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
    List<Attachment> listAllChanged( Date timestamp )
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
    Attachment getAttachmentInfo( WikiPage page, String name, int version )
        throws ProviderException;

    /**
     *  Returns version history.  Each element should be
     *  an Attachment.
     *  
     *  @param att The attachment for which to find the version history for.
     *  @return A List of Attachment objects.
     */
    List<Attachment> getVersionHistory( Attachment att );

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

    void deleteVersion( Attachment att )
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
    void deleteAttachment( Attachment att )
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
    void moveAttachmentsForPage( String oldParent,
                                        String newParent )
        throws ProviderException;
}


