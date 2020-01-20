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

package org.apache.wiki;

import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockServletContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.providers.AbstractFileProvider;
import org.apache.wiki.providers.BasicAttachmentProvider;
import org.apache.wiki.providers.FileSystemProvider;
import org.apache.wiki.providers.WikiPageProvider;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.PropertyReader;
import org.apache.wiki.util.TextUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Properties;

/**
 *  Simple test engine that always assumes pages are found.
 */
public class TestEngine extends WikiEngine
{
    static Logger log = Logger.getLogger( TestEngine.class );

    private WikiSession m_adminWikiSession = null;
    private WikiSession m_janneWikiSession = null;
    private WikiSession m_guestWikiSession = null;

    // combined properties file (jspwiki.properties + custom override, if any)
    private static Properties combinedProperties = null;

    /**
     * Creates WikiSession with the privileges of the administrative user.
     * For testing purposes, obviously.
     * @return the wiki session
     * @throws WikiSecurityException
     */
    public WikiSession adminSession() throws WikiSecurityException
    {
        if ( m_adminWikiSession == null )
        {
            // Set up long-running admin session
            HttpServletRequest request = newHttpRequest();
            m_adminWikiSession = WikiSession.getWikiSession( this, request );
            this.getAuthenticationManager().login( m_adminWikiSession, request,
                                                   Users.ADMIN,
                                                   Users.ADMIN_PASS );
        }
        return m_adminWikiSession;
    }

    /**
     * Creates guest WikiSession with the no privileges.
     * For testing purposes, obviously.
     * @return the wiki session
     */
    public WikiSession guestSession()
    {
        if ( m_guestWikiSession == null )
        {
            // Set up guest session
            HttpServletRequest request = newHttpRequest();
            m_guestWikiSession = WikiSession.getWikiSession( this, request );
        }
        return m_guestWikiSession;
    }

    /**
     * Creates WikiSession with the privileges of the Janne.
     * For testing purposes, obviously.
     * @return the wiki session
     * @throws WikiSecurityException
     */
    public WikiSession janneSession() throws WikiSecurityException
    {
        if ( m_janneWikiSession == null )
        {
            // Set up a test Janne session
            HttpServletRequest request = newHttpRequest();
            m_janneWikiSession = WikiSession.getWikiSession( this, request );
            this.getAuthenticationManager().login( m_janneWikiSession, request, Users.JANNE, Users.JANNE_PASS );
        }
        return m_janneWikiSession;
    }

    public static TestEngine build() {
        return build( getTestProperties() );
    }

    public static TestEngine build( final Properties props ) {
        try {
            return new TestEngine( props );
        } catch( final WikiException we ) {
            throw new UnsupportedOperationException( "Unable to build TestEngine: " + we.getMessage(), we );
        }
    }

    public TestEngine() throws WikiException {
        this( getTestProperties() );
    }

    public TestEngine( Properties props ) throws WikiException {
        super( createServletContext( "test" ), "test", cleanTestProps( props ) );

        // Stash the WikiEngine in the servlet context
        ServletContext servletContext = this.getServletContext();
        servletContext.setAttribute("org.apache.wiki.WikiEngine", this);
    }

    public static MockServletContext createServletContext( final String contextName ) {
        return new MockServletContext( contextName ) {

            @Override
            public int getMajorVersion() {
                return 3;
            }

            @Override
            public int getMinorVersion() {
                return 1;
            }
        };
    }

    /**
     * Creates a correctly-instantiated mock HttpServletRequest with an associated
     * HttpSession.
     * @return the new request
     */
    public MockHttpServletRequest newHttpRequest() {
        return newHttpRequest( "/Wiki.jsp" );
    }

    /**
     * Creates a correctly-instantiated mock HttpServletRequest with an associated
     * HttpSession and path.
     * @param path the path relative to the wiki context, for example "/Wiki.jsp"
     * @return the new request
     */
    public MockHttpServletRequest newHttpRequest( String path ) {
        MockHttpServletRequest request = new MockHttpServletRequest( "/JSPWiki", path ) {
            @Override
            public ServletContext getServletContext() { // stripes mock returns null
                return new MockServletContext( "/JSPWiki" ) {
                    @Override
                    public String getRealPath( final String path ) { // stripes mock returns null
                        return path ;
                    }
                };
            }
        };
        request.setSession( new MockHttpSession( this.getServletContext() ) );
        request.addLocale( new Locale( "" ) );
        return request;
    }

    public static void emptyWorkDir() {
        emptyWorkDir( null );
    }

    public static void emptyWorkDir(Properties properties) {
        if (properties == null) {
            properties = getTestProperties();
        }

        String workdir = properties.getProperty( WikiEngine.PROP_WORKDIR );
        if ( workdir != null ) {
            File f = new File( workdir );

            if (f.exists() && f.isDirectory() && new File( f, "refmgr.ser" ).exists()) {
                // System.out.println( "Deleting " + f.getAbsolutePath() );
                deleteAll( f );
            }
        }
    }

    public static void emptyWikiDir() {
        emptyWikiDir( null );
    }

    public static void emptyWikiDir(Properties properties) {
        if (properties == null) {
            properties = getTestProperties();
        }

        String wikidir = properties.getProperty( AbstractFileProvider.PROP_PAGEDIR );
        if ( wikidir != null ) {
            File f = new File( wikidir );

            if (f.exists() && f.isDirectory()) {
                deleteAll( f );
            }
        }
    }

    public static final Properties getTestProperties() {
        if (combinedProperties == null) {
            combinedProperties = PropertyReader.getCombinedProperties(PropertyReader.CUSTOM_JSPWIKI_CONFIG);
        }
        // better to make a copy via putAll instead of Properties(properties)
        // constructor, see http://stackoverflow.com/a/2004900
        Properties propCopy = new Properties();
        propCopy.putAll(combinedProperties);
        return propCopy;
    }

    public static final Properties getTestProperties(String customPropFile) {
        return PropertyReader.getCombinedProperties(customPropFile);
    }
/*
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
*/
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
                    for (File file2 : files) {
                        if( file2.isDirectory() )
                        {
                            deleteAll(file2);
                        }

                        file2.delete();
                    }
                }
            }

            file.delete();
        }
    }

    /**
     *  Copied from FileSystemProvider
     */
    protected static String mangleName( String pagename ) {
        final Properties properties = new Properties();
        final String m_encoding = properties.getProperty( WikiEngine.PROP_ENCODING, AbstractFileProvider.DEFAULT_ENCODING );

        pagename = TextUtil.urlEncode( pagename, m_encoding );
        pagename = TextUtil.replaceString( pagename, "/", "%2F" );
        return pagename;
    }

    /**
     *  Removes a page, but not any auxiliary information.  Works only
     *  with FileSystemProvider.
     */
    public void deleteTestPage( final String name ) {
        final Properties properties = getTestProperties();
        try {
            final String files = properties.getProperty( FileSystemProvider.PROP_PAGEDIR );

            File f = new File( files, mangleName(name)+FileSystemProvider.FILE_EXT );

            f.delete();

            // Remove the property file, too
            f = new File( files, mangleName(name)+".properties" );

            if( f.exists() ) {
				f.delete();
			}

            deleteAttachments( name );
            firePageEvent( WikiPageEvent.PAGE_DELETED, name );
        } catch( final Exception e ) {
            log.error("Couldn't delete "+name, e );
        }
    }

    /**
     *  Deletes all attachments related to the given page.
     */
    public static void deleteAttachments( final String page ) {
        final Properties properties = getTestProperties();

        try {
            final String files = properties.getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );
            final File f = new File( files, TextUtil.urlEncodeUTF8( page ) + BasicAttachmentProvider.DIR_EXTENSION );

            deleteAll( f );
        } catch( final Exception e ) {
            log.error("Could not remove attachments.",e);
        }
    }

    /**
     *  Makes a temporary file with some content, and returns a handle to it.
     */
    public File makeAttachmentFile() throws Exception {
        final File tmpFile = File.createTempFile("test","txt");
        tmpFile.deleteOnExit();

        try( final FileWriter out = new FileWriter( tmpFile ) ) {
            FileUtil.copyContents( new StringReader( "asdfa???dfzbvasdjkfbwfkUg783gqdwog" ), out );
        }

        return tmpFile;
    }

    /**
     *  Adds an attachment to a page for testing purposes.
     * @param pageName
     * @param attachmentName
     * @param data
     */
    public void addAttachment( final String pageName, final String attachmentName, final byte[] data ) throws ProviderException, IOException {
        final Attachment att = new Attachment( this,pageName,attachmentName );
        getAttachmentManager().storeAttachment( att, new ByteArrayInputStream( data ) );
    }

    /**
     * Convenience method that saves a wiki page by constructing a fake
     * WikiContext and HttpServletRequest. We always want to do this using a
     * WikiContext whose subject contains Role.ADMIN.
     * Note: the WikiPage author will have the default value of "Guest".
     * @param pageName
     * @param content
     * @throws WikiException
     */
    public void saveText( final String pageName, final String content ) throws WikiException {
        // Build new request and associate our admin session
        final MockHttpServletRequest request = newHttpRequest();
        final WikiSession wikiSession = SessionMonitor.getInstance( this ).find( request.getSession() );
        this.getAuthenticationManager().login( wikiSession, request, Users.ADMIN, Users.ADMIN_PASS );

        // Create page and wiki context
        final WikiPage page = new WikiPage( this, pageName );
        final WikiContext context = new WikiContext( this, request, page );
        getPageManager().saveText( context, content );
    }

    public void saveTextAsJanne( final String pageName, final String content ) throws WikiException {
        // Build new request and associate our Janne session
        final MockHttpServletRequest request = newHttpRequest();
        final WikiSession wikiSession = SessionMonitor.getInstance( this ).find( request.getSession() );
        this.getAuthenticationManager().login( wikiSession, request, Users.JANNE, Users.JANNE_PASS );

        // Create page and wiki context
        final WikiPage page = new WikiPage( this, pageName );
        page.setAuthor(Users.JANNE);
        final WikiContext context = new WikiContext( this, request, page );
        getPageManager().saveText( context, content );
    }

    /**
     * Some pages may produce some i18n text, so we enforce english locale in order to
     * be able to compare properly to assertion texts.
     *
     * @param pagename name of the page.
     * @return (english) contents corresponding to the given page name.
     */
    public String getI18nHTML( final String pagename ) {
        final WikiPage page = getPageManager().getPage( pagename, WikiPageProvider.LATEST_VERSION );
        final WikiContext context = new WikiContext( this, newHttpRequest(), page );
        context.setRequestContext( WikiContext.NONE );
        return getRenderingManager().getHTML( context, page );
    }

    public static void trace() {
        try {
            throw new Exception("Foo");
        } catch( final Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Supplies a clean set of test properties for the TestEngine constructor.
     * @param props the properties supplied by callers
     * @return the corrected/clean properties
     */
    private static Properties cleanTestProps( final Properties props ) {
        final long millis = System.currentTimeMillis();
        props.put( AuthenticationManager.PROP_LOGIN_THROTTLING, "false" );
        props.setProperty( "jspwiki.fileSystemProvider.pageDir", cleanNewDirFrom( props.getProperty( "jspwiki.fileSystemProvider.pageDir" ), millis ) );
        props.setProperty( "jspwiki.basicAttachmentProvider.storageDir", cleanNewDirFrom( props.getProperty( "jspwiki.basicAttachmentProvider.storageDir" ), millis ) );
        props.setProperty( "jspwiki.workDir", cleanNewDirFrom( props.getProperty( "jspwiki.workDir" ), millis ) );
        return props;
    }

    private static String cleanNewDirFrom( final String pageDir, final long millis ) {
        final String testEngineCreationOrigin = getTestEngineCreationOrigin();
        if( StringUtils.isBlank( pageDir ) ) {
            return "target" + File.separator + millis + "-" + testEngineCreationOrigin;
        }
        // take into account executions on Windows boxes can have both / and \
        final int lastDirPosition = Math.max( pageDir.lastIndexOf( '/' ), pageDir.lastIndexOf( File.separator ) );
        if( lastDirPosition == -1 ) {
            return "target" + File.separator + millis + "-" + testEngineCreationOrigin + "-" + pageDir;
        }
        final String stripNumbers = pageDir.substring( lastDirPosition );
        return pageDir.substring( 0, lastDirPosition + 1 )
             + millis
             + "-" + testEngineCreationOrigin
             + stripNumbers.replaceAll( "\\d", StringUtils.EMPTY ); // place all related tests' folders one next to the others
    }

    private static String getTestEngineCreationOrigin() {
        for( final StackTraceElement trace : Thread.currentThread().getStackTrace() ) {
            if( !( trace.getClassName().contains( TestEngine.class.getSimpleName() ) ||
                   trace.getClassName().contains( Thread.class.getSimpleName() ) ) ) {
                return trace.getClassName() + "-" + trace.getMethodName().replace( "<", "" ) // <init> -> init
                                                                         .replace( ">", "" );
            }
        }
        return "Unable to locate TestEngine creation";
    }

}
