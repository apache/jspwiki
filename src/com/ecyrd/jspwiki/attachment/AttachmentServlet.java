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
package com.ecyrd.jspwiki.attachment;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.ParseException;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.UserProfile;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.filters.RedirectException;

// multipartrequest.jar imports:
import http.utils.multipartrequest.*;


/**
 * This is a simple file upload servlet customized for JSPWiki. It receives 
 * a mime/multipart POST message, as sent by an Attachment page, stores it
 * temporarily, figures out what WikiName to use to store it, checks for
 * previously existing versions.
 *
 * <p>This servlet does not worry about authentication; we leave that to the 
 * container, or a previous servlet that chains to us.
 *
 * @author Erik Bunn
 * @author Janne Jalkanen
 *
 * @since 1.9.45.
 */
public class AttachmentServlet
    extends HttpServlet
{
    private WikiEngine m_engine;
    Logger log = Logger.getLogger(this.getClass().getName());

    public static final String HDR_VERSION     = "version";
    public static final String HDR_NAME        = "page";

    /** Default expiry period is 1 day */
    protected static final long DEFAULT_EXPIRY = 1 * 24 * 60 * 60 * 1000; 

    private String m_tmpDir;

    /**
     *  The maximum size that an attachment can be.
     */
    private int   m_maxSize = Integer.MAX_VALUE;

    //
    // Not static as DateFormat objects are not thread safe.
    // Used to handle the RFC date format = Sat, 13 Apr 2002 13:23:01 GMT
    //
    private final DateFormat rfcDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    /**
     * Initializes the servlet from WikiEngine properties.
     */
    public void init( ServletConfig config )
        throws ServletException 
    {
        super.init( config );

        m_engine         = WikiEngine.getInstance( config );
        Properties props = m_engine.getWikiProperties();

        m_tmpDir         = m_engine.getWorkDir()+File.separator+"attach-tmp";
 
        m_maxSize        = TextUtil.getIntegerProperty( props, 
                                                        AttachmentManager.PROP_MAXSIZE,
                                                        Integer.MAX_VALUE );

        File f = new File( m_tmpDir );
        if( !f.exists() )
        {
            f.mkdirs();
        }
        else if( !f.isDirectory() )
        {
            log.fatal("A file already exists where the temporary dir is supposed to be: "+m_tmpDir+".  Please remove it.");
        }

        log.debug( "UploadServlet initialized. Using " + 
                   m_tmpDir + " for temporary storage." );
    }

    /**
     *  If returns true, then should return a 304 (HTTP_NOT_MODIFIED)
     */
    private boolean checkFor304( HttpServletRequest req,
                                 Attachment att )
    {

        Date lastModified = att.getLastModified();

        //
        //  We'll do some handling for CONDITIONAL GET (and return a 304)
        //  If the client has set the following headers, do not try for a 304.
        //
        //    pragma: no-cache
        //    cache-control: no-cache
        //

        if( "no-cache".equalsIgnoreCase(req.getHeader("Pragma"))
            || "no-cache".equalsIgnoreCase(req.getHeader("cache-control"))) 
        {
            log.debug("Client specifically wants a fresh copy of this attachment :" + 
                     att.getFileName());
        } 
        else 
        {
            long ifModifiedSince = req.getDateHeader("If-Modified-Since");

            //log.info("ifModifiedSince:"+ifModifiedSince);
            if( ifModifiedSince != -1 )
            {
                long lastModifiedTime = lastModified.getTime();

                //log.info("lastModifiedTime:" + lastModifiedTime);
                if( lastModifiedTime <= ifModifiedSince )
                {
                    return true;
                }
            } 
            else
            {
                try 
                {
                    String s = req.getHeader("If-Modified-Since");

                    if( s != null ) 
                    {
                        Date ifModifiedSinceDate = rfcDateFormat.parse(s);
                        //log.info("ifModifiedSinceDate:" + ifModifiedSinceDate);
                        if( lastModified.before(ifModifiedSinceDate) ) 
                        {
                            return true;
                        }
                    }
                } 
                catch (ParseException e) 
                {
                    log.warn(e.getLocalizedMessage(), e);
                }
            }
        }
         
        return false;
    }

    /**
     * Serves a GET with two parameters: 'wikiname' specifying the wikiname
     * of the attachment, 'version' specifying the version indicator.
     */

    // FIXME: Messages would need to be localized somehow.
    public void doGet( HttpServletRequest  req, HttpServletResponse res ) 
        throws IOException, ServletException 
    {
        String page     = m_engine.safeGetParameter( req, "page" );
        String version  = m_engine.safeGetParameter( req, HDR_VERSION );
        String nextPage = m_engine.safeGetParameter( req, "nextpage" );

        String msg      = "An error occurred. Ouch.";
        int    ver      = WikiProvider.LATEST_VERSION;

        AttachmentManager mgr = m_engine.getAttachmentManager();
        AuthorizationManager authmgr = m_engine.getAuthorizationManager();

        UserProfile wup = m_engine.getUserManager().getUserProfile( req );

        if( page == null )
        {
            msg = "Invalid attachment name.";
        }
        else
        {
            try 
            {
                // System.out.println("Attempting to download att "+page+", version "+version);
                if( version != null )
                {
                    ver = Integer.parseInt( version );
                }

                Attachment att = mgr.getAttachmentInfo( page, ver );

                if( att != null )
                {
                    //
                    //  Check if the user has permission for this attachment
                    //

                    if( !authmgr.checkPermission( att, wup, "view" ) )
                    {
                        log.debug("User does not have permission for this");
                        res.sendError( HttpServletResponse.SC_FORBIDDEN );
                        return;
                    }
                                                 

                    //
                    //  Check if the client already has a version of this attachment.
                    //
                    if( checkFor304( req, att ) )
                    {
                        log.debug("Client has latest version already, sending 304...");
                        res.sendError( HttpServletResponse.SC_NOT_MODIFIED );
                        return;
                    }

                    String mimetype = getServletConfig().getServletContext().getMimeType( att.getFileName().toLowerCase() );

                    if( mimetype == null )
                    {
                        mimetype = "application/binary";
                    }

                    res.setContentType( mimetype );

                    //
                    //  We use 'inline' instead of 'attachment' so that user agents
                    //  can try to automatically open the file.
                    //

                    res.addHeader( "Content-Disposition",
                                   "inline; filename=\"" + att.getFileName() + "\";" );
                    long expires = new Date().getTime() + DEFAULT_EXPIRY;
                    res.addDateHeader("Expires",expires);
                    res.addDateHeader("Last-Modified",att.getLastModified().getTime());

                    // If a size is provided by the provider, report it.
                    if( att.getSize() >= 0 )
                    {
                        // log.info("size:"+att.getSize());
                        res.setContentLength( (int)att.getSize() );
                    }

                    OutputStream out = res.getOutputStream();
                    InputStream  in  = mgr.getAttachmentStream( att );

                    int read = 0;
                    byte buffer[] = new byte[8192];
                    
                    while( (read = in.read( buffer )) > -1 )
                    {
                        out.write( buffer, 0, read );
                    }
                    
                    in.close();
                    out.close();

                    if(log.isDebugEnabled())
                    {
                        msg = "Attachment "+att.getFileName()+" sent to "+req.getRemoteUser()+" on "+req.getRemoteHost();
                        log.debug( msg );
                    }
                    if( nextPage != null ) res.sendRedirect( nextPage );

                    return;
                }
                else
                {
                    msg = "Attachment '" + page + "', version " + ver + 
                          " does not exist.";
                }
                
            }
            catch( ProviderException pe )
            {
                msg = "Provider error: "+pe.getMessage();
            }
            catch( NumberFormatException nfe )
            {
                msg = "Invalid version number (" + version + ")";
            }
            catch( IOException ioe )
            {
                msg = "Error: " + ioe.getMessage();
            }
        }
        log.info( msg );

        if( nextPage != null ) res.sendRedirect( nextPage );
    }




    /**
     * Grabs mime/multipart data and stores it into the temporary area.
     * Uses other parameters to determine which name to store as.
     *
     * <p>The input to this servlet is generated by an HTML FORM with
     * two parts. The first, named 'page', is the WikiName identifier
     * for the parent file. The second, named 'content', is the binary
     * content of the file.
     */
    public void doPost( HttpServletRequest  req, HttpServletResponse res ) 
        throws IOException, ServletException 
    {
        try
        {
            String nextPage = upload( req );
            req.getSession().removeAttribute("msg");
            res.sendRedirect( nextPage );
        }
        catch( RedirectException e )
        {
            req.getSession().setAttribute("msg", e.getMessage());
            res.sendRedirect( e.getRedirect() );
        }
    }


    /**
     *  Uploads a specific mime multipart input set, intercepts exceptions.
     *
     *  @return The page to which we should go next.
     */

    protected String upload( HttpServletRequest req )
        throws RedirectException,
               IOException
    {
        String msg     = "";
        String attName = "(unknown)";
        String errorPage = m_engine.getBaseURL()+"Error.jsp"; // If something bad happened, Upload should be able to take care of most stuff
        String nextPage = errorPage;

        try
        {
            MultipartRequest multi;

            multi = new MultipartRequest( null, // no debugging
                                          req.getContentType(), 
                                          req.getContentLength(), 
                                          req.getInputStream(), 
                                          m_tmpDir, 
                                          Integer.MAX_VALUE,
                                          m_engine.getContentEncoding() );

            nextPage        = multi.getURLParameter( "nextpage" );
            String wikipage = multi.getURLParameter( "page" );

            WikiContext context = m_engine.createContext( req, WikiContext.UPLOAD );
            errorPage = m_engine.getBaseURL()+
                        "Upload.jsp?page="+
                        m_engine.encodeName( wikipage );

            /**
             *  FIXME: This has the unfortunate side effect that it will receive the
             *  contents.  But we can't figure out the page to redirect to
             *  before we receive the file, due to the stupid constructor of MultipartRequest.
             */
            if( req.getContentLength() > m_maxSize )
            {
                // FIXME: Does not delete the received files.
                throw new RedirectException( "File exceeds maximum size ("+m_maxSize+" bytes)",
                                             errorPage );
            }

            UserProfile user    = context.getCurrentUser();

            //
            //  Go through all files being uploaded.
            //
            Enumeration files = multi.getFileParameterNames();

            while( files.hasMoreElements() )
            {
                String part = (String) files.nextElement();
                File   f    = multi.getFile( part );
                AttachmentManager mgr = m_engine.getAttachmentManager();
                InputStream in;

                try
                {
                    //
                    //  Is a file to be uploaded.
                    //

                    String filename = multi.getFileSystemName( part );

                    if( filename == null || filename.trim().length() == 0 )
                    {
                        log.error("Empty file name given.");

                        throw new RedirectException("Empty file name given.",
                                                    errorPage);
                    }

                    //
                    //  Should help with IE 5.22 on OSX
                    //
                    filename = filename.trim();
                
                    //
                    //  Attempt to open the input stream
                    //
                    if( f != null )
                    {
                        in = new FileInputStream( f );
                    }
                    else
                    {
                        //
                        //  This happens onl when the size of the 
                        //  file is small enough to be cached in memory
                        //
                        in = multi.getFileContents( part );
                    }

                    if( in == null )
                    {
                        log.error("File could not be opened.");

                        throw new RedirectException("File could not be opened.",
                                                    errorPage);
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

                    Attachment att = mgr.getAttachmentInfo( wikipage );

                    if( att == null )
                    {
                        att = new Attachment( wikipage, filename );
                    }

                    //
                    //  Check if we're allowed to do this?
                    //

                    if( m_engine.getAuthorizationManager().checkPermission( att,
                                                                            user,
                                                                            "upload" ) )
                    {
                        if( user != null )
                        {
                            att.setAuthor( user.getName() );
                        }
                
                        m_engine.getAttachmentManager().storeAttachment( att, in );

                        log.info( "User " + user + " uploaded attachment to " + wikipage + 
                                  " called "+filename+", size " + multi.getFileSize(part) );
                    }
                    else
                    {
                        throw new RedirectException("No permission to upload a file",
                                                    errorPage);
                    }
                }
                finally
                {
                    if( f != null )
                        f.delete();
                }
            }

            // Inform the JSP page of which file we are handling:
            // req.setAttribute( ATTR_ATTACHMENT, wikiname );
        }
        catch( ProviderException e )
        {
            msg = "Upload failed because the provider failed: "+e.getMessage();
            log.warn( msg + " (attachment: " + attName + ")", e );

            throw new IOException(msg);
        }
        catch( IOException e )
        {
            // Show the submit page again, but with a bit more 
            // intimidating output.
            msg = "Upload failure: " + e.getMessage();
            log.warn( msg + " (attachment: " + attName + ")", e );

            throw e;
        }

        return nextPage;
    }


    /**
     * Produces debug output listing parameters and files.
     */
    /*
    private void debugContentList( MultipartRequest  multi )
    {
        StringBuffer sb = new StringBuffer();
        
        sb.append( "Upload information: parameters: [" );

        Enumeration params = multi.getParameterNames();
        while( params.hasMoreElements() ) 
        {
            String name = (String)params.nextElement();
            String value = multi.getURLParameter( name );
            sb.append( "[" + name + " = " + value + "]" );
        }
              
        sb.append( " files: [" );
        Enumeration files = multi.getFileParameterNames();
        while( files.hasMoreElements() ) 
        {
            String name = (String)files.nextElement();
            String filename = multi.getFileSystemName( name );
            String type = multi.getContentType( name );
            File f = multi.getFile( name );
            sb.append( "[name: " + name );
            sb.append( " temp_file: " + filename );
            sb.append( " type: " + type );
            if (f != null) 
            {
                sb.append( " abs: " + f.getPath() );
                sb.append( " size: " + f.length() );
            }
            sb.append( "]" );
        }
        sb.append( "]" );


        log.debug( sb.toString() );
    }
    */

}


