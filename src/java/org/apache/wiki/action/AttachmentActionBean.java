/* 
    JSPWiki - a JSP-based WikiWiki clone.

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

package org.apache.wiki.action;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;

@UrlBinding( "/attach/{page}" )
public class AttachmentActionBean extends AbstractPageActionBean
{
    private static final Logger log = LoggerFactory.getLogger( AttachmentActionBean.class );

    private List<FileBean> m_newAttachments;

    private String m_changeNote = null;

    private int m_version = WikiProvider.LATEST_VERSION;

    /**
     * Returns the version of the attachment to download. If not set by
     * {@link #setVersion(int)}, returns {@link WikiProvider#LATEST_VERSION}.
     * 
     * @return the version
     */
    public int getVersion()
    {
        return m_version;
    }

    /**
     * Sets the version of the attachment to download.
     * 
     * @param version the version to download
     */
    @Validate( required = false )
    public void setVersion( int version )
    {
        m_version = version;
    }

    /**
     * Returns the new attachments uploaded by the user.
     * 
     * @return the new files to attach
     */
    public List<FileBean> getNewAttachments()
    {
        return m_newAttachments;
    }

    /**
     * Sets the new attachments that should be saved when the
     * {@link #upload()} event is executed.
     * 
     * @param newAttachments the new files to attach
     */
    public void setNewAttachments( List<FileBean> newAttachments )
    {
        m_newAttachments = newAttachments;
    }

    /**
     * Sets the changenote for when the {@link #upload()} event is executed.
     * Usually, this is a short comment.
     * 
     * @param changenote the change note
     */
    @Validate( required = false )
    public void setChangenote( String changenote )
    {
        m_changeNote = changenote;
    }

    /*
     * Returns the changenote for this upload.
     */
    public String getChangenote()
    {
        return m_changeNote;
    }

    /**
     * Handler method that uploads a new attachment.
     * 
     * @return Resolution
     */
    @HandlesEvent( "upload" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.UPLOAD_ACTION )
    @WikiRequestContext( "upload" )
    public Resolution upload() throws Exception
    {
        for( FileBean attachment : m_newAttachments )
        {
            if( attachment != null )
            {
                executeUpload( attachment );
                log.debug( "Executed upload; " + m_newAttachments.size() + " attachments found." );
            }
        }

        return new RedirectResolution( ViewActionBean.class, "attachments" ).addParameter( "page", getPage().getName() );
    }

    /**
     * Handler method that downloads an attachment.
     * 
     * @return a streaming resolution
     * @throws Exception
     */
    @DefaultHandler
    @HandlesEvent( "download" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "download" )
    public Resolution download() throws Exception
    {
        Attachment att = (Attachment)getPage();
        StreamingResolution r = new StreamingResolution( att.getContentType(), att.getContentAsStream() );
        r.setFilename( att.getPath().getName() );
        
        // Add caching properties to response
        HttpServletResponse response = getContext().getResponse();
        if ( att.getLastModified() != null )
        {
            response.addDateHeader("Last-Modified", att.getLastModified().getTime());
        }
        if( !att.isCacheable() )
        {
            response.addHeader( "Pragma", "no-cache" );
            response.addHeader( "Cache-control", "no-cache" );
        }

        // If provider reports file size, add to response
        if( att.getSize() >= 0 )
        {
            response.setContentLength( (int)att.getSize() );
        }
        
        // Send it!
        if(log.isDebugEnabled())
        {
            HttpServletRequest req = getContext().getRequest();
            String msg = "Attachment "+att.getFileName()+" sent to "+req.getRemoteUser()+" on "+req.getRemoteAddr();
            log.debug( msg );
        }
        return r;
    }

    /**
     * Validates that a requested download exists at the path supplied. This
     * method fires when the {@link #download()} handler executes.
     * 
     * @param errors the current validation errors collection
     */
    @ValidationMethod( on = "download" )
    public void validateDownloadedFile( ValidationErrors errors )
    {
        // Check if the page + version exists
        WikiPath path = getPage().getPath();
        ContentManager manager = getContext().getEngine().getContentManager();
        try
        {
            WikiPage page = manager.getPage( path, m_version );
            // If the page isn't actually an attachment, add a validation error
            if ( !page.isAttachment() )
            {
                // FIXME: bogus labels
                errors.add( "download", new LocalizableError( "notAnAttachment", path.toString(), m_version ) );
                return;
            }
        }
        catch( PageNotFoundException e )
        {
            // FIXME: bogus labels
            errors.add( "cantFindAttachment", new LocalizableError( e.getMessage(), path.toString(), m_version ) );
            return;
        }
        catch( ProviderException e )
        {
            // FIXME: bogus labels
            errors.add( "error", new LocalizableError( e.getMessage(), path.toString(), m_version ) );
            return;
        }
    }

    /**
     * Validates that an uploaded attachment has a clean file name, and isn't
     * using a prohibited file extension. This method files when the
     * {@link #upload()} handler executes.
     * 
     * @param errors the current validation errors collection
     */
    @ValidationMethod( on = "upload" )
    public void validateUploadedFileType( ValidationErrors errors )
    {
        AttachmentManager mgr = getContext().getEngine().getAttachmentManager();
        for( FileBean attachment : m_newAttachments )
        {
            if( attachment != null )
            {
                // Clean the file name before validating
                String filename = attachment.getFileName();
                try
                {
                    filename = AttachmentManager.cleanFileName( filename );
                }
                catch( WikiException e )
                {
                    // Error message returns the i18n key name
                    errors.add( "newAttachments", new LocalizableError( e.getMessage(), filename ) );
                }

                if( !mgr.isFileTypeAllowed( filename ) )
                {
                    errors.add( "newAttachments", new LocalizableError( "attach.bad.filetype", filename ) );
                }
            }
        }
    }

    /**
     * Uploads a single FileBean to the AttachmentManager. This method assumes
     * that the user has permissions to do so; handler method callers should use
     * the {@link HandlerPermission} annotation to force permission checks.
     * 
     * @param filebean the FileBean containing the file to be uploaded
     * @return <code>true</code> if upload results in the creation of a new
     *         page; <code>false</code> otherwise
     * @throws IOException If there is a problem in the upload.
     * @throws ProviderException If there is a problem in the backend.
     */
    private boolean executeUpload( FileBean filebean ) throws Exception
    {
        boolean created = false;
        WikiContext context = getContext();
        WikiEngine engine = context.getEngine();
        AttachmentManager mgr = engine.getAttachmentManager();

        // Get the file name, size etc from the FileBean
        InputStream data = filebean.getInputStream();
        String filename = filebean.getFileName();

        // Cleanse the file name
        filename = AttachmentManager.cleanFileName( filename );
        log.debug( "file=" + filename );

        // Get the name of the user uploading the file
        Principal user = context.getCurrentUser();

        // Look up the attachment for this page
        WikiPage page = context.getPage();
        Attachment attachment = null;
        Iterator<WikiPage> attachments = mgr.listAttachments( page ).iterator();
        while ( attachments.hasNext() )
        {
            WikiPage currentAttachment = attachments.next();
            if( filename.equals( currentAttachment.getPath().getName() ) )
            {
                attachment = (Attachment) currentAttachment;
            }
        }

        if( attachment == null )
        {
            WikiPath path = WikiPath.valueOf( page.getPath().toString() + "/" + filename );
            attachment = engine.getContentManager().addPage( path, filebean.getContentType() );
            created = true;
        }

        if( user != null )
        {
            attachment.setAuthor( user.getName() );
        }

        if( m_changeNote != null && m_changeNote.length() > 0 )
        {
            attachment.setAttribute( WikiPage.CHANGENOTE, m_changeNote );
        }

        try
        {
            engine.getAttachmentManager().storeAttachment( attachment, data );
        }
        catch( ProviderException pe )
        {
            // this is a kludge, the exception that is caught here contains
            // the i18n key
            // here we have the context available, so we can
            // internationalize it properly :
            throw new ProviderException( context.getBundle( InternationalizationManager.CORE_BUNDLE ).getString( pe.getMessage() ),
                                         pe );
        }

        // Close the stream and delete the filebean, since we're done with it
        data.close();
        filebean.delete();

        log.info( "User " + user + " uploaded attachment to " + getPage().getName() + " called " + filename + ", size "
                  + filebean.getSize() );

        return created;
    }

}
