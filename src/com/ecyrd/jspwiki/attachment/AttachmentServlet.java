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
package com.ecyrd.jspwiki.attachment;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;

import org.apache.log4j.Category;
//import org.apache.commons.fileupload.*;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.providers.WikiAttachmentProvider;
import com.ecyrd.jspwiki.providers.ProviderException;

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
 */
public class AttachmentServlet
    extends HttpServlet
{
    private WikiEngine m_engine;
    private Category log = Category.getInstance( AttachmentServlet.class ); 


    public static final String HDR_VERSION     = "version";
    public static final String HDR_WIKINAME    = "wikiname";
    public static final String HDR_ACTION      = "action";
    public static final String ATTR_MSG        = "msg";
    public static final String ATTR_ATTACHMENT = "attachment";

    private String m_resultPage;
    private String m_tmpDir;

    /**
     * Initializes the servlet from WikiEngine properties.
     */
    public void init( ServletConfig config )
        throws ServletException 
    {
        super.init( config );

        m_engine         = WikiEngine.getInstance( config );
        Properties props = m_engine.getWikiProperties();
            
        m_resultPage = "/Attachment.jsp";
        m_tmpDir     = System.getProperty( "java.io.tmpdir" );
 
        log.debug( "UploadServlet initialized. Using " + 
                   m_tmpDir + " for temporary storage, directing to " + 
                   m_resultPage + " after upload." );
    }


    /**
     * Serves a GET with two parameters: 'wikiname' specifying the wikiname
     * of the attachment, 'version' specifying the version indicator.
     */

    // FIXME: Messages would need to be localized somehow.
    public void doGet( HttpServletRequest  req, HttpServletResponse res ) 
        throws IOException, ServletException 
    {
        String name    = req.getParameter( HDR_WIKINAME );
        String page    = req.getParameter( "page" );
        String version = req.getParameter( HDR_VERSION );
        String msg     = "An error occurred. Ouch.";
        int    ver     = -1;

        AttachmentManager mgr = m_engine.getAttachmentManager();

        if( name == null || version == null )
        {
            msg = "Invalid attachment name/version.";
        }
        else
        {
            try 
            {
                //  FIXME: Uses provider I/F directly; should go through
                //         manager.

                ver = Integer.parseInt( version );

                Attachment att = mgr.getAttachmentInfo( new WikiPage(page), name, ver );

                if( att != null )
                {
                    res.setContentType( "application/binary" );
                    // Won't work, must be "attachement"
                    res.setHeader( "Content-Disposition", 
                                   "attachment; filename=" + name + ";" );

                    // If a size is provided by the provider, report it. 
                    if( att.getSize() >= 0 )
                        res.setContentLength( (int)att.getSize() );

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

                    msg = "Attachment "+name+" sent to "+req.getRemoteUser()+" on "+req.getRemoteHost();
                    log.debug( msg );

		    return;
                }
                else
                {
                    msg = "Attachment " + name + " version " + ver + 
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

	// Direct back to the Attachment page with informative message.
        ServletContext ctx   = getServletContext();
	RequestDispatcher rd = ctx.getRequestDispatcher( m_resultPage );

        if( msg != null )
        {
            req.setAttribute( ATTR_MSG, msg );
        }
    }




    /**
     * Grabs mime/multipart data and stores it into the temporary area.
     * Uses other parameters to determine which name to store as.
     *
     * <p>The input to this servlet is generated by an HTML FORM with
     * two parts. The first, named 'wikiname', is the WikiName identifier
     * for the attachment. The second, named 'content', is the binary
     * content of the file.
     *
     * <p>After handling, the request is forwarded to m_resultPage.
     */
    public void doPost( HttpServletRequest  req, HttpServletResponse res ) 
        throws IOException, ServletException 
    {
        ServletContext ctx = getServletContext();
        String msg = "";

        String action = req.getParameter( HDR_ACTION );

        // Only the one action at this time, but preparing for more.
        upload( req );

        RequestDispatcher rd = ctx.getRequestDispatcher( m_resultPage );
        log.debug( "Forwarding to " + m_resultPage );
        rd.forward( req, res );
    }




    /**
     * Uploads a specific mime multipart input set, intercepts exceptions.
     */
    protected String upload( HttpServletRequest req )
    {
        String msg     = "";
        String attName = "(unknown)";

        try
        {
            MultipartRequest multi = new ServletMultipartRequest( req, m_tmpDir, Integer.MAX_VALUE );

            //
            // We expect to get parameters 'user', 'wikiname'
            //
            // FIXME: Should be read from standard UserProfile.
            String user     = multi.getURLParameter( "user" );
            String wikipage = multi.getURLParameter( "page" );

            Enumeration files = multi.getFileParameterNames();

            while( files.hasMoreElements() )
            {
                String part = (String) files.nextElement();
                File   f    = multi.getFile( part );
                InputStream in;

                if( f != null )
                {
                    in = new FileInputStream( f );
                }
                else
                {
                    in = multi.getFileContents( part );
                }

                //
                //  Is a file to be uploaded.
                //

                String filename = multi.getFileSystemName( part );

                Attachment att = new Attachment( wikipage );
                att.setAuthor( user );
                att.setFileName( filename );

                m_engine.getAttachmentManager().storeAttachment( att, in );

                log.info( "User " + user + " uploaded attachment to " + wikipage + 
                          " called "+filename+", size " + multi.getFileSize(part) );
                
                f.delete();
            }

            // Inform the JSP page of which file we are handling:
            // req.setAttribute( ATTR_ATTACHMENT, wikiname );
        }
        catch( ProviderException e )
        {
            msg = "Upload failed because the provider failed: "+e.getMessage();
            log.warn( msg + " (attachment: " + attName + ")", e );
        }
        catch( IOException e )
        {
            // Show the submit page again, but with a bit more 
            // intimidating output.
            msg = "Upload failure: " + e.getMessage();
            log.warn( msg + " (attachment: " + attName + ")", e );
        }

        return( msg );
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


