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

package com.ecyrd.jspwiki;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;

import org.apache.jspwiki.api.WikiException;
import org.apache.jspwiki.api.WikiPage;

import com.ecyrd.jspwiki.action.WikiActionBean;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.SessionMonitor;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.content.WikiName;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;
import com.ecyrd.jspwiki.providers.AbstractFileProvider;
import com.ecyrd.jspwiki.providers.BasicAttachmentProvider;
import com.ecyrd.jspwiki.providers.FileSystemProvider;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.ui.WikiServletFilter;
import com.ecyrd.jspwiki.util.FileUtil;
import com.ecyrd.jspwiki.util.TextUtil;

/**
 *  <p>Simple test engine that always assumes pages are found. The version of TestEngine that is part of JSPWiki 3.0
 *  differs slightly from earlier versions. In particular, it integrates the Stripes framework's mock objects to simulate
 *  servlet testing.</p>
 *  <p>Because of its use of Stripes mock objects, TestEngine needs to be able to find the various ActionBean
 *  implementations provided in JSPWiki. Therefore, it is <em>extremely</em> sensitive to changes in the build
 *  path. In particular, the mock servlet filter used by TestEngine hard-wires in the relative location
 *  <code>build</code> for finding ActionBeans. This is the directory (relative to the project root) that the
 *  Ant build scripts use for placing generated Java class files. The Eclipse project configuration must configure
 *  itself the same way. To run unit tests in Eclipse, the <code>build</code> directory absolutely <em>must</em>
 *  place generated class files in this directory, rather than the Eclipse default of <code>classes</code>. If
 *  unit tests do not run in Eclipse for some reason, this is the likeliest culprit.
 */
public class TestEngine extends WikiEngine
{
    static Logger log = LoggerFactory.getLogger( TestEngine.class );

    private WikiSession m_adminWikiSession = null;
    private WikiSession m_janneWikiSession = null;
    private WikiSession m_guestWikiSession = null;

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
            this.getAuthenticationManager().login( m_adminWikiSession,
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
            this.getAuthenticationManager().login( m_janneWikiSession,
                    Users.JANNE,
                    Users.JANNE_PASS );
        }
        return m_janneWikiSession;
    }

    public TestEngine( Properties props )
        throws WikiException
    {
        super( new MockServletContext( "test" ), "test", cleanTestProps( props ) );
        
        // Stash the WikiEngine in the servlet context
        ServletContext servletContext = this.getServletContext();
        servletContext.setAttribute("com.ecyrd.jspwiki.WikiEngine", this);
    }
    
    /**
     * Creates a correctly-instantiated mock HttpServletRequest with an associated
     * HttpSession.
     * @return the new request
     */
    public MockHttpServletRequest newHttpRequest()
    {
        return newHttpRequest( "/Wiki.jsp" );
    }

    /**
     * Creates a correctly-instantiated mock HttpServletRequest with an associated
     * HttpSession and path.
     * @param path the path relative to the wiki context, for example "/Wiki.jsp"
     * @return the new request
     */
    public MockHttpServletRequest newHttpRequest( String path )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( "/JSPWiki", path );
        request.setSession( new MockHttpSession( this.getServletContext() ) );
        return request;
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
            
            deleteAttachments( name );
        }
        catch( Exception e )
        {
            log.error("Couldn't delete "+name, e );
        }
    }

    /**
     *  Deletes all attachments related to the given page.
     */
    public static void deleteAttachments( String page )
    {
        Properties properties = new Properties();

        try
        {
            properties.load( findTestProperties() );
            String files = properties.getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );

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
     * @throws WikiException 
     */
    public void addAttachment( String pageName, String attachmentName, byte[] data )
        throws IOException, WikiException
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
        MockHttpServletRequest request = newHttpRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( this ).find( request.getSession() );
        this.getAuthenticationManager().login( wikiSession,
                Users.ADMIN,
                Users.ADMIN_PASS );

        // Create page and wiki context
        WikiPage page = createPage( WikiName.valueOf( pageName ) );
        WikiContext context = this.getWikiContextFactory().newViewContext( request, null, page );
        saveText( context, content );
    }

    public void saveTextAsJanne( String pageName, String content )
        throws WikiException
    {
        // Build new request and associate our Janne session
        MockHttpServletRequest request = newHttpRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( this ).find( request.getSession() );
        this.getAuthenticationManager().login( wikiSession,
                Users.JANNE,
                Users.JANNE_PASS );

        // Create page and wiki context
        WikiPage page = createPage( WikiName.valueOf( pageName ) );
        WikiContext context = this.getWikiContextFactory().newViewContext( request, null, page );
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
    
    /**
     * Supplies a clean set of test properties for the TestEngine constructor.
     * @param props the properties supplied by callers
     * @return the corrected/clean properties
     */
    private static Properties cleanTestProps( Properties props )
    {
        props.put( AuthenticationManager.PROP_LOGIN_THROTTLING, "false" );
        props.put( WikiEngine.PROP_URLCONSTRUCTOR, "com.ecyrd.jspwiki.url.DefaultURLConstructor" );
        return props;
    }

    /**
     * Creates a guest "round trip" object that initializes itself with the TestEngine's mock servlet context,
     * plus a new mock request, mock response and action bean of type {@link com.ecyrd.jspwiki.action.ViewActionBean}.
     * This method is the preferred way to instantiate request and response objects, which can be
     * obtained by calling {@link net.sourceforge.stripes.mock.MockRoundtrip#getRequest()} and
     * {@link net.sourceforge.stripes.mock.MockRoundtrip#getResponse()}.
     * @param beanClass the Stripes action bean to start with
     * @return the mock rountrip
     */
    public MockRoundtrip guestTrip( Class<? extends WikiActionBean> beanClass )
    {
        MockServletContext servletContext = (MockServletContext)getServletContext();
        if ( servletContext.getFilters().size() == 0 )
        {
            initStripesServletContext();
        }
        return new MockRoundtrip( servletContext, beanClass );
    }
    
    /**
     * Creates a guest "round trip" object that initializes itself with the TestEngine's mock servlet context,
     * plus a new mock request, mock response and URL.
     * This method is the preferred way to instantiate request and response objects, which can be
     * obtained by calling {@link net.sourceforge.stripes.mock.MockRoundtrip#getRequest()} and
     * {@link net.sourceforge.stripes.mock.MockRoundtrip#getResponse()}.
     * @param url the URL to start with
     * @return the mock rountrip
     */
    public MockRoundtrip guestTrip( String url )
    {
        MockServletContext servletContext = (MockServletContext)getServletContext();
        if ( servletContext.getFilters().size() == 0 )
        {
            initStripesServletContext();
        }
        return new MockRoundtrip( servletContext, url );
    }
    

    /**
     * Creates a "round trip" object initialized with a supplied set of credentials. The WikiSession
     * associated with the created MockRoundtrip object will have privileges appropriate for
     * the credentials supplied.
     * @param user the login name
     * @param password the password
     * @param beanClass the Stripes action bean to start with
     * @return the initialized round trip
     * @throws WikiSecurityException
     */
    public MockRoundtrip authenticatedTrip( String user, String password, Class<? extends WikiActionBean> beanClass ) throws WikiSecurityException
    {
        MockServletContext servletContext = (MockServletContext)getServletContext();
        if ( servletContext.getFilters().size() == 0 )
        {
            initStripesServletContext();
        }
        MockRoundtrip trip = new MockRoundtrip( servletContext, beanClass );
        WikiSession session = WikiSession.getWikiSession( this, trip.getRequest() );
        this.getAuthenticationManager().login( session, user, password );
        return trip;
    }
    
    /**
     * Initializes the TestEngine's MockServletContext, with the Stripes filters and servlets added
     */
    private void initStripesServletContext()
    {
        // Configure the filter and servlet
        MockServletContext servletContext = (MockServletContext)getServletContext();
        servletContext.addFilter( WikiServletFilter.class, "WikiServletFilter", new HashMap<String,String>() );
        servletContext.setServlet(DispatcherServlet.class, "StripesDispatcher", null);
        
        // Add extension classes
        Map<String,String> filterParams = new HashMap<String,String>();
        filterParams.put("ActionResolver.Packages", "com.ecyrd.jspwiki.action");
        filterParams.put("Extension.Packages", "com.ecyrd.jspwiki.ui.stripes");
        
        // Add the exception handler class
        filterParams.put( "ExceptionHandler.Class", "com.ecyrd.jspwiki.ui.stripes.WikiExceptionHandler" );
        
        // Return the configured servlet context
        servletContext.addFilter(StripesFilter.class, "StripesFilter", filterParams);
    }
    
}
