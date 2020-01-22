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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.RedirectException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.ui.progress.ProgressItem;
import org.apache.wiki.util.HttpUtil;
import org.apache.wiki.util.TextUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.security.Permission;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 *  This is the chief JSPWiki attachment management servlet.  It is used for
 *  both uploading new content and downloading old content.  It can handle
 *  most common cases, e.g. check for modifications and return 304's as necessary.
 *  <p>
 *  Authentication is done using JSPWiki's normal AAA framework.
 *  <p>
 *  This servlet is also capable of managing dynamically created attachments.
 *
 *
 *  @since 1.9.45.
 */
public class AttachmentServlet extends HttpServlet {

    private static final int BUFFER_SIZE = 8192;

    private static final long serialVersionUID = 3257282552187531320L;

    private WikiEngine m_engine;
    private static final Logger log = Logger.getLogger( AttachmentServlet.class );

    private static final String HDR_VERSION     = "version";
    // private static final String HDR_NAME        = "page";

    /** Default expiry period is 1 day */
    protected static final long DEFAULT_EXPIRY = 1 * 24 * 60 * 60 * 1000;

    /**
     *  The maximum size that an attachment can be.
     */
    private int   m_maxSize = Integer.MAX_VALUE;

    /**
     *  List of attachment types which are allowed
     */

    private String[] m_allowedPatterns;

    private String[] m_forbiddenPatterns;

    //
    // Not static as DateFormat objects are not thread safe.
    // Used to handle the RFC date format = Sat, 13 Apr 2002 13:23:01 GMT
    //
    //private final DateFormat rfcDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    /**
     *  Initializes the servlet from WikiEngine properties.
     *
     */
    public void init( ServletConfig config ) throws ServletException {
        String tmpDir;

        m_engine         = WikiEngine.getInstance( config );
        Properties props = m_engine.getWikiProperties();

        tmpDir         = m_engine.getWorkDir()+File.separator+"attach-tmp";

        m_maxSize        = TextUtil.getIntegerProperty( props,
                AttachmentManager.PROP_MAXSIZE,
                Integer.MAX_VALUE );

        String allowed = TextUtil.getStringProperty( props,
                AttachmentManager.PROP_ALLOWEDEXTENSIONS,
                null );

        if( allowed != null && allowed.length() > 0 )
            m_allowedPatterns = allowed.toLowerCase().split("\\s");
        else
            m_allowedPatterns = new String[0];

        String forbidden = TextUtil.getStringProperty( props,
                AttachmentManager.PROP_FORBIDDENEXTENSIONS,
                null );

        if( forbidden != null && forbidden.length() > 0 )
            m_forbiddenPatterns = forbidden.toLowerCase().split("\\s");
        else
            m_forbiddenPatterns = new String[0];

        File f = new File( tmpDir );
        if( !f.exists() )
        {
            f.mkdirs();
        }
        else if( !f.isDirectory() )
        {
            log.fatal("A file already exists where the temporary dir is supposed to be: "+tmpDir+".  Please remove it.");
        }

        log.debug( "UploadServlet initialized. Using " +
                tmpDir + " for temporary storage." );
    }

    private boolean isTypeAllowed( String name )
    {
        if( name == null || name.length() == 0 ) return false;

        name = name.toLowerCase();

        for( int i = 0; i < m_forbiddenPatterns.length; i++ )
        {
            if( name.endsWith(m_forbiddenPatterns[i]) && m_forbiddenPatterns[i].length() > 0 )
                return false;
        }

        for( int i = 0; i < m_allowedPatterns.length; i++ )
        {
            if( name.endsWith(m_allowedPatterns[i]) && m_allowedPatterns[i].length() > 0 )
                return true;
        }

        return m_allowedPatterns.length == 0;
    }

    /**
     *  Implements the OPTIONS method.
     *
     *  @param req The servlet request
     *  @param res The servlet response
     */

    protected void doOptions( HttpServletRequest req, HttpServletResponse res )
    {
        res.setHeader( "Allow", "GET, PUT, POST, OPTIONS, PROPFIND, PROPPATCH, MOVE, COPY, DELETE");
        res.setStatus( HttpServletResponse.SC_OK );
    }

    /**
     *  Serves a GET with two parameters: 'wikiname' specifying the wikiname
     *  of the attachment, 'version' specifying the version indicator.
     *
     */
    // FIXME: Messages would need to be localized somehow.
    public void doGet( final HttpServletRequest  req, final HttpServletResponse res ) throws IOException {
        final WikiContext context = new WikiContext( m_engine, req, WikiContext.ATTACH );
        final AttachmentManager mgr = m_engine.getAttachmentManager();
        final AuthorizationManager authmgr = m_engine.getAuthorizationManager();

        final String version = req.getParameter( HDR_VERSION );
        final String nextPage = req.getParameter( "nextpage" );
        final String page = context.getPage().getName();
        int ver = WikiProvider.LATEST_VERSION;

        if( page == null ) {
            log.info( "Invalid attachment name." );
            res.sendError( HttpServletResponse.SC_BAD_REQUEST );
            return;
        }

        final OutputStream out = res.getOutputStream();
        try {
            log.debug("Attempting to download att "+page+", version "+version);
            if( version != null ) {
                ver = Integer.parseInt( version );
            }

            final Attachment att = mgr.getAttachmentInfo( page, ver );
            if( att != null ) {
                //
                //  Check if the user has permission for this attachment
                //

                final Permission permission = PermissionFactory.getPagePermission( att, "view" );
                if( !authmgr.checkPermission( context.getWikiSession(), permission ) ) {
                    log.debug("User does not have permission for this");
                    res.sendError( HttpServletResponse.SC_FORBIDDEN );
                    return;
                }

                //
                //  Check if the client already has a version of this attachment.
                //
                if( HttpUtil.checkFor304( req, att.getName(), att.getLastModified() ) ) {
                    log.debug( "Client has latest version already, sending 304..." );
                    res.sendError( HttpServletResponse.SC_NOT_MODIFIED );
                    return;
                }

                final String mimetype = getMimeType( context, att.getFileName() );
                res.setContentType( mimetype );

                //
                //  We use 'inline' instead of 'attachment' so that user agents
                //  can try to automatically open the file.
                //
                res.addHeader( "Content-Disposition", "inline; filename=\"" + att.getFileName() + "\";" );
                res.addDateHeader("Last-Modified",att.getLastModified().getTime());

                if( !att.isCacheable() ) {
                    res.addHeader( "Pragma", "no-cache" );
                    res.addHeader( "Cache-control", "no-cache" );
                }

                // If a size is provided by the provider, report it.
                if( att.getSize() >= 0 ) {
                    // log.info("size:"+att.getSize());
                    res.setContentLength( (int)att.getSize() );
                }

                try( final InputStream  in = mgr.getAttachmentStream( context, att ) ) {
                    int read;
                    final byte[] buffer = new byte[ BUFFER_SIZE ];

                    while( ( read = in.read( buffer ) ) > -1 ) {
                        out.write( buffer, 0, read );
                    }
                }

                if( log.isDebugEnabled() ) {
                    log.debug( "Attachment "+att.getFileName()+" sent to "+req.getRemoteUser()+" on "+HttpUtil.getRemoteAddress(req) );
                }
                if( nextPage != null ) {
                    res.sendRedirect(
                        validateNextPage(
                            TextUtil.urlEncodeUTF8(nextPage),
                            m_engine.getURL( WikiContext.ERROR, "", null )
                        )
                    );
                }

            } else {
                final String msg = "Attachment '" + page + "', version " + ver + " does not exist.";
                log.info( msg );
                res.sendError( HttpServletResponse.SC_NOT_FOUND, msg );
            }
        } catch( final ProviderException pe ) {
            log.debug("Provider failed while reading", pe);
            //
            //  This might fail, if the response is already committed.  So in that
            //  case we just log it.
            //
            sendError( res, "Provider error: "+ pe.getMessage() );
        } catch( final NumberFormatException nfe ) {
            log.warn( "Invalid version number: " + version );
            res.sendError( HttpServletResponse.SC_BAD_REQUEST, "Invalid version number" );
        } catch( final SocketException se ) {
            //
            //  These are very common in download situations due to aggressive
            //  clients.  No need to try and send an error.
            //
            log.debug( "I/O exception during download", se );
        } catch( final IOException ioe ) {
            //
            //  Client dropped the connection or something else happened.
            //  We don't know where the error came from, so we'll at least
            //  try to send an error and catch it quietly if it doesn't quite work.
            //
            log.debug( "I/O exception during download", ioe );
            sendError( res, "Error: " + ioe.getMessage() );
        } finally {
            //
            //  Quite often, aggressive clients close the connection when they have received the last bits.
            //  Therefore, we close the output, but ignore any exception that might come out of it.
            //
            try {
                if( out != null ) {
                    out.close();
                }
            } catch( final IOException ioe ) {
                // ignore
            }
        }
    }

    void sendError( final HttpServletResponse res, final String message ) throws IOException {
        try {
            res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message );
        } catch( final IllegalStateException e ) {
            // ignore
        }
    }

    /**
     *  Returns the mime type for this particular file.  Case does not matter.
     *
     * @param ctx WikiContext; required to access the ServletContext of the request.
     * @param fileName The name to check for.
     * @return A valid mime type, or application/binary, if not recognized
     */
    private static String getMimeType(WikiContext ctx, String fileName )
    {
        String mimetype = null;

        HttpServletRequest req = ctx.getHttpRequest();
        if( req != null )
        {
            ServletContext s = req.getSession().getServletContext();

            if( s != null )
            {
                mimetype = s.getMimeType( fileName.toLowerCase() );
            }
        }

        if( mimetype == null )
        {
            mimetype = "application/binary";
        }

        return mimetype;
    }


    /**
     * Grabs mime/multipart data and stores it into the temporary area.
     * Uses other parameters to determine which name to store as.
     *
     * <p>The input to this servlet is generated by an HTML FORM with
     * two parts. The first, named 'page', is the WikiName identifier
     * for the parent file. The second, named 'content', is the binary
     * content of the file.
     *
     */
    public void doPost( final HttpServletRequest req, final HttpServletResponse res ) throws IOException {
        try {
            final String nextPage = upload( req );
            req.getSession().removeAttribute("msg");
            res.sendRedirect( nextPage );
        } catch( final RedirectException e ) {
            WikiSession session = WikiSession.getWikiSession( m_engine, req );
            session.addMessage( e.getMessage() );

            req.getSession().setAttribute("msg", e.getMessage());
            res.sendRedirect( e.getRedirect() );
        }
    }

    /**
     *  Validates the next page to be on the same server as this webapp.
     *  Fixes [JSPWIKI-46].
     */
    private String validateNextPage( String nextPage, String errorPage )
    {
        if( nextPage.indexOf("://") != -1 )
        {
            // It's an absolute link, so unless it starts with our address, we'll
            // log an error.

            if( !nextPage.startsWith( m_engine.getBaseURL() ) )
            {
                log.warn("Detected phishing attempt by redirecting to an unsecure location: "+nextPage);
                nextPage = errorPage;
            }
        }

        return nextPage;
    }

    /**
     *  Uploads a specific mime multipart input set, intercepts exceptions.
     *
     *  @param req The servlet request
     *  @return The page to which we should go next.
     *  @throws RedirectException If there's an error and a redirection is needed
     *  @throws IOException If upload fails
     * @throws FileUploadException
     */
    protected String upload( final HttpServletRequest req ) throws RedirectException, IOException {
        String msg     = "";
        final String attName = "(unknown)";
        final String errorPage = m_engine.getURL( WikiContext.ERROR, "", null ); // If something bad happened, Upload should be able to take care of most stuff
        String nextPage = errorPage;
        final String progressId = req.getParameter( "progressid" );

        // Check that we have a file upload request
        if( !ServletFileUpload.isMultipartContent(req) ) {
            throw new RedirectException( "Not a file upload", errorPage );
        }

        try {
            final FileItemFactory factory = new DiskFileItemFactory();

            // Create the context _before_ Multipart operations, otherwise strict servlet containers may fail when setting encoding.
            final WikiContext context = new WikiContext( m_engine, req, WikiContext.ATTACH );
            final UploadListener pl = new UploadListener();

            m_engine.getProgressManager().startProgress( pl, progressId );

            final ServletFileUpload upload = new ServletFileUpload( factory );
            upload.setHeaderEncoding( "UTF-8" );
            if( !context.hasAdminPermissions() ) {
                upload.setFileSizeMax( m_maxSize );
            }
            upload.setProgressListener( pl );
            List<FileItem> items = upload.parseRequest( req );

            String   wikipage   = null;
            String   changeNote = null;
            //FileItem actualFile = null;
            List<FileItem> fileItems = new ArrayList<>();

            for( final FileItem item : items ) {
                if( item.isFormField() ) {
                    if( item.getFieldName().equals("page") ) {
                        //
                        // FIXME: Kludge alert.  We must end up with the parent page name, if this is an upload of a new revision
                        //

                        wikipage = item.getString("UTF-8");
                        int x = wikipage.indexOf("/");

                        if( x != -1 ) wikipage = wikipage.substring(0,x);
                    } else if( item.getFieldName().equals("changenote") ) {
                        changeNote = item.getString("UTF-8");
                        if (changeNote != null) {
                            changeNote = TextUtil.replaceEntities(changeNote);
                        }
                    } else if( item.getFieldName().equals( "nextpage" ) ) {
                        nextPage = validateNextPage( item.getString("UTF-8"), errorPage );
                    }
                } else {
                    fileItems.add( item );
                }
            }

            if( fileItems.size() == 0 ) {
                throw new RedirectException( "Broken file upload", errorPage );

            } else {
                for( FileItem actualFile : fileItems ) {
                    String filename = actualFile.getName();
                    long   fileSize = actualFile.getSize();
                    try( InputStream in  = actualFile.getInputStream() ) {
                        executeUpload( context, in, filename, nextPage, wikipage, changeNote, fileSize );
                    }
                }
            }

        } catch( final ProviderException e ) {
            msg = "Upload failed because the provider failed: "+e.getMessage();
            log.warn( msg + " (attachment: " + attName + ")", e );

            throw new IOException( msg );
        } catch( final IOException e ) {
            // Show the submit page again, but with a bit more intimidating output.
            msg = "Upload failure: " + e.getMessage();
            log.warn( msg + " (attachment: " + attName + ")", e );

            throw e;
        } catch( final FileUploadException e ) {
            // Show the submit page again, but with a bit more intimidating output.
            msg = "Upload failure: " + e.getMessage();
            log.warn( msg + " (attachment: " + attName + ")", e );

            throw new IOException( msg, e );
        } finally {
            m_engine.getProgressManager().stopProgress( progressId );
            // FIXME: In case of exceptions should absolutely remove the uploaded file.
        }

        return nextPage;
    }

    /**
     *
     * @param context the wiki context
     * @param data the input stream data
     * @param filename the name of the file to upload
     * @param errorPage the place to which you want to get a redirection
     * @param parentPage the page to which the file should be attached
     * @param changenote The change note
     * @param contentLength The content length
     * @return <code>true</code> if upload results in the creation of a new page;
     * <code>false</code> otherwise
     * @throws RedirectException If the content needs to be redirected
     * @throws IOException       If there is a problem in the upload.
     * @throws ProviderException If there is a problem in the backend.
     */
    protected boolean executeUpload( WikiContext context, InputStream data,
                                     String filename, String errorPage,
                                     String parentPage, String changenote,
                                     long contentLength )
            throws RedirectException,
            IOException, ProviderException
    {
        boolean created = false;

        try {
            filename = AttachmentManager.validateFileName( filename );
        } catch( final WikiException e ) {
            // this is a kludge, the exception that is caught here contains the i18n key
            // here we have the context available, so we can internationalize it properly :
            throw new RedirectException (Preferences.getBundle( context, InternationalizationManager.CORE_BUNDLE )
                    .getString( e.getMessage() ), errorPage );
        }

        //
        //  FIXME: This has the unfortunate side effect that it will receive the
        //  contents.  But we can't figure out the page to redirect to
        //  before we receive the file, due to the stupid constructor of MultipartRequest.
        //

        if( !context.hasAdminPermissions() ) {
            if( contentLength > m_maxSize ) {
                // FIXME: Does not delete the received files.
                throw new RedirectException( "File exceeds maximum size ("+m_maxSize+" bytes)", errorPage );
            }

            if( !isTypeAllowed(filename) ) {
                throw new RedirectException( "Files of this type may not be uploaded to this wiki", errorPage );
            }
        }

        final Principal user    = context.getCurrentUser();
        final AttachmentManager mgr = m_engine.getAttachmentManager();

        log.debug("file="+filename);

        if( data == null ) {
            log.error("File could not be opened.");
            throw new RedirectException("File could not be opened.", errorPage);
        }

        //
        //  Check whether we already have this kind of a page.
        //  If the "page" parameter already defines an attachment
        //  name for an update, then we just use that file.
        //  Otherwise we create a new attachment, and use the
        //  filename given.  Incidentally, this will also mean
        //  that if the user uploads a file with the exact
        //  same name than some other previous attachment,
        //  then that attachment gains a new version.
        //

        Attachment att = mgr.getAttachmentInfo( context.getPage().getName() );

        if( att == null ) {
            att = new Attachment( m_engine, parentPage, filename );
            created = true;
        }
        att.setSize( contentLength );

        //
        //  Check if we're allowed to do this?
        //

        final Permission permission = PermissionFactory.getPagePermission( att, "upload" );
        if( m_engine.getAuthorizationManager().checkPermission( context.getWikiSession(), permission ) ) {
            if( user != null ) {
                att.setAuthor( user.getName() );
            }

            if( changenote != null && changenote.length() > 0 ) {
                att.setAttribute( WikiPage.CHANGENOTE, changenote );
            }

            try {
                m_engine.getAttachmentManager().storeAttachment( att, data );
            } catch( final ProviderException pe ) {
                // this is a kludge, the exception that is caught here contains the i18n key
                // here we have the context available, so we can internationalize it properly :
                throw new ProviderException( Preferences.getBundle( context, InternationalizationManager.CORE_BUNDLE ).getString( pe.getMessage() ) );
            }

            log.info( "User " + user + " uploaded attachment to " + parentPage + " called "+filename+", size " + att.getSize() );
        } else {
            throw new RedirectException( "No permission to upload a file", errorPage );
        }

        return created;
    }

    /**
     *  Provides tracking for upload progress.
     *
     */
    private static class UploadListener extends ProgressItem implements ProgressListener {
        public long m_currentBytes;
        public long m_totalBytes;

        public void update( final long recvdBytes, final long totalBytes, final int item) {
            m_currentBytes = recvdBytes;
            m_totalBytes   = totalBytes;
        }

        public int getProgress() {
            return ( int )( ( ( float )m_currentBytes / m_totalBytes ) * 100 + 0.5 );
        }
    }

}


