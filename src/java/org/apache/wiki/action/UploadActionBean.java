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
import java.util.List;

import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;

public class UploadActionBean extends AbstractPageActionBean
{
    private static final Logger log = LoggerFactory.getLogger( UploadActionBean.class );

    private List<FileBean> m_newAttachments;

    private String m_changeNote = null;

    /**
     * Returns the new attachments uploaded by the user.
     * @return the new files to attach
     */
    public List<FileBean> getNewAttachments()
    {
        return m_newAttachments;
    }
    
    /**
     * Sets the set of new attachments that should be saved when the
     * {@link #upload()} event is executed.
     * 
     * @param newAttachments the new files to attach
     */
    public void setNewAttachments( List<FileBean> newAttachments )
    {
        m_newAttachments = newAttachments;
    }

    /**
     * Sets the changenote for this upload; usually a short comment.
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
     * Handler method that uploads a new attachment to the ViewActionBean.
     * 
     * @return Resolution
     */
    @HandlesEvent( "upload" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.UPLOAD_ACTION )
    @WikiRequestContext( "upload" )
    public Resolution upload() throws Exception
    {
        for( FileBean attachment : m_newAttachments )
        {
            if ( attachment != null )
            {
                executeUpload( attachment );
                log.debug( "Executed upload; " + m_newAttachments.size() + " attachments found." );
            }
        }

        return new RedirectResolution( ViewActionBean.class, "attachments" ).addParameter( "page", getPage().getName() );
    }
    
    @ValidationMethod
    public void validateFileType( ValidationErrors errors )
    {
        AttachmentManager mgr = getContext().getEngine().getAttachmentManager();
        for ( FileBean attachment : m_newAttachments )
        {
            if ( attachment != null )
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
                
                if ( !mgr.isFileTypeAllowed( filename ) )
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

        //
        // Check whether we already have this kind of a page.
        // If the "page" parameter already defines an attachment
        // name for an update, then we just use that file.
        // Otherwise we create a new attachment, and use the
        // filename given. Incidentally, this will also mean
        // that if the user uploads a file with the exact
        // same name than some other previous attachment,
        // then that attachment gains a new version.
        //

        Attachment att = mgr.getAttachmentInfo( context.getPage().getName() );

        if( att == null )
        {
            String contentType = "application/octet-stream"; // FIXME: This is not a good guess
            WikiPath path = context.getPage().getPath().resolve(filename);
            att = engine.getContentManager().addPage( path, contentType );
            created = true;
        }

        if( user != null )
        {
            att.setAuthor( user.getName() );
        }

        if( m_changeNote != null && m_changeNote.length() > 0 )
        {
            att.setAttribute( WikiPage.CHANGENOTE, m_changeNote );
        }

        try
        {
            engine.getAttachmentManager().storeAttachment( att, data );
        }
        catch( ProviderException pe )
        {
            // this is a kludge, the exception that is caught here contains
            // the i18n key
            // here we have the context available, so we can
            // internationalize it properly :
            throw new ProviderException( context.getBundle( InternationalizationManager.CORE_BUNDLE )
                .getString( pe.getMessage() ), pe );
        }
        
        // Close the stream and delete the filebean, since we're done with it
        data.close();
        filebean.delete();

        log.info( "User " + user + " uploaded attachment to " + getPage().getName() + " called " + filename + ", size " + att.getSize() );

        return created;
    }

}
