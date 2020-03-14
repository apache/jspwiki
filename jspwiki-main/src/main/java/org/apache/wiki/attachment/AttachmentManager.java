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
package org.apache.wiki.attachment;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.providers.WikiProvider;
import org.apache.wiki.providers.WikiAttachmentProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;


/**
 *  Provides facilities for handling attachments.  All attachment handling goes through this class.
 *  <p>
 *  The AttachmentManager provides a facade towards the current WikiAttachmentProvider that is in use.
 *  It is created by the Engine as a singleton object, and can be requested through the Engine.
 *
 *  @since 1.9.28
 */
public interface AttachmentManager {

    /** The property name for defining the attachment provider class name. */
    String PROP_PROVIDER = "jspwiki.attachmentProvider";

    /** The maximum size of attachments that can be uploaded. */
    String PROP_MAXSIZE  = "jspwiki.attachment.maxsize";

    /** A space-separated list of attachment types which can be uploaded */
    String PROP_ALLOWEDEXTENSIONS = "jspwiki.attachment.allowed";

    /** A space-separated list of attachment types which cannot be uploaded */
    String PROP_FORBIDDENEXTENSIONS = "jspwiki.attachment.forbidden";

    /** A space-separated list of attachment types which never will open in the browser. */
    String PROP_FORCEDOWNLOAD = "jspwiki.attachment.forceDownload";

    /** Name of the page cache. */
    String CACHE_NAME = "jspwiki.dynamicAttachmentCache";

    /** The capacity of the cache, if you want something else, tweak ehcache.xml. */
    int DEFAULT_CACHECAPACITY = 1_000;

    /**
     *  Returns true, if attachments are enabled and running.
     *
     *  @return A boolean value indicating whether attachment functionality is enabled.
     */
    boolean attachmentsEnabled();

    /**
     *  Gets info on a particular attachment, latest version.
     *
     *  @param name A full attachment name.
     *  @return Attachment, or null, if no such attachment exists.
     *  @throws ProviderException If something goes wrong.
     */
    default Attachment getAttachmentInfo( final String name ) throws ProviderException {
        return getAttachmentInfo( name, WikiProvider.LATEST_VERSION );
    }

    /**
     *  Gets info on a particular attachment with the given version.
     *
     *  @param name A full attachment name.
     *  @param version A version number.
     *  @return Attachment, or null, if no such attachment or version exists.
     *  @throws ProviderException If something goes wrong.
     */

    default Attachment getAttachmentInfo( final String name, final int version ) throws ProviderException {
        if( name == null ) {
            return null;
        }

        return getAttachmentInfo( null, name, version );
    }

    /**
     *  Figures out the full attachment name from the context and attachment name.
     *
     *  @param context The current WikiContext
     *  @param attachmentname The file name of the attachment.
     *  @return Attachment, or null, if no such attachment exists.
     *  @throws ProviderException If something goes wrong.
     */
    default Attachment getAttachmentInfo( final Context context, final String attachmentname ) throws ProviderException {
        return getAttachmentInfo( context, attachmentname, WikiProvider.LATEST_VERSION );
    }

    /**
     *  Figures out the full attachment name from the context and attachment name.
     *
     *  @param context The current WikiContext
     *  @param attachmentname The file name of the attachment.
     *  @param version A particular version.
     *  @return Attachment, or null, if no such attachment or version exists.
     *  @throws ProviderException If something goes wrong.
     */
    Attachment getAttachmentInfo( Context context, String attachmentname, int version ) throws ProviderException;

    /**
     *  Figures out the full attachment name from the context and attachment name.
     *
     *  @param context The current WikiContext
     *  @param attachmentname The file name of the attachment.
     *  @return Attachment, or null, if no such attachment exists.
     */
    String getAttachmentInfoName( Context context, String attachmentname );

    /**
     *  Returns the list of attachments associated with a given wiki page. If there are no attachments, returns an empty Collection.
     *
     *  @param wikipage The wiki page from which you are seeking attachments for.
     *  @return a valid collection of attachments.
     *  @throws ProviderException If there was something wrong in the backend.
     */
    List< Attachment > listAttachments( WikiPage wikipage ) throws ProviderException;

    /**
     *  Returns true, if the page has any attachments at all.  This is a convenience method.
     *
     *  @param wikipage The wiki page from which you are seeking attachments for.
     *  @return True, if the page has attachments, else false.
     */
    default boolean hasAttachments( final WikiPage wikipage ) {
        try {
            return listAttachments( wikipage ).size() > 0;
        } catch( final Exception e ) {
            Logger.getLogger( AttachmentManager.class ).info( e.getMessage(), e );
        }

        return false;
    }

    /**
     *  Check if attachement link should force a download iso opening the attachment in the browser.
     *
     *  @param name  Name of attachment to be checked
     *  @return true, if the attachment should be downloaded when clicking the link
     *  @since 2.11.0 M4
    */
    boolean forceDownload( String name );

    /**
     *  Finds a (real) attachment from the repository as an {@link InputStream}.
     *
     *  @param att Attachment
     *  @return An InputStream to read from. May return null, if attachments are disabled.
     *  @throws IOException If the stream cannot be opened
     *  @throws ProviderException If the backend fails due to some other reason.
     */
    default InputStream getAttachmentStream( final Attachment att ) throws IOException, ProviderException {
        return getAttachmentStream( null, att );
    }

    /**
     *  Returns an attachment stream using the particular WikiContext. This method should be used instead of
     *  {@link #getAttachmentStream(Attachment)}, since it also allows the DynamicAttachments to function.
     *
     *  @param ctx The Wiki Context
     *  @param att The Attachment to find
     *  @return An InputStream.  May return null, if attachments are disabled.  You must take care of closing it.
     *  @throws ProviderException If the backend fails due to some reason
     *  @throws IOException If the stream cannot be opened
     */
    InputStream getAttachmentStream( WikiContext ctx, Attachment att ) throws ProviderException, IOException;

    /**
     *  Stores a dynamic attachment.  Unlike storeAttachment(), this just stores the attachment in the memory.
     *
     *  @param ctx A WikiContext
     *  @param att An attachment to store
     */
    void storeDynamicAttachment( WikiContext ctx, DynamicAttachment att );

    /**
     *  Finds a DynamicAttachment.  Normally, you should just use {@link #getAttachmentInfo(String)} , since that will find also
     *  {@link DynamicAttachment}s.
     *
     *  @param name The name of the attachment to look for
     *  @return An Attachment, or null.
     *  @see #getAttachmentInfo(String)
     */
    DynamicAttachment getDynamicAttachment( String name );

    /**
     *  Stores an attachment that lives in the given file. If the attachment did not exist previously, this method will create it.
     *  If it did exist, it stores a new version.
     *
     *  @param att Attachment to store this under.
     *  @param source A file to read from.
     *
     *  @throws IOException If writing the attachment failed.
     *  @throws ProviderException If something else went wrong.
     */
    default void storeAttachment( final Attachment att, final File source ) throws IOException, ProviderException {
        try( final FileInputStream in = new FileInputStream( source ) ) {
            storeAttachment( att, in );
        }
    }

    /**
     *  Stores an attachment directly from a stream. If the attachment did not exist previously, this method will create it.
     *  If it did exist, it stores a new version.
     *
     *  @param att Attachment to store this under.
     *  @param in  InputStream from which the attachment contents will be read.
     *  @throws IOException If writing the attachment failed.
     *  @throws ProviderException If something else went wrong.
     */
    void storeAttachment( Attachment att, InputStream in ) throws IOException, ProviderException;

    /**
     *  Returns a list of versions of the attachment.
     *
     *  @param attachmentName A fully qualified name of the attachment.
     *
     *  @return A list of Attachments.  May return null, if attachments are
     *          disabled.
     *  @throws ProviderException If the provider fails for some reason.
     */
    List< Attachment > getVersionHistory( String attachmentName ) throws ProviderException;

    /**
     *  Returns a collection of Attachments, containing each and every attachment that is in this Wiki.
     *
     *  @return A collection of attachments.  If attachments are disabled, will return an empty collection.
     *  @throws ProviderException If something went wrong with the backend
     */
    Collection< Attachment > getAllAttachments() throws ProviderException;

    /**
     *  Returns the current attachment provider.
     *
     *  @return The current provider.  May be null, if attachments are disabled.
     */
    WikiAttachmentProvider getCurrentProvider();

    /**
     *  Deletes the given attachment version.
     *
     *  @param att The attachment to delete
     *  @throws ProviderException If something goes wrong with the backend.
     */
    void deleteVersion( Attachment att ) throws ProviderException;

    /**
     *  Deletes all versions of the given attachment.
     *
     *  @param att The Attachment to delete.
     *  @throws ProviderException if something goes wrong with the backend.
     */
    void deleteAttachment( Attachment att ) throws ProviderException;

    /**
     *  Validates the filename and makes sure it is legal.  It trims and splits and replaces bad characters.
     *
     *  @param filename file name to validate.
     *  @return A validated name with annoying characters replaced.
     *  @throws WikiException If the filename is not legal (e.g. empty)
     */
    static String validateFileName( String filename ) throws WikiException {
        if( filename == null || filename.trim().length() == 0 ) {
            Logger.getLogger( AttachmentManager.class ).error( "Empty file name given." );

            // the caller should catch the exception and use the exception text as an i18n key
            throw new WikiException(  "attach.empty.file" );
        }

        //  Some browser send the full path info with the filename, so we need
        //  to remove it here by simply splitting along slashes and then taking the path.
        final String[] splitpath = filename.split( "[/\\\\]" );
        filename = splitpath[splitpath.length-1];

        // Should help with IE 5.22 on OSX
        filename = filename.trim();

        // If file name ends with .jsp or .jspf, the user is being naughty!
        if( filename.toLowerCase().endsWith( ".jsp" ) || filename.toLowerCase().endsWith( ".jspf" ) ) {
            Logger.getLogger( AttachmentManager.class )
                  .info( "Attempt to upload a file with a .jsp/.jspf extension.  In certain cases this " +
                         "can trigger unwanted security side effects, so we're preventing it." );

            // the caller should catch the exception and use the exception text as an i18n key
            throw new WikiException(  "attach.unwanted.file"  );
        }

        //  Remove any characters that might be a problem. Most importantly - characters that might stop processing of the URL.
        return StringUtils.replaceChars( filename, "#?\"'", "____" );
    }

}
