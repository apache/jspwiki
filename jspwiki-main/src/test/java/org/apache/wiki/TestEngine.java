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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.Release;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.providers.AttachmentProvider;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.providers.AbstractFileProvider;
import org.apache.wiki.providers.BasicAttachmentProvider;
import org.apache.wiki.providers.FileSystemProvider;
import org.apache.wiki.render.RenderingManager;
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
import java.util.AbstractMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 *  Simple test engine that always assumes pages are found.
 */
public class TestEngine extends WikiEngine {
    private static final Logger LOG = LogManager.getLogger( TestEngine.class );

    private Session m_adminWikiSession;
    private Session m_janneWikiSession;
    private Session m_guestWikiSession;

    // combined properties file (jspwiki.properties + custom override, if any)
    private static Properties combinedProperties;

    /**
     * Creates WikiSession with the privileges of the administrative user. For testing purposes, obviously.
     *
     * @return the wiki session
     * @throws WikiSecurityException admin login operation had some trouble
     */
    public Session adminSession() throws WikiSecurityException {
        if ( m_adminWikiSession == null ) {
            // Set up long-running admin session
            final HttpServletRequest request = newHttpRequest();
            m_adminWikiSession = WikiSession.getWikiSession( this, request );
            this.getManager( AuthenticationManager.class ).login( m_adminWikiSession, request, Users.ADMIN, Users.ADMIN_PASS );
        }
        return m_adminWikiSession;
    }

    /**
     * Creates guest WikiSession with the no privileges. For testing purposes, obviously.
     *
     * @return the wiki session
     */
    public Session guestSession() {
        if ( m_guestWikiSession == null ) {
            // Set up guest session
            final HttpServletRequest request = newHttpRequest();
            m_guestWikiSession = WikiSession.getWikiSession( this, request );
        }
        return m_guestWikiSession;
    }

    /**
     * Creates WikiSession with the privileges of the Janne. For testing purposes, obviously.
     *
     * @return the wiki session
     * @throws WikiSecurityException janne login operation had some trouble
     */
    public Session janneSession() throws WikiSecurityException {
        if ( m_janneWikiSession == null ) {
            // Set up a test Janne session
            final HttpServletRequest request = newHttpRequest();
            m_janneWikiSession = WikiSession.getWikiSession( this, request );
            this.getManager( AuthenticationManager.class ).login( m_janneWikiSession, request, Users.JANNE, Users.JANNE_PASS );
        }
        return m_janneWikiSession;
    }

    /**
     * Obtains a TestEngine using {@link #getTestProperties()}.
     *
     * @return TestEngine using {@link #getTestProperties()}.
     */
    public static TestEngine build() {
        return build( getTestProperties() );
    }

    /**
     * Obtains a TestEngine using {@link #getTestProperties()} and additional configuration.
     *
     * @param entries additional configuration entries that may overwrite default test properties.
     * @return TestEngine using {@link #getTestProperties()} and additional configuration.
     */
    @SafeVarargs
    public static TestEngine build( final Map.Entry< String, String >... entries ) {
        final Properties properties = getTestProperties();
        for( final Map.Entry< String, String > entry : entries ) {
            properties.setProperty( entry.getKey(), entry.getValue() );
        }
        return build( properties );
    }

    /**
     * Helper method, intended to be imported statically, to ease passing properties to {@link #build(Map.Entry[])}.
     *
     * @param prop property name.
     * @param value property value.
     * @return populated entry ready to be used in {@link #build(Map.Entry[])}.
     */
    public static Map.Entry< String, String > with( final String prop, final String value ) {
        return new AbstractMap.SimpleEntry<>( prop, value );
    }

    /**
     * Obtains a TestEngine using the provided properties.
     *
     * @param props configuration entries.
     * @return TestEngine using the provided properties.
     */
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

    public TestEngine( final Properties props ) throws WikiException {
        super( createServletContext( "test" ), "test" );
        try {
            start( cleanTestProps( props ) );
        } catch( final Exception e ) {
            throw new WikiException( Release.APPNAME + ": Unable to load and setup properties from jspwiki.properties. " + e.getMessage(), e );
        }

        // Stash the WikiEngine in the servlet context
        final ServletContext servletContext = this.getServletContext();
        servletContext.setAttribute( "org.apache.wiki.WikiEngine", this );
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
    public MockHttpServletRequest newHttpRequest( final String path ) {
        final MockHttpServletRequest request = new MockHttpServletRequest( "/JSPWiki", path ) {
            @Override
            public ServletContext getServletContext() { // stripes mock returns null
                return createServletContext( "/JSPWiki" );
            }
        };
        request.setSession( new MockHttpSession( this.getServletContext() ) );
        request.addLocale( new Locale( "" ) );
        return request;
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        super.shutdown();
        TestEngine.emptyWikiDir( getWikiProperties() );
        TestEngine.emptyWorkDir( getWikiProperties() );
    }

    public static void emptyWorkDir() {
        emptyWorkDir( null );
    }

    public static void emptyWorkDir(Properties properties) {
        if( properties == null ) {
            properties = getTestProperties();
        }

        final String workdir = properties.getProperty( WikiEngine.PROP_WORKDIR );
        if( workdir != null ) {
            final File f = new File( workdir );

            if( f.exists() && f.isDirectory() && new File( f, "refmgr.ser" ).exists() ) {
                // System.out.println( "Deleting " + f.getAbsolutePath() );
                deleteAll( f );
            }
        }
    }

    public static void emptyWikiDir() {
        emptyWikiDir( null );
    }

    public static void emptyWikiDir( Properties properties ) {
        if( properties == null ) {
            properties = getTestProperties();
        }
        emptyDir( properties.getProperty( AbstractFileProvider.PROP_PAGEDIR ) );
        emptyDir( properties.getProperty( AttachmentProvider.PROP_STORAGEDIR ) );
    }

    static void emptyDir( final String dir ) {
        if ( dir != null ) {
            final File f = new File( dir );
            if( f.exists() && f.isDirectory() ) {
                deleteAll( f );
            }
        }
    }

    public static Properties getTestProperties() {
        if( combinedProperties == null ) {
            combinedProperties = PropertyReader.getCombinedProperties(PropertyReader.CUSTOM_JSPWIKI_CONFIG);
        }
        // better to make a copy via putAll instead of Properties(properties)
        // constructor, see http://stackoverflow.com/a/2004900
        final Properties propCopy = new Properties();
        propCopy.putAll( combinedProperties );
        return propCopy;
    }

    public static Properties getTestProperties( final String customPropFile ) {
        return PropertyReader.getCombinedProperties( customPropFile );
    }

    /**
     *  Deletes all files under this directory, and does them recursively.
     */
    public static void deleteAll( final File file ) {
        if( file != null ) {
            if( file.isDirectory() ) {
                final File[] files = file.listFiles();
                if( files != null ) {
                    for( final File file2 : files ) {
                        if( file2.isDirectory() ) {
                            deleteAll( file2 );
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
            LOG.error("Couldn't delete "+name, e );
        }
    }

    /**
     *  Deletes all attachments related to the given page.
     */
    public static void deleteAttachments( final String page ) {
        final Properties properties = getTestProperties();

        try {
            final String files = properties.getProperty( AttachmentProvider.PROP_STORAGEDIR );
            final File f = new File( files, TextUtil.urlEncodeUTF8( page ) + BasicAttachmentProvider.DIR_EXTENSION );

            deleteAll( f );
        } catch( final Exception e ) {
            LOG.error("Could not remove attachments.",e);
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
     * Adds an attachment to a page for testing purposes.
     *
     * @param pageName page name
     * @param attachmentName attachment name
     * @param data attachment data
     */
    public void addAttachment( final String pageName, final String attachmentName, final byte[] data ) throws ProviderException, IOException {
        final Attachment att = Wiki.contents().attachment( this, pageName, attachmentName );
        getManager( AttachmentManager.class ).storeAttachment( att, new ByteArrayInputStream( data ) );
    }

    /**
     * Convenience method that saves a wiki page by constructing a fake WikiContext and HttpServletRequest. We always want to do this
     * using a WikiContext whose subject contains Role.ADMIN. Note: the WikiPage author will have the default value of "Guest".
     *
     * @param pageName page name
     * @param content page content
     * @throws WikiException associated login operation or page save had some trouble
     */
    public void saveText( final String pageName, final String content ) throws WikiException {
        // Build new request and associate our admin session
        final MockHttpServletRequest request = newHttpRequest();
        final Session wikiSession = SessionMonitor.getInstance( this ).find( request.getSession() );
        this.getManager( AuthenticationManager.class ).login( wikiSession, request, Users.ADMIN, Users.ADMIN_PASS );

        // Create page and wiki context
        final Page page = Wiki.contents().page( this, pageName );
        final Context context = Wiki.context().create( this, request, page );
        getManager( PageManager.class ).saveText( context, content );
    }

    public void saveTextAsJanne( final String pageName, final String content ) throws WikiException {
        // Build new request and associate our Janne session
        final MockHttpServletRequest request = newHttpRequest();
        final Session wikiSession = SessionMonitor.getInstance( this ).find( request.getSession() );
        this.getManager( AuthenticationManager.class ).login( wikiSession, request, Users.JANNE, Users.JANNE_PASS );

        // Create page and wiki context
        final Page page = Wiki.contents().page( this, pageName );
        page.setAuthor(Users.JANNE);
        final Context context = Wiki.context().create( this, request, page );
        getManager( PageManager.class ).saveText( context, content );
    }

    /**
     * Some pages may produce some i18n text, so we enforce english locale in order to
     * be able to compare properly to assertion texts.
     *
     * @param pagename name of the page.
     * @return (english) contents corresponding to the given page name.
     */
    public String getI18nHTML( final String pagename ) {
        final Page page = getManager( PageManager.class ).getPage( pagename, PageProvider.LATEST_VERSION );
        final Context context = Wiki.context().create( this, newHttpRequest(), page );
        context.setRequestContext( ContextEnum.PAGE_NONE.getRequestContext() );
        return getManager( RenderingManager.class ).getHTML( context, page );
    }

    /**
     * Supplies a clean set of test properties for the TestEngine constructor.
     * @param props the properties supplied by callers
     * @return the corrected/clean properties
     */
    private static Properties cleanTestProps( final Properties props ) {
        final long millis = System.currentTimeMillis();
        if( !"true".equalsIgnoreCase( props.getProperty( "jspwiki.test.disable-clean-props" ) ) ) {
            props.put( AuthenticationManager.PROP_LOGIN_THROTTLING, "false" );
            props.setProperty( "jspwiki.fileSystemProvider.pageDir", cleanNewDirFrom( props.getProperty( "jspwiki.fileSystemProvider.pageDir" ), millis ) );
            props.setProperty( "jspwiki.basicAttachmentProvider.storageDir", cleanNewDirFrom( props.getProperty( "jspwiki.basicAttachmentProvider.storageDir" ), millis ) );
            props.setProperty( "jspwiki.workDir", cleanNewDirFrom( props.getProperty( "jspwiki.workDir" ), millis ) );
        }

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
