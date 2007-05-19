
package com.ecyrd.jspwiki;
import java.util.Properties;
import java.io.*;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.providers.*;

/**
 *  Simple test engine that always assumes pages are found.
 */
public class TestEngine extends WikiEngine
{
    static Logger log = Logger.getLogger( TestEngine.class );

    private HttpSession m_adminSession;
    private HttpSession m_janneSession;
    private WikiSession m_adminWikiSession;
    private WikiSession m_janneWikiSession;
    private WikiSession m_guestWikiSession;

    /**
     * Creates WikiSession with the privileges of the administrative user.
     * For testing purposes, obviously.
     * @return the wiki session
     */
    public WikiSession adminSession()
    {
        return m_adminWikiSession;
    }

    /**
     * Creates guest WikiSession with the no privileges.
     * For testing purposes, obviously.
     * @return the wiki session
     */
    public WikiSession guestSession()
    {
        return m_guestWikiSession;
    }

    /**
     * Creates WikiSession with the privileges of the Janne.
     * For testing purposes, obviously.
     * @return the wiki session
     */
    public WikiSession janneSession()
    {
        return m_janneWikiSession;
    }

    public TestEngine( Properties props )
        throws WikiException
    {
        super( props );

        // Set up long-running admin session
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.setRemoteAddr( "53.33.128.9" );
        m_adminWikiSession = WikiSession.getWikiSession( this, request );
        this.getAuthenticationManager().login( m_adminWikiSession,
                Users.ADMIN,
                Users.ADMIN_PASS );
        m_adminSession = request.getSession();

        // Set up a test Janne session
        request = new TestHttpServletRequest();
        request.setRemoteAddr( "42.22.17.8" );
        m_janneWikiSession = WikiSession.getWikiSession( this, request );
        this.getAuthenticationManager().login( m_janneWikiSession,
                Users.JANNE,
                Users.JANNE_PASS );
        m_janneSession = request.getSession();

        // Set up guest session
        request = new TestHttpServletRequest();
        request.setRemoteAddr( "42.22.17.8" );
        m_guestWikiSession = WikiSession.getWikiSession( this, request );
    }

    public static void emptyWorkDir()
    {
        Properties properties = new Properties();

        try
        {
            properties.load( findTestProperties() );

            String workdir = properties.getProperty( WikiEngine.PROP_WORKDIR );
            if( workdir != null )
            {
                File f = new File( workdir );

                if( f.exists() && f.isDirectory() && new File( f, "refmgr.ser" ).exists() )
                {
                    deleteAll( f );
                }
            }
        }
        catch( IOException e ) {} // Fine
    }

    public static final InputStream findTestProperties()
    {
        return findTestProperties( "/jspwiki.properties" );
    }

    public static final InputStream findTestProperties( String properties )
    {
        InputStream in = TestEngine.class.getResourceAsStream( properties );

        if( in == null ) throw new InternalWikiException("Unable to locate test property resource: "+properties);

        return in;
    }

    /**
     *  Deletes all files under this directory, and does them recursively.
     */
    public static void deleteAll( File file )
    {
        if( file != null )
        {
            if( file.isDirectory() )
            {
                File[] files = file.listFiles();

                if( files != null )
                {
                    for( int i = 0; i < files.length; i++ )
                    {
                        if( files[i].isDirectory() )
                        {
                            deleteAll(files[i]);
                        }

                        files[i].delete();
                    }
                }
            }

            file.delete();
        }
    }

    /**
     *  Copied from FileSystemProvider
     */
    protected static String mangleName( String pagename )
        throws IOException
    {
        Properties properties = new Properties();
        String m_encoding = properties.getProperty( WikiEngine.PROP_ENCODING,
                                                    AbstractFileProvider.DEFAULT_ENCODING );

        pagename = TextUtil.urlEncode( pagename, m_encoding );
        pagename = TextUtil.replaceString( pagename, "/", "%2F" );
        return pagename;
    }

    /**
     *  Removes a page, but not any auxiliary information.  Works only
     *  with FileSystemProvider.
     */
    public static void deleteTestPage( String name )
    {
        Properties properties = new Properties();

        try
        {
            properties.load( findTestProperties() );
            String files = properties.getProperty( FileSystemProvider.PROP_PAGEDIR );

            File f = new File( files, mangleName(name)+FileSystemProvider.FILE_EXT );

            f.delete();

            // Remove the property file, too
            f = new File( files, mangleName(name)+".properties" );

            if( f.exists() )
                f.delete();
        }
        catch( Exception e )
        {
            log.error("Couldn't delete "+name, e );
        }
    }

    /**
     *  Deletes all attachments related to the given page.
     */
    public void deleteAttachments( String page )
    {
        try
        {
            String files = getWikiProperties().getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );

            File f = new File( files, TextUtil.urlEncodeUTF8( page ) + BasicAttachmentProvider.DIR_EXTENSION );

            deleteAll( f );
        }
        catch( Exception e )
        {
            log.error("Could not remove attachments.",e);
        }
    }

    /**
     *  Makes a temporary file with some content, and returns a handle to it.
     */
    public File makeAttachmentFile()
        throws Exception
    {
        File tmpFile = File.createTempFile("test","txt");
        tmpFile.deleteOnExit();

        FileWriter out = new FileWriter( tmpFile );

        FileUtil.copyContents( new StringReader( "asdfa???dfzbvasdjkfbwfkUg783gqdwog" ), out );

        out.close();

        return tmpFile;
    }

    /**
     *  Adds an attachment to a page for testing purposes.
     * @param pageName
     * @param attachmentName
     * @param data
     */
    public void addAttachment( String pageName, String attachmentName, byte[] data )
        throws ProviderException, IOException
    {
        Attachment att = new Attachment(this,pageName,attachmentName);

        getAttachmentManager().storeAttachment(att, new ByteArrayInputStream(data));
    }

    /**
     * Convenience method that saves a wiki page by constructing a fake
     * WikiContext and HttpServletRequest. We always want to do this using a
     * WikiContext whose subject contains Role.ADMIN.
     * @param pageName
     * @param content
     * @throws WikiException
     */
    public void saveText( String pageName, String content )
        throws WikiException
    {
        // Build new request and associate our admin session
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.m_session = m_adminSession;

        // Create page and wiki context
        WikiPage page = new WikiPage( this, pageName );
        WikiContext context = new WikiContext( this, request, page );
        saveText( context, content );
    }

    public void saveTextAsJanne( String pageName, String content )
        throws WikiException
    {
        // Build new request and associate our Janne session
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.m_session = m_janneSession;

        // Create page and wiki context
        WikiPage page = new WikiPage( this, pageName );
        WikiContext context = new WikiContext( this, request, page );
        saveText( context, content );
    }

    public static void trace()
    {
        try
        {
            throw new Exception("Foo");
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}
