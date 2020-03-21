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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.wiki.api.Release;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.acl.AclManager;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.content.PageRenamer;
import org.apache.wiki.diff.DifferenceManager;
import org.apache.wiki.event.WikiEngineEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.filters.FilterManager;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.references.ReferenceManager;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.rss.RSSGenerator;
import org.apache.wiki.search.SearchManager;
import org.apache.wiki.tasks.TasksManager;
import org.apache.wiki.ui.CommandResolver;
import org.apache.wiki.ui.EditorManager;
import org.apache.wiki.ui.TemplateManager;
import org.apache.wiki.ui.admin.AdminBeanManager;
import org.apache.wiki.ui.progress.ProgressManager;
import org.apache.wiki.url.URLConstructor;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.PropertyReader;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.variables.VariableManager;
import org.apache.wiki.workflow.WorkflowManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 *  Main implementation for {@link Engine}.
 *
 *  <P>
 *  Using this class:  Always get yourself an instance from JSP page by using the {@code WikiEngine.getInstance(..)} method.  Never create
 *  a new WikiEngine() from scratch, unless you're writing tests.
 *
 *  <p>
 *  {@inheritDoc}
 */
public class WikiEngine implements Engine {

    private static final String ATTR_WIKIENGINE = "org.apache.wiki.WikiEngine";
    private static final Logger log = Logger.getLogger( WikiEngine.class );

    /** True, if log4j has been configured. */
    // FIXME: If you run multiple applications, the first application to run defines where the log goes.  Not what we want.
    private static boolean c_configured = false;

    /** Stores properties. */
    private Properties m_properties;

    /** Does the work in renaming pages. */
    private PageRenamer m_pageRenamer = null;

    /** Should the user info be saved with the page data as well? */
    private boolean m_saveUserInfo = true;

    /** If true, uses UTF8 encoding for all data */
    private boolean m_useUTF8 = true;

    /** Store the file path to the basic URL.  When we're not running as a servlet, it defaults to the user's current directory. */
    private String m_rootPath = System.getProperty( "user.dir" );

    /** Stores references between wikipages. */
    private ReferenceManager  m_referenceManager = null;

    /** Stores the Plugin manager */
    private PluginManager     m_pluginManager;

    /** Stores the Variable manager */
    private VariableManager   m_variableManager;

    /** Stores the Attachment manager */
    private AttachmentManager m_attachmentManager = null;

    /** Stores the Page manager */
    private PageManager       m_pageManager = null;

    /** Stores the authorization manager */
    private AuthorizationManager m_authorizationManager = null;

    /** Stores the authentication manager.*/
    private AuthenticationManager m_authenticationManager = null;

    /** Stores the ACL manager. */
    private AclManager       m_aclManager = null;

    /** Resolves wiki actions, JSPs and special pages. */
    private CommandResolver  m_commandResolver = null;

    private TemplateManager  m_templateManager = null;

    /** Does all our diffs for us. */
    private DifferenceManager m_differenceManager;

    /** Handlers page filters. */
    private FilterManager    m_filterManager;

    /** Stores the Search manager */
    private SearchManager    m_searchManager = null;

    /** Facade for managing users */
    private UserManager      m_userManager = null;

    /** Facade for managing users */
    private GroupManager     m_groupManager = null;

    private RenderingManager m_renderingManager;

    private EditorManager    m_editorManager;

    private InternationalizationManager m_internationalizationManager;

    private ProgressManager  m_progressManager;

    private TasksManager m_tasksManager;

    /** Constructs URLs */
    private URLConstructor   m_urlConstructor;

    /** Generates RSS feed when requested. */
    private RSSGenerator     m_rssGenerator;

    /** The RSS file to generate. */
    private String           m_rssFile;

    /** Store the ServletContext that we're in.  This may be null if WikiEngine is not running inside a servlet container (i.e. when testing). */
    private ServletContext   m_servletContext = null;

    /** Stores the template path.  This is relative to "templates". */
    private String           m_templateDir;

    /** The default front page name.  Defaults to "Main". */
    private String           m_frontPage;

    /** The time when this engine was started. */
    private Date             m_startTime;

    /** The location where the work directory is. */
    private String           m_workDir;

    /** Each engine has their own application id. */
    private String           m_appid = "";

    private boolean          m_isConfigured = false; // Flag.

    /** Each engine has its own workflow manager. */
    private WorkflowManager m_workflowMgr = null;

    private AdminBeanManager m_adminBeanManager;

    /** Stores wikiengine attributes. */
    private Map< String, Object > m_attributes = new ConcurrentHashMap<>();

    /** Stores WikiEngine's associated managers. */
    protected Map< Class< ? >, Object > managers = new ConcurrentHashMap<>();

    /**
     *  Gets a WikiEngine related to this servlet.  Since this method is only called from JSP pages (and JspInit()) to be specific,
     *  we throw a RuntimeException if things don't work.
     *
     *  @param config The ServletConfig object for this servlet.
     *
     *  @return A WikiEngine instance.
     *  @throws InternalWikiException in case something fails. This is a RuntimeException, so be prepared for it.
     */
    // FIXME: It seems that this does not work too well, jspInit() does not react to RuntimeExceptions, or something...
    public static synchronized WikiEngine getInstance( final ServletConfig config ) throws InternalWikiException {
        return getInstance( config.getServletContext(), null );
    }

    /**
     *  Gets a WikiEngine related to the servlet. Works like getInstance(ServletConfig), but does not force the Properties object.
     *  This method is just an optional way of initializing a WikiEngine for embedded JSPWiki applications; normally, you
     *  should use getInstance(ServletConfig).
     *
     *  @param config The ServletConfig of the webapp servlet/JSP calling this method.
     *  @param props  A set of properties, or null, if we are to load JSPWiki's default jspwiki.properties (this is the usual case).
     *
     *  @return One well-behaving WikiEngine instance.
     */
    public static synchronized WikiEngine getInstance( final ServletConfig config, final Properties props ) {
        return getInstance( config.getServletContext(), props );
    }

    /**
     *  Gets a WikiEngine related to the servlet. Works just like getInstance( ServletConfig )
     *
     *  @param context The ServletContext of the webapp servlet/JSP calling this method.
     *  @param props  A set of properties, or null, if we are to load JSPWiki's default
     *                jspwiki.properties (this is the usual case).
     *
     *  @return One fully functional, properly behaving WikiEngine.
     *  @throws InternalWikiException If the WikiEngine instantiation fails.
     */

    // FIXME: Potential make-things-easier thingy here: no need to fetch the wikiengine anymore
    //        Wiki.jsp.jspInit() [really old code]; it's probably even faster to fetch it
    //        using this method every time than go to pageContext.getAttribute().
    public static synchronized WikiEngine getInstance( final ServletContext context, Properties props ) throws InternalWikiException {
        WikiEngine engine = ( WikiEngine )context.getAttribute( ATTR_WIKIENGINE );

        if( engine == null ) {
            final String appid = Integer.toString(context.hashCode()); //FIXME: Kludge, use real type.
            context.log(" Assigning new engine to "+appid);
            try {
                if( props == null ) {
                    props = PropertyReader.loadWebAppProps( context );
                }

                engine = new WikiEngine( context, appid, props );
                context.setAttribute( ATTR_WIKIENGINE, engine );
            } catch( final Exception e ) {
                context.log( "ERROR: Failed to create a Wiki engine: "+e.getMessage() );
                log.error( "ERROR: Failed to create a Wiki engine, stacktrace follows " , e);
                throw new InternalWikiException( "No wiki engine, check logs." , e);
            }
        }
        return engine;
    }

    /**
     *  Instantiate the WikiEngine using a given set of properties. Use this constructor for testing purposes only.
     *
     *  @param properties A set of properties to use to initialize this WikiEngine.
     *  @throws WikiException If the initialization fails.
     */
    public WikiEngine( final Properties properties ) throws WikiException {
        initialize( properties );
    }

    /**
     *  Instantiate using this method when you're running as a servlet and WikiEngine will figure out where to look for the property file.
     *  Do not use this method - use WikiEngine.getInstance() instead.
     *
     *  @param context A ServletContext.
     *  @param appid   An Application ID.  This application is an unique random string which is used to recognize this WikiEngine.
     *  @param props   The WikiEngine configuration.
     *  @throws WikiException If the WikiEngine construction fails.
     */
    protected WikiEngine( final ServletContext context, final String appid, final Properties props ) throws WikiException {
        m_servletContext = context;
        m_appid          = appid;

        // Stash the WikiEngine in the servlet context
        if ( context != null ) {
            context.setAttribute( ATTR_WIKIENGINE,  this );
            m_rootPath = context.getRealPath("/");
        }

        try {
            //  Note: May be null, if JSPWiki has been deployed in a WAR file.
            initialize( props );
            log.info( "Root path for this Wiki is: '" + m_rootPath + "'" );
        } catch( final Exception e ) {
            final String msg = Release.APPNAME+": Unable to load and setup properties from jspwiki.properties. "+e.getMessage();
            if ( context != null ) {
                context.log( msg );
            }
            throw new WikiException( msg, e );
        }
    }

    /**
     *  Does all the real initialization.
     */
    private void initialize( final Properties props ) throws WikiException {
        m_startTime  = new Date();
        m_properties = props;

        //
        //  Initialize log4j.  However, make sure that we don't initialize it multiple times. By default we load the log4j config
        //  statements from jspwiki.properties, unless the property jspwiki.use.external.logconfig=true, in that case we let log4j
        //  figure out the logging configuration.
        //
        if( !c_configured ) {
            final String useExternalLogConfig = TextUtil.getStringProperty( props,"jspwiki.use.external.logconfig","false" );
            if( useExternalLogConfig == null || useExternalLogConfig.equals( "false" ) ) {
                PropertyConfigurator.configure( props );
            }
            c_configured = true;
        }

        log.info( "*******************************************" );
        log.info( Release.APPNAME + " " + Release.getVersionString() + " starting. Whee!" );

        fireEvent( WikiEngineEvent.INITIALIZING ); // begin initialization

        log.debug( "Java version: " + System.getProperty( "java.runtime.version" ) );
        log.debug( "Java vendor: " + System.getProperty( "java.vm.vendor" ) );
        log.debug( "OS: " + System.getProperty( "os.name" ) + " " + System.getProperty( "os.version" ) + " " + System.getProperty( "os.arch" ) );
        log.debug( "Default server locale: " + Locale.getDefault() );
        log.debug( "Default server timezone: " + TimeZone.getDefault().getDisplayName( true, TimeZone.LONG ) );

        if( m_servletContext != null ) {
            log.info( "Servlet container: " + m_servletContext.getServerInfo() );
            if( m_servletContext.getMajorVersion() < 3 || ( m_servletContext.getMajorVersion() == 3 && m_servletContext.getMinorVersion() < 1 ) ) {
                throw new InternalWikiException( "JSPWiki requires a container which supports at least version 3.1 of Servlet specification" );
            }
        }

        log.debug( "Configuring WikiEngine..." );

        //  Create and find the default working directory.
        m_workDir = TextUtil.getStringProperty( props, PROP_WORKDIR, null );

        if( m_workDir == null ) {
            m_workDir = System.getProperty("java.io.tmpdir", ".");
            m_workDir += File.separator+Release.APPNAME+"-"+m_appid;
        }

        try {
            final File f = new File( m_workDir );
            f.mkdirs();

            //
            //  A bunch of sanity checks
            //
            if( !f.exists() ) {
                throw new WikiException("Work directory does not exist: "+m_workDir);
            }
            if( !f.canRead() ) {
                throw new WikiException("No permission to read work directory: "+m_workDir);
            }
            if( !f.canWrite() ) {
                throw new WikiException("No permission to write to work directory: "+m_workDir);
            }
            if( !f.isDirectory() ) {
                throw new WikiException("jspwiki.workDir does not point to a directory: "+m_workDir);
            }
        } catch( final SecurityException e ) {
            log.fatal( "Unable to find or create the working directory: "+m_workDir, e );
            throw new IllegalArgumentException( "Unable to find or create the working dir: " + m_workDir, e );
        }

        log.info( "JSPWiki working directory is '" + m_workDir + "'" );

        m_saveUserInfo   = TextUtil.getBooleanProperty( props, PROP_STOREUSERNAME, m_saveUserInfo );
        m_useUTF8        = StandardCharsets.UTF_8.name().equals( TextUtil.getStringProperty( props, PROP_ENCODING, StandardCharsets.ISO_8859_1.name() ) );
        m_templateDir    = TextUtil.getStringProperty( props, PROP_TEMPLATEDIR, "default" );
        enforceValidTemplateDirectory();
        m_frontPage      = TextUtil.getStringProperty( props, PROP_FRONTPAGE,   "Main" );

        //
        //  Initialize the important modules.  Any exception thrown by the managers means that we will not start up.
        //
        // FIXME: This part of the code is getting unwieldy.  We must think of a better way to do the startup-sequence.
        try {
            final String aclClassName = m_properties.getProperty( PROP_ACL_MANAGER_IMPL, ClassUtil.getMappedClass( AclManager.class.getName() ).getName() );
            final String urlConstructorClassnName = TextUtil.getStringProperty( props, PROP_URLCONSTRUCTOR, "DefaultURLConstructor" );
            final Class< ? > urlclass = ClassUtil.findClass( "org.apache.wiki.url", urlConstructorClassnName );

            m_commandResolver = ClassUtil.getMappedObject( CommandResolver.class.getName(), this, props );
            managers.put( CommandResolver.class, m_commandResolver );

            m_urlConstructor = ( URLConstructor ) urlclass.getDeclaredConstructor().newInstance();
            m_urlConstructor.initialize( this, props );
            managers.put( URLConstructor.class, m_urlConstructor );

            m_pageManager           = ClassUtil.getMappedObject( PageManager.class.getName(), this, props );
            managers.put( PageManager.class, m_pageManager );

            m_pluginManager         = ClassUtil.getMappedObject( PluginManager.class.getName(), this, props );
            managers.put( PluginManager.class, m_pluginManager );

            m_differenceManager     = ClassUtil.getMappedObject( DifferenceManager.class.getName(), this, props );
            managers.put( DifferenceManager.class, m_differenceManager );

            m_attachmentManager     = ClassUtil.getMappedObject( AttachmentManager.class.getName(), this, props );
            managers.put( AttachmentManager.class, m_attachmentManager );

            m_variableManager       = ClassUtil.getMappedObject( VariableManager.class.getName(), props );
            managers.put( VariableManager.class, m_variableManager );

            m_searchManager         = ClassUtil.getMappedObject( SearchManager.class.getName(), this, props );
            managers.put( SearchManager.class, m_searchManager );

            m_authenticationManager = ClassUtil.getMappedObject( AuthenticationManager.class.getName() );
            m_authenticationManager.initialize( this, props );
            managers.put( AuthenticationManager.class, m_authenticationManager );

            m_authorizationManager  = ClassUtil.getMappedObject( AuthorizationManager.class.getName() );
            m_authorizationManager.initialize( this, props );
            managers.put( AuthorizationManager.class, m_authorizationManager );

            m_userManager           = ClassUtil.getMappedObject( UserManager.class.getName() );
            m_userManager.initialize( this, props );
            managers.put( UserManager.class, m_userManager );

            m_groupManager          = ClassUtil.getMappedObject( GroupManager.class.getName() );
            m_groupManager.initialize( this, props );
            managers.put( GroupManager.class, m_groupManager );

            m_editorManager         = ClassUtil.getMappedObject( EditorManager.class.getName(), this );
            m_editorManager.initialize( props );
            managers.put( EditorManager.class, m_editorManager );

            m_progressManager       = ClassUtil.getMappedObject( ProgressManager.class.getName(), this );
            managers.put( ProgressManager.class, m_progressManager );

            m_aclManager = ClassUtil.getMappedObject( aclClassName ); // TODO: I am not sure whether this is the right call
            m_aclManager.initialize( this, m_properties );
            managers.put( AclManager.class, m_aclManager );

            // Start the Workflow manager
            m_workflowMgr = ClassUtil.getMappedObject(WorkflowManager.class.getName());
            m_workflowMgr.initialize(this, props);
            managers.put( WorkflowManager.class, m_workflowMgr );

            m_tasksManager = ClassUtil.getMappedObject(TasksManager.class.getName());
            managers.put( TasksManager.class, m_tasksManager );

            m_internationalizationManager = ClassUtil.getMappedObject(InternationalizationManager.class.getName(),this);
            managers.put( InternationalizationManager.class, m_internationalizationManager );

            m_templateManager = ClassUtil.getMappedObject(TemplateManager.class.getName(), this, props );
            managers.put( TemplateManager.class, m_templateManager );

            // Since we want to use a page filters initilize() method as a engine startup listener where we can initialize global event
            // listeners, it must be called lastly, so that all object references in the engine are availabe to the initialize() method
            m_filterManager = ClassUtil.getMappedObject(FilterManager.class.getName(), this, props );
            managers.put( FilterManager.class, m_filterManager );

            m_adminBeanManager = ClassUtil.getMappedObject(AdminBeanManager.class.getName(),this);
            managers.put( AdminBeanManager.class, m_adminBeanManager );

            m_pageRenamer = ClassUtil.getMappedObject( PageRenamer.class.getName(), this, props );
            managers.put( PageRenamer.class, m_pageRenamer );

            // RenderingManager depends on FilterManager events.
            m_renderingManager = ClassUtil.getMappedObject( RenderingManager.class.getName() );
            m_renderingManager.initialize( this, props );
            managers.put( RenderingManager.class, m_renderingManager );

            //  ReferenceManager has the side effect of loading all pages.  Therefore after this point, all page attributes are available.
            //  initReferenceManager is indirectly using m_filterManager, therefore it has to be called after it was initialized.
            initReferenceManager();

            //  Hook the different manager routines into the system.
            m_filterManager.addPageFilter( m_referenceManager, -1001 );
            m_filterManager.addPageFilter( m_searchManager, -1002 );
        } catch( final RuntimeException e ) {
            // RuntimeExceptions may occur here, even if they shouldn't.
            log.fatal( "Failed to start managers.", e );
            throw new WikiException( "Failed to start managers: " + e.getMessage(), e );
        } catch( final ClassNotFoundException e ) {
            log.fatal( "JSPWiki could not start, URLConstructor was not found: " + e.getMessage(), e );
            throw new WikiException( e.getMessage(), e );
        } catch( final InstantiationException e ) {
            log.fatal( "JSPWiki could not start, URLConstructor could not be instantiated: " + e.getMessage(), e );
            throw new WikiException( e.getMessage(), e );
        } catch( final IllegalAccessException e ) {
            log.fatal( "JSPWiki could not start, URLConstructor cannot be accessed: " + e.getMessage(), e );
            throw new WikiException( e.getMessage(), e );
        } catch( final Exception e ) {
            // Final catch-all for everything
            log.fatal( "JSPWiki could not start, due to an unknown exception when starting.",e );
            throw new WikiException( "Failed to start. Caused by: " + e.getMessage() + "; please check log files for better information.", e );
        }

        //  Initialize the good-to-have-but-not-fatal modules.
        try {
            if( TextUtil.getBooleanProperty( props, RSSGenerator.PROP_GENERATE_RSS,false ) ) {
                m_rssGenerator = ClassUtil.getMappedObject( RSSGenerator.class.getName(), this, props );
                m_rssGenerator.initialize( this, props );
                managers.put( RSSGenerator.class, m_rssGenerator );
            }
        } catch( final Exception e ) {
            log.error( "Unable to start RSS generator - JSPWiki will still work, but there will be no RSS feed.", e );
        }

        fireEvent( WikiEngineEvent.INITIALIZED ); // initialization complete

        log.info("WikiEngine configured.");
        m_isConfigured = true;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings( "unchecked" )
    public < T > T getManager( final Class< T > manager ) {
        return ( T )managers.get( manager );
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings( "unchecked" )
    public < T > List< T > getManagers( final Class< T > manager ) {
        return ( List< T > )managers.entrySet().stream()
                                    .filter( e -> manager.isAssignableFrom( e.getKey() ) )
                                    .map( Map.Entry::getValue )
                                    .collect( Collectors.toList() );
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConfigured() {
        return m_isConfigured;
    }

    /**
     * Checks if the template directory specified in the wiki's properties actually exists. If it doesn't, then {@code m_templateDir} is
     * set to {@link #DEFAULT_TEMPLATE_NAME}.
     * <p>
     * This checks the existence of the <tt>ViewTemplate.jsp</tt> file, which exists in every template using {@code m_servletContext.getRealPath("/")}.
     * <p>
     * {@code m_servletContext.getRealPath("/")} can return {@code null} on certain servers/conditions (f.ex, packed wars), an extra check
     * against {@code m_servletContext.getResource} is made.
     */
    void enforceValidTemplateDirectory() {
        if( m_servletContext != null ) {
            final String viewTemplate = "templates" + File.separator + getTemplateDir() + File.separator + "ViewTemplate.jsp";
            boolean exists = new File( m_servletContext.getRealPath("/") + viewTemplate ).exists();
            if( !exists ) {
                try {
                    final URL url = m_servletContext.getResource( viewTemplate );
                    exists = url != null && StringUtils.isNotEmpty( url.getFile() );
                } catch( final MalformedURLException e ) {
                    log.warn( "template not found with viewTemplate " + viewTemplate );
                }
            }
            if( !exists ) {
                log.warn( getTemplateDir() + " template not found, updating WikiEngine's default template to " + DEFAULT_TEMPLATE_NAME );
                m_templateDir = DEFAULT_TEMPLATE_NAME;
            }
        }
    }

    /**
     *  Initializes the reference manager. Scans all existing WikiPages for
     *  internal links and adds them to the ReferenceManager object.
     *
     *  @throws WikiException If the reference manager initialization fails.
     */
    public void initReferenceManager() throws WikiException {
        try {
            final ArrayList< Page > pages = new ArrayList<>();
            pages.addAll( m_pageManager.getAllPages() );
            pages.addAll( m_attachmentManager.getAllAttachments() );

            // Build a new manager with default key lists.
            if( m_referenceManager == null ) {
                m_referenceManager = ClassUtil.getMappedObject(ReferenceManager.class.getName(), this );
                m_referenceManager.initialize( pages );
                managers.put( ReferenceManager.class, m_referenceManager );
            }

        } catch( final ProviderException e ) {
            log.fatal("PageProvider is unable to list pages: ", e);
        } catch( final ReflectiveOperationException | IllegalArgumentException e ) {
            throw new WikiException( "Could not instantiate ReferenceManager: " + e.getMessage(), e );
        }
    }

    /** {@inheritDoc} */
    @Override
    public Properties getWikiProperties() {
        return m_properties;
    }

    /** {@inheritDoc} */
    @Override
    public String getWorkDir() {
        return m_workDir;
    }

    /** {@inheritDoc} */
    @Override
    public String getTemplateDir() {
        return m_templateDir;
    }

    /**
     *  Returns the current TemplateManager.
     *
     *  @return A TemplateManager instance.
     */
    public TemplateManager getTemplateManager() {
        return getManager( TemplateManager.class );
    }

    /** {@inheritDoc} */
    @Override
    public Date getStartTime() {
        return ( Date )m_startTime.clone();
    }

    /** {@inheritDoc} */
    @Override
    public String getBaseURL() {
        return m_servletContext.getContextPath();
    }

    /** {@inheritDoc} */
    @Override
    public String getGlobalRSSURL() {
        if( m_rssGenerator != null && m_rssGenerator.isEnabled() ) {
            return getBaseURL() + "/" + m_rssGenerator.getRssFile();
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getInterWikiURL( final String wikiName ) {
        return TextUtil.getStringProperty( m_properties,PROP_INTERWIKIREF + wikiName,null );
    }

    /** {@inheritDoc} */
    @Override
    public String getURL( final String context, String pageName, final String params ) {
        if( pageName == null ) {
            pageName = getFrontPage();
        }
        return m_urlConstructor.makeURL( context, pageName, params );
    }

    /** {@inheritDoc} */
    @Override
    public String getFrontPage() {
        return m_frontPage;
    }

    /** {@inheritDoc} */
    @Override
    public ServletContext getServletContext() {
        return m_servletContext;
    }

    /** {@inheritDoc} */
    @Override
    public Collection< String > getAllInterWikiLinks() {
        final ArrayList< String > list = new ArrayList<>();
        for( final Enumeration< ? > i = m_properties.propertyNames(); i.hasMoreElements(); ) {
            final String prop = ( String )i.nextElement();
            if( prop.startsWith( PROP_INTERWIKIREF ) ) {
                list.add( prop.substring( prop.lastIndexOf( "." ) + 1 ) );
            }
        }

        return list;
    }

    /** {@inheritDoc} */
    @Override
    public Collection< String > getAllInlinedImagePatterns() {
        final ArrayList< String > ptrnlist = new ArrayList<>();
        for( final Enumeration< ? > e = m_properties.propertyNames(); e.hasMoreElements(); ) {
            final String name = ( String )e.nextElement();
            if( name.startsWith( PROP_INLINEIMAGEPTRN ) ) {
                ptrnlist.add( TextUtil.getStringProperty( m_properties, name, null ) );
            }
        }

        if( ptrnlist.size() == 0 ) {
            ptrnlist.add( DEFAULT_INLINEPATTERN );
        }

        return ptrnlist;
    }

    /** {@inheritDoc} */
    @Override
    public String getSpecialPageReference( final String original ) {
        return m_commandResolver.getSpecialPageReference( original );
    }

    /** {@inheritDoc} */
    @Override
    // FIXME: Should use servlet context as a default instead of a constant.
    public String getApplicationName() {
        final String appName = TextUtil.getStringProperty( m_properties, PROP_APPNAME, Release.APPNAME );
        return TextUtil.cleanString( appName, TextUtil.PUNCTUATION_CHARS_ALLOWED );
    }

    /** {@inheritDoc} */
    @Override
    public String getFinalPageName( final String page ) throws ProviderException {
        return m_commandResolver.getFinalPageName( page );
    }

    /** {@inheritDoc} */
    @Override
    public String encodeName( final String pagename ) {
        try {
            return URLEncoder.encode( pagename, m_useUTF8 ? "UTF-8" : "ISO-8859-1" );
        } catch( final UnsupportedEncodingException e ) {
            throw new InternalWikiException( "ISO-8859-1 not a supported encoding!?!  Your platform is borked." , e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String decodeName( final String pagerequest ) {
        try {
            return URLDecoder.decode( pagerequest, m_useUTF8 ? "UTF-8" : "ISO-8859-1" );
        } catch( final UnsupportedEncodingException e ) {
            throw new InternalWikiException("ISO-8859-1 not a supported encoding!?!  Your platform is borked.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Charset getContentEncoding() {
        if( m_useUTF8 ) {
            return StandardCharsets.UTF_8;
        }
        return StandardCharsets.ISO_8859_1;
    }

    /**
     * Returns the {@link org.apache.wiki.workflow.WorkflowManager} associated with this WikiEngine. If the WIkiEngine has not been
     * initialized, this method will return <code>null</code>.
     *
     * @return the task queue
     */
    public WorkflowManager getWorkflowManager() {
        return getManager( WorkflowManager.class );
    }

    /**
     * {@inheritDoc}
     * <p>It is called by {@link WikiServlet#destroy()}. When this method is called, it fires a "shutdown" WikiEngineEvent to
     * all registered listeners.
     */
    @Override
    public void shutdown() {
        fireEvent( WikiEngineEvent.SHUTDOWN );
        getManager( FilterManager.class ).destroy();
    }

    /**
     *  Returns this object's ReferenceManager.
     *
     *  @return The current ReferenceManager instance.
     *  @since 1.6.1
     */
    public ReferenceManager getReferenceManager() {
        return getManager( ReferenceManager.class );
    }

    /**
     *  Returns the current rendering manager for this wiki application.
     *
     *  @since 2.3.27
     * @return A RenderingManager object.
     */
    public RenderingManager getRenderingManager() {
        return getManager( RenderingManager.class );
    }

    /**
     *  Returns the current plugin manager.
     *
     *  @since 1.6.1
     *  @return The current PluginManager instance
     */
    public PluginManager getPluginManager() {
        return getManager( PluginManager.class );
    }

    /**
     *  Returns the current variable manager.
     *
     *  @return The current VariableManager.
     */
    public VariableManager getVariableManager()  {
        return getManager( VariableManager.class );
    }

    /**
     *  Returns the current PageManager which is responsible for storing and managing WikiPages.
     *
     *  @return The current PageManager instance.
     */
    public PageManager getPageManager() {
        return getManager( PageManager.class );
    }

    /**
     * Returns the CommandResolver for this wiki engine.
     *
     * @return the resolver
     */
    public CommandResolver getCommandResolver() {
        return getManager( CommandResolver.class );
    }

    /**
     *  Returns the current AttachmentManager, which is responsible for storing and managing attachments.
     *
     *  @since 1.9.31.
     *  @return The current AttachmentManager instance
     */
    public AttachmentManager getAttachmentManager() {
        return getManager( AttachmentManager.class );
    }

    /**
     *  Returns the currently used authorization manager.
     *
     *  @return The current AuthorizationManager instance
     */
    public AuthorizationManager getAuthorizationManager()  {
        return getManager( AuthorizationManager.class );
    }

    /**
     *  Returns the currently used authentication manager.
     *
     *  @return The current AuthenticationManager instance.
     */
    public AuthenticationManager getAuthenticationManager() {
        return getManager( AuthenticationManager.class );
    }

    /**
     *  Returns the manager responsible for the filters.
     *
     *  @since 2.1.88
     *  @return The current FilterManager instance
     */
    public FilterManager getFilterManager() {
        return getManager( FilterManager.class );
    }

    /**
     *  Returns the manager responsible for searching the Wiki.
     *
     *  @since 2.2.21
     *  @return The current SearchManager instance
     */
    public SearchManager getSearchManager() {
        return getManager( SearchManager.class );
    }

    /**
     *  Returns the progress manager we're using
     *
     *  @return A ProgressManager
     *  @since 2.6
     */
    public ProgressManager getProgressManager() {
        return getManager( ProgressManager.class );
    }

    /** {@inheritDoc} */
    @Override
    public String getRootPath() {
        return m_rootPath;
    }

    /**
     * @since 2.2.6
     * @return the URL constructor
     */
    public URLConstructor getURLConstructor() {
        return getManager( URLConstructor.class );
    }

    /**
     * Returns the RSSGenerator. If the property <code>jspwiki.rss.generate</code> has not been set to <code>true</code>, this method
     * will return <code>null</code>, <em>and callers should check for this value.</em>
     *
     * @since 2.1.165
     * @return the RSS generator
     */
    public RSSGenerator getRSSGenerator() {
        return getManager( RSSGenerator.class );
    }

    /**
     *  Returns the PageRenamer employed by this WikiEngine.
     *
     *  @since 2.5.141
     *  @return The current PageRenamer instance.
     */
    public PageRenamer getPageRenamer() {
        return getManager( PageRenamer.class );
    }

    /**
     *  Returns the UserManager employed by this WikiEngine.
     *
     *  @since 2.3
     *  @return The current UserManager instance.
     */
    public UserManager getUserManager() {
        return getManager( UserManager.class );
    }

    /**
     *  Returns the TasksManager employed by this WikiEngine.
     *
     *  @return The current TasksManager instance.
     */
    public TasksManager getTasksManager() {
        return getManager( TasksManager.class );
    }

    /**
     *  Returns the GroupManager employed by this WikiEngine.
     *
     *  @since 2.3
     *  @return The current GroupManager instance
     */
    public GroupManager getGroupManager() {
        return getManager( GroupManager.class );
    }

    /**
     *  Returns the current {@link AdminBeanManager}.
     *
     *  @return The current {@link AdminBeanManager}.
     *  @since  2.6
     */
    public AdminBeanManager getAdminBeanManager() {
        return getManager( AdminBeanManager.class );
    }

    /**
     *  Returns the AclManager employed by this WikiEngine. The AclManager is lazily initialized.
     *  <p>
     *  The AclManager implementing class may be set by the System property {@link #PROP_ACL_MANAGER_IMPL}.
     *  </p>
     *
     * @since 2.3
     * @return The current AclManager.
     */
    public AclManager getAclManager()  {
        return getManager( AclManager.class );
    }

    /**
     *  Returns the DifferenceManager so that texts can be compared.
     *
     *  @return the difference manager
     */
    public DifferenceManager getDifferenceManager() {
        return getManager( DifferenceManager.class );
    }

    /**
     *  Returns the current EditorManager instance.
     *
     *  @return The current EditorManager.
     */
    public EditorManager getEditorManager() {
        return getManager( EditorManager.class );
    }

    /**
     *  Returns the current i18n manager.
     *
     *  @return The current Intertan... Interante... Internatatializ... Whatever.
     */
    public InternationalizationManager getInternationalizationManager() {
        return getManager( InternationalizationManager.class );
    }

    /** {@inheritDoc} */
    @Override
    public final synchronized void addWikiEventListener( final WikiEventListener listener ) {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /** {@inheritDoc} */
    @Override
    public final synchronized void removeWikiEventListener( final WikiEventListener listener ) {
        WikiEventManager.removeWikiEventListener( this, listener );
    }

    /**
     * Fires a WikiEngineEvent to all registered listeners.
     *
     * @param type  the event type
     */
    protected final void fireEvent( final int type ) {
        if( WikiEventManager.isListening(this ) ) {
            WikiEventManager.fireEvent( this, new WikiEngineEvent(this, type ) );
        }
    }

    /**
     * Fires a WikiPageEvent to all registered listeners.
     * @param type  the event type
     */
    protected final void firePageEvent( final int type, final String pageName ) {
        if( WikiEventManager.isListening(this ) ) {
            WikiEventManager.fireEvent(this,new WikiPageEvent(this, type, pageName ) );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setAttribute( final String key, final Object value ) {
        m_attributes.put( key, value );
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings( "unchecked" )
    public < T > T getAttribute( final String key ) {
        return ( T )m_attributes.get( key );
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings( "unchecked" )
    public < T > T removeAttribute( final String key ) {
        return ( T )m_attributes.remove( key );
    }

}
