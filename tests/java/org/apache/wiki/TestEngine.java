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

package org.apache.wiki;
import java.io.*;
import java.util.*;

import javax.jcr.RepositoryException;
import javax.security.auth.login.LoginException;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.config.BootstrapPropertyResolver;
import net.sourceforge.stripes.controller.AnnotatedClassActionResolver;
import net.sourceforge.stripes.controller.DynamicMappingFilter;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.mock.*;
import net.sourceforge.stripes.util.CryptoUtil;

import org.apache.wiki.action.ViewActionBean;
import org.apache.wiki.action.WikiActionBean;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.content.PageAlreadyExistsException;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.content.inspect.BotTrapInspector;
import org.apache.wiki.content.inspect.Challenge;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.tags.SpamProtectTag;
import org.apache.wiki.ui.stripes.ShortUrlFilter;
import org.apache.wiki.ui.stripes.SpamInterceptor;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.TextUtil;


/**
 *  <p>Simple test engine that always assumes pages are found. The version of TestEngine that is part of JSPWiki 3.0
 *  differs slightly from earlier versions. In particular, it integrates the Stripes framework's mock objects to simulate
 *  servlet testing.</p>
 *  <p>JUnit test classes <em>must</em> call {@link #shutdown()} at the end of tests so that the
 *  Priha JCR backend used for testing is properly shut down.</p>
 *  <p>Because of its use of Stripes mock objects, TestEngine needs to be able to find the various ActionBean
 *  implementations provided in JSPWiki. Therefore, it is <em>extremely</em> sensitive to changes in the build
 *  path. In particular, the mock servlet filter used by TestEngine hard-wires in the relative location
 *  <code>build</code> for finding ActionBeans. This is the directory (relative to the project root) that the
 *  Ant build scripts use for placing generated Java class files. The Eclipse project configuration must configure
 *  itself the same way. To run unit tests in Eclipse, the <code>build</code> directory absolutely <em>must</em>
 *  place generated class files in this directory, rather than the Eclipse default of <code>classes</code>. If
 *  unit tests do not run in Eclipse for some reason, this is the likeliest culprit.</p>
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
    public WikiSession adminSession() throws WikiSecurityException, LoginException
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
    public WikiSession janneSession() throws WikiSecurityException, LoginException
    {
        if ( m_janneWikiSession == null )
        {
            // Set up a test Janne session
            HttpServletRequest request = newHttpRequest();
            m_janneWikiSession = WikiSession.getWikiSession( this, request );
            this.getAuthenticationManager().login( m_janneWikiSession, request,
                    Users.JANNE,
                    Users.JANNE_PASS );
        }
        return m_janneWikiSession;
    }

    public TestEngine( Properties props )
        throws WikiException
    {
        super( configureServletContext( new MockServletContext( "TestEngine" ) ), "test", cleanTestProps( props ) );
        
        // Configure and stash the WikiEngine in the servlet context
        ServletContext servletContext = this.getServletContext();
        servletContext.setAttribute("org.apache.wiki.WikiEngine", this);
    }
    
    /**
     * Creates a correctly-instantiated mock HttpServletRequest with an associated
     * HttpSession. The servlet path will be for ViewActionBean.
     * @return the new request
     */
    public MockHttpServletRequest newHttpRequest()
    {
        UrlBinding binding = ViewActionBean.class.getAnnotation( UrlBinding.class );
        String url = binding == null ? "/Wiki.jsp" : binding.value();
        return newHttpRequest( url );
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
    
    /**
     * For testing purposes: generates and adds sample spam-protect form parameters to a MockRountrip object.
     * This is done in the same way that {@link SpamProtectTag} does it.
     */
    public static void addSpamProtectParams( MockRoundtrip trip )
    {
        // Add the trap + token params
        String paramValue = CryptoUtil.encrypt( "TOKENA" );
        trip.addParameter( BotTrapInspector.REQ_TRAP_PARAM, new String[0] );
        trip.addParameter( "TOKENA", trip.getRequest().getSession().getId() );
        trip.addParameter( BotTrapInspector.REQ_SPAM_PARAM, paramValue );
        paramValue = CryptoUtil.encrypt( Challenge.State.CHALLENGE_NOT_PRESENTED.name() );
        trip.addParameter( SpamInterceptor.CHALLENGE_REQUEST_PARAM, paramValue );

        // Add the UTF-8 token
        trip.addParameter( BotTrapInspector.REQ_ENCODING_CHECK, "\u3041" );
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

                if( f.exists() && f.isDirectory()  )
                {
                    deleteAll( f );
                }
            }
        }
        catch( IOException e ) {} // Fine
    }
    
    /**
     * Empties the page directory and re-initializes the references database.
     * @throws ProviderException
     */
    public void emptyRepository() throws ProviderException
    {
        Collection<WikiPage> pages = getContentManager().getAllPages( null );
        
        for( WikiPage p : pages )
        {
            getContentManager().deletePage( p );
        }
        try
        {
            getReferenceManager().rebuild();
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Could not rebuild reference database.", e );
        }
    }
    
    public static final InputStream findTestProperties()
    {
        return findTestProperties( "/WEB-INF/jspwiki.properties" );
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
        pagename = TextUtil.urlEncode( pagename, "UTF-8" );
        pagename = TextUtil.replaceString( pagename, "/", "%2F" );
        return pagename;
    }

    /**
     *  Deletes all attachments related to the given page.
     */
    public static void deleteAttachments( String page )
    {
        // FIXME: Does not work atm.
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
        throws PageAlreadyExistsException, ProviderException, IOException
    {
        Attachment att = getContentManager().addPage( WikiPath.valueOf( pageName + "/" + attachmentName ), "application/octet-stream" );

        att.setContent( new ByteArrayInputStream(data) );

        att.save();
    }

    /**
     * Convenience method that saves a wiki page by constructing a fake
     * WikiContext and HttpServletRequest. We always want to do this using a
     * WikiContext whose subject contains Role.ADMIN.
     * @param pageName
     * @param content
     * @throws WikiException
     * @throws PageNotFoundException 
     */
    public void saveText( String pageName, String content )
        throws WikiException, PageNotFoundException
    {
        // Build new request and associate our admin session
        MockHttpServletRequest request = newHttpRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( this ).find( request.getSession() );
        try
        {
            this.getAuthenticationManager().login( wikiSession, request,
                    Users.ADMIN,
                    Users.ADMIN_PASS );
        }
        catch( LoginException e )
        {
            throw new WikiException( e.getMessage(), e );
        }

        // Create page and wiki context
        WikiPage page = null;
        
        try
        {
            page = getPage( pageName );
        }
        catch ( PageNotFoundException e )
        { 
            try
            {
                page = createPage( WikiPath.valueOf( pageName ) );
            }
            catch( PageAlreadyExistsException e1 )
            {
                // This should not happen
                throw new WikiException( e1.getMessage(), e1 );
            }
        }
        WikiContext context = this.getWikiContextFactory().newViewContext( request, null, page );
        saveText( context, content );
    }

    public void saveTextAsJanne( String pageName, String content )
        throws WikiException, PageNotFoundException
    {
        // Build new request and associate our Janne session
        MockHttpServletRequest request = newHttpRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( this ).find( request.getSession() );
        try
        {
            this.getAuthenticationManager().login( wikiSession, request,
                    Users.JANNE,
                    Users.JANNE_PASS );
        }
        catch( LoginException e )
        {
            throw new WikiException( e.getMessage(), e );
        }

        // Create page and wiki context
        WikiPage page;
        try
        {
            page = createPage( WikiPath.valueOf( pageName ) );
        }
        catch( PageAlreadyExistsException e1 )
        {
            // This should not happen
            throw new WikiException( e1.getMessage(), e1 );
        }
        WikiContext context = this.getWikiContextFactory().newViewContext( request, null, page );
        saveText( context, content );
    }
    
    /**
     * Shuts down the WikiEngine in an orderly fashion. All of the filters
     * in MockServletContext are destroyed. In particular, StripesFilter's
     * configuration stash is cleaned up.
     */
    public void shutdown()
    {
        MockServletContext servletContext = (MockServletContext)getServletContext();
        for ( Filter filter : servletContext.getFilters() )
        {
            filter.destroy();
        }
        super.shutdown();
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
        props.put( WikiEngine.PROP_URLCONSTRUCTOR, "org.apache.wiki.url.DefaultURLConstructor" );
        return props;
    }

    /**
     * Creates a guest "round trip" object that initializes itself with the TestEngine's mock servlet context,
     * plus a new mock request, mock response and action bean of type {@link org.apache.wiki.action.ViewActionBean}.
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
            getFilterChain();
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
            getFilterChain();
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
    public MockRoundtrip authenticatedTrip( String user, String password, Class<? extends WikiActionBean> beanClass ) throws WikiSecurityException, LoginException
    {
        MockServletContext servletContext = (MockServletContext)getServletContext();
        if ( servletContext.getFilters().size() == 0 )
        {
            getFilterChain();
        }
        MockRoundtrip trip = new MockRoundtrip( servletContext, beanClass );
        WikiSession session = WikiSession.getWikiSession( this, trip.getRequest() );
        this.getAuthenticationManager().login( session, trip.getRequest(), user, password );
        return trip;
    }
    
    /**
     * Initializes a MockFilterChain for a supplied MockServletContext.
     * @param servletContext the mock context
     * @return a configured MockFilterChain appropriate for the servlet context
     */
    public MockFilterChain getFilterChain()
    {
        MockServletContext servletContext = (MockServletContext)getServletContext();
        
        // Create FilterConfig
        MockFilterConfig filterConfig = new MockFilterConfig();
        filterConfig.setFilterName( "TestFilterConfig" );
        filterConfig.setServletContext( servletContext );
        
        // Create and return the FilterChain
        MockFilterChain chain = new MockFilterChain();
        List<Filter> filters = servletContext.getFilters();
        for ( Filter f : filters )
        {
            chain.addFilter( f );
        }
        chain.setServlet( new MockServlet() );
        return chain;
    }
    
    /**
     * Initializes a supplied MockServletContext with the Stripes filters and
     * dummy servlet added.
     * @param servletContext the mock context
     * @return a configured MockFilterChain appropriate for the servlet context
     */
    public static MockServletContext configureServletContext( MockServletContext servletContext )
    {
        // Create the ServletContext and add init params
        Map<String,String> initParams = new HashMap<String,String>();
        initParams.put( AnnotatedClassActionResolver.PACKAGES, "org.apache.wiki.action" );
        servletContext.addAllInitParameters( initParams );
        
        // Configure the ShortURLFilter
        servletContext.addFilter( ShortUrlFilter.class, "ShortURLFilter", null );
        
        // Configure the StripesFilter
        Map<String,String> filterParams = new HashMap<String,String>();
        filterParams.put( BootstrapPropertyResolver.PACKAGES, "org.apache.wiki.ui.stripes" );
        filterParams.put( "ExceptionHandler.Class", "org.apache.wiki.ui.stripes.WikiExceptionHandler" );
        servletContext.addFilter( StripesFilter.class, "StripesFilter", filterParams );
        
        // Configure the DynamicMappingFilter and dummy servlet
        filterParams = new HashMap<String,String>();
        servletContext.addFilter( DynamicMappingFilter.class, "DynamicMappingFilter", filterParams );
        servletContext.setServlet( MockServlet.class, "MockServlet", new HashMap<String,String>() );
        
        return servletContext;
    }

    /**
     * Dummy servlet that returns a {@link java.io.FileNotFoundException}
     * when its {@link javax.servlet.Servlet#service(ServletRequest, ServletResponse)}
     * method is called. This is very useful when used with
     * {@link net.sourceforge.stripes.controller.DynamicMappingFilter}. 
     */
    public static class MockServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        private ServletConfig m_config;
        
        public void destroy() { }

        public ServletConfig getServletConfig()
        {
            return m_config;
        }

        public String getServletInfo()
        {
            return "Mock servlet";
        }

        public void init( ServletConfig config ) throws ServletException
        {
            m_config = config;
        }

        /**
         * Must throw a FileNotFoundException to force DynamicMappingFilter to resolve the ActionBean.
         */
        public void service( ServletRequest request, ServletResponse response ) throws ServletException, IOException
        {
            throw new FileNotFoundException( "File not found: required for DynamicMappingFilter." );
        }
    }
}
