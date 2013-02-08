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
package org.apache.wiki.plugin;

import java.io.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

import org.apache.commons.lang.ClassUtils;
import org.apache.ecs.xhtml.*;
import org.apache.log4j.Logger;
import org.apache.oro.text.regex.*;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.engine.PluginManager;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.InitializablePlugin;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.modules.ModuleManager;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.parser.PluginContent;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.TextUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 *  Manages plugin classes.  There exists a single instance of PluginManager
 *  per each instance of WikiEngine, that is, each JSPWiki instance.
 *  <P>
 *  A plugin is defined to have three parts:
 *  <OL>
 *    <li>The plugin class
 *    <li>The plugin parameters
 *    <li>The plugin body
 *  </ol>
 *
 *  For example, in the following line of code:
 *  <pre>
 *  [{INSERT org.apache.wiki.plugin.FunnyPlugin  foo='bar'
 *  blob='goo'
 *
 *  abcdefghijklmnopqrstuvw
 *  01234567890}]
 *  </pre>
 *
 *  The plugin class is "org.apache.wiki.plugin.FunnyPlugin", the
 *  parameters are "foo" and "blob" (having values "bar" and "goo",
 *  respectively), and the plugin body is then
 *  "abcdefghijklmnopqrstuvw\n01234567890".   The plugin body is
 *  accessible via a special parameter called "_body".
 *  <p>
 *  If the parameter "debug" is set to "true" for the plugin,
 *  JSPWiki will output debugging information directly to the page if there
 *  is an exception.
 *  <P>
 *  The class name can be shortened, and marked without the package.
 *  For example, "FunnyPlugin" would be expanded to
 *  "org.apache.wiki.plugin.FunnyPlugin" automatically.  It is also
 *  possible to define other packages, by setting the
 *  "jspwiki.plugin.searchPath" property.  See the included
 *  jspwiki.properties file for examples.
 *  <P>
 *  Even though the nominal way of writing the plugin is
 *  <pre>
 *  [{INSERT pluginclass WHERE param1=value1...}],
 *  </pre>
 *  it is possible to shorten this quite a lot, by skipping the
 *  INSERT, and WHERE words, and dropping the package name.  For
 *  example:
 *
 *  <pre>
 *  [{INSERT org.apache.wiki.plugin.Counter WHERE name='foo'}]
 *  </pre>
 *
 *  is the same as
 *  <pre>
 *  [{Counter name='foo'}]
 *  </pre>
 *  <h3>Plugin property files</h3>
 *  <p>
 *  Since 2.3.25 you can also define a generic plugin XML properties file per
 *  each JAR file.
 *  <pre>
 *  <modules>
 *   <plugin class="org.apache.wiki.foo.TestPlugin">
 *       <author>Janne Jalkanen</author>
 *       <script>foo.js</script>
 *       <stylesheet>foo.css</stylesheet>
 *       <alias>code</alias>
 *   </plugin>
 *   <plugin class="org.apache.wiki.foo.TestPlugin2">
 *       <author>Janne Jalkanen</author>
 *   </plugin>
 *   </modules>
 *  </pre>
 *  <h3>Plugin lifecycle</h3>
 *
 *  <p>Plugin can implement multiple interfaces to let JSPWiki know at which stages they should
 *  be invoked:
 *  <ul>
 *  <li>InitializablePlugin: If your plugin implements this interface, the initialize()-method is
 *      called once for this class
 *      before any actual execute() methods are called.  You should use the initialize() for e.g.
 *      precalculating things.  But notice that this method is really called only once during the
 *      entire WikiEngine lifetime.  The InitializablePlugin is available from 2.5.30 onwards.</li>
 *  <li>ParserStagePlugin: If you implement this interface, the executeParse() method is called
 *      when JSPWiki is forming the DOM tree.  You will receive an incomplete DOM tree, as well
 *      as the regular parameters.  However, since JSPWiki caches the DOM tree to speed up later
 *      places, which means that whatever this method returns would be irrelevant.  You can do some DOM
 *      tree manipulation, though.  The ParserStagePlugin is available from 2.5.30 onwards.</li>
 *  <li>WikiPlugin: The regular kind of plugin which is executed at every rendering stage.  Each
 *      new page load is guaranteed to invoke the plugin, unlike with the ParserStagePlugins.</li>
 *  </ul>
 *
 *  @since 1.6.1
 */
public class DefaultPluginManager extends ModuleManager implements PluginManager
{
    private static final String PLUGIN_INSERT_PATTERN = "\\{?(INSERT)?\\s*([\\w\\._]+)[ \\t]*(WHERE)?[ \\t]*";

    private static Logger log = Logger.getLogger( DefaultPluginManager.class );

    private static final String DEFAULT_FORMS_PACKAGE = "org.apache.wiki.forms";

    private ArrayList<String>  m_searchPath = new ArrayList<String>();

    private Pattern m_pluginPattern;

    private boolean m_pluginsEnabled = true;

    /**
     *  Keeps a list of all known plugin classes.
     */
    private Map<String, WikiPluginInfo> m_pluginClassMap = new HashMap<String, WikiPluginInfo>();

    /**
     *  Create a new PluginManager.
     *
     *  @param engine WikiEngine which owns this manager.
     *  @param props Contents of a "jspwiki.properties" file.
     */
    public DefaultPluginManager( WikiEngine engine, Properties props )
    {
        super(engine);
        String packageNames = props.getProperty( PROP_SEARCHPATH );

        if( packageNames != null )
        {
            StringTokenizer tok = new StringTokenizer( packageNames, "," );

            while( tok.hasMoreTokens() )
            {
                m_searchPath.add( tok.nextToken().trim() );
            }
        }

        registerPlugins();

        //
        //  The default packages are always added.
        //
        m_searchPath.add( DEFAULT_PACKAGE );
        m_searchPath.add( DEFAULT_FORMS_PACKAGE );

        PatternCompiler compiler = new Perl5Compiler();

        try
        {
            m_pluginPattern = compiler.compile( PLUGIN_INSERT_PATTERN );
        }
        catch( MalformedPatternException e )
        {
            log.fatal("Internal error: someone messed with pluginmanager patterns.", e );
            throw new InternalWikiException( "PluginManager patterns are broken" );
        }

    }

    /**
     * Enables or disables plugin execution.
     * 
     * @param enabled True, if plugins should be globally enabled; false, if disabled.
     */
    public void enablePlugins( boolean enabled )
    {
        m_pluginsEnabled = enabled;
    }

    /**
     * Returns plugin execution status. If false, plugins are not
     * executed when they are encountered on a WikiPage, and an
     * empty string is returned in their place.
     * 
     * @return True, if plugins are enabled; false otherwise.
     */
    public boolean pluginsEnabled()
    {
        return m_pluginsEnabled;
    }

    /**
     *  Returns true if the link is really command to insert
     *  a plugin.
     *  <P>
     *  Currently we just check if the link starts with "{INSERT",
     *  or just plain "{" but not "{$".
     *
     *  @param link Link text, i.e. the contents of text between [].
     *  @return True, if this link seems to be a command to insert a plugin here.
     *  
     *  @deprecated will be removed in 2.10 scope. Consider using 
     *  {@link org.apache.wiki.parser.JSPWikiMarkupParser#isPluginLink(String)} instead
     */
    @Deprecated
    public static boolean isPluginLink( String link )
    {
        return link.startsWith("{INSERT") ||
               (link.startsWith("{") && !link.startsWith("{$"));
    }
    
    /**
     *  Attempts to locate a plugin class from the class path
     *  set in the property file.
     *
     *  @param classname Either a fully fledged class name, or just
     *  the name of the file (that is,
     *  "org.apache.wiki.plugin.Counter" or just plain "Counter").
     *
     *  @return A found class.
     *
     *  @throws ClassNotFoundException if no such class exists.
     */
    private Class findPluginClass( String classname )
        throws ClassNotFoundException
    {
        return ClassUtil.findClass( m_searchPath, classname );
    }

    /**
     *  Outputs a HTML-formatted version of a stack trace.
     */
    private String stackTrace( Map< String, String > params, Throwable t )
    {
        div d = new div();
        d.setClass("debug");
        d.addElement("Plugin execution failed, stack trace follows:");
        StringWriter out = new StringWriter();
        t.printStackTrace( new PrintWriter(out) );
        d.addElement( new pre( out.toString() ) );
        d.addElement( new b( "Parameters to the plugin" ) );

        ul list = new ul();
        for( Iterator<Map.Entry< String, String > > i = params.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry< String, String > e = i.next();
            String key = e.getKey();

            list.addElement(new li( key+"'='"+e.getValue() ) );
        }

        d.addElement( list );

        return d.toString();
    }

    /**
     *  Executes a plugin class in the given context.
     *  <P>Used to be private, but is public since 1.9.21.
     *
     *  @param context The current WikiContext.
     *  @param classname The name of the class.  Can also be a
     *  shortened version without the package name, since the class name is searched from the
     *  package search path.
     *
     *  @param params A parsed map of key-value pairs.
     *
     *  @return Whatever the plugin returns.
     *
     *  @throws PluginException If the plugin execution failed for
     *  some reason.
     *
     *  @since 2.0
     */
    public String execute( WikiContext context,
                           String classname,
                           Map< String, String > params )
        throws PluginException
    {
        if( !m_pluginsEnabled )
            return "";

        ResourceBundle rb = Preferences.getBundle( context, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );
        boolean debug = TextUtil.isPositive( params.get( PARAM_DEBUG ) );
        try
        {
            //
            //   Create...
            //
            WikiPlugin plugin = newWikiPlugin( classname, rb );
            if( plugin == null ) {
                return "Plugin '" + classname + "' not compatible with this version of JSPWiki";
            }

            //
            //  ...and launch.
            //
            try
            {
                return plugin.execute( context, params );
            }
            catch( PluginException e )
            {
                if( debug )
                {
                    return stackTrace( params, e );
                }

                // Just pass this exception onward.
                throw ( PluginException )e.fillInStackTrace();
            }
            catch( Throwable t )
            {
                // But all others get captured here.
                log.info( "Plugin failed while executing:", t );
                if( debug )
                {
                    return stackTrace( params, t );
                }

                throw new PluginException( rb.getString( "plugin.error.failed" ), t );
            }

        }
        catch( ClassCastException e )
        {
            throw new PluginException( MessageFormat.format( rb.getString( "plugin.error.notawikiplugin" ), classname ), e );
        }
    }

    /**
     *  Parses plugin arguments.  Handles quotes and all other kewl stuff.
     *
     *  <h3>Special parameters</h3>
     *  The plugin body is put into a special parameter defined by {@link #PARAM_BODY};
     *  the plugin's command line into a parameter defined by {@link #PARAM_CMDLINE};
     *  and the bounds of the plugin within the wiki page text by a parameter defined
     *  by {@link #PARAM_BOUNDS}, whose value is stored as a two-element int[] array,
     *  i.e., <tt>[start,end]</tt>.
     *
     * @param argstring The argument string to the plugin.  This is
     *  typically a list of key-value pairs, using "'" to escape
     *  spaces in strings, followed by an empty line and then the
     *  plugin body.  In case the parameter is null, will return an
     *  empty parameter list.
     *
     * @return A parsed list of parameters.
     *
     * @throws IOException If the parsing fails.
     */
    public Map<String, String> parseArgs( String argstring )
        throws IOException
    {
        Map<String, String> arglist = new HashMap<String, String>();

        //
        //  Protection against funny users.
        //
        if( argstring == null ) return arglist;

        arglist.put( PARAM_CMDLINE, argstring );

        StringReader    in      = new StringReader(argstring);
        StreamTokenizer tok     = new StreamTokenizer(in);
        int             type;


        String param = null;
        String value = null;

        tok.eolIsSignificant( true );

        boolean potentialEmptyLine = false;
        boolean quit               = false;

        while( !quit )
        {
            String s;

            type = tok.nextToken();

            switch( type )
            {
              case StreamTokenizer.TT_EOF:
                quit = true;
                s = null;
                break;

              case StreamTokenizer.TT_WORD:
                s = tok.sval;
                potentialEmptyLine = false;
                break;

              case StreamTokenizer.TT_EOL:
                quit = potentialEmptyLine;
                potentialEmptyLine = true;
                s = null;
                break;

              case StreamTokenizer.TT_NUMBER:
                s = Integer.toString( (int) tok.nval );
                potentialEmptyLine = false;
                break;

              case '\'':
                s = tok.sval;
                break;

              default:
                s = null;
            }

            //
            //  Assume that alternate words on the line are
            //  parameter and value, respectively.
            //
            if( s != null )
            {
                if( param == null )
                {
                    param = s;
                }
                else
                {
                    value = s;

                    arglist.put( param, value );

                    // log.debug("ARG: "+param+"="+value);
                    param = null;
                }
            }
        }

        //
        //  Now, we'll check the body.
        //

        if( potentialEmptyLine )
        {
            StringWriter out = new StringWriter();
            FileUtil.copyContents( in, out );

            String bodyContent = out.toString();

            if( bodyContent != null )
            {
                arglist.put( PARAM_BODY, bodyContent );
            }
        }

        return arglist;
    }

    /**
     *  Parses a plugin.  Plugin commands are of the form:
     *  [{INSERT myplugin WHERE param1=value1, param2=value2}]
     *  myplugin may either be a class name or a plugin alias.
     *  <P>
     *  This is the main entry point that is used.
     *
     *  @param context The current WikiContext.
     *  @param commandline The full command line, including plugin
     *  name, parameters and body.
     *
     *  @return HTML as returned by the plugin, or possibly an error
     *  message.
     *  
     *  @throws PluginException From the plugin itself, it propagates, waah!
     */
    public String execute( WikiContext context,
                           String commandline )
        throws PluginException
    {
        if( !m_pluginsEnabled )
        {
            return "";
        }

        ResourceBundle rb = Preferences.getBundle( context, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );
        PatternMatcher matcher = new Perl5Matcher();

        try
        {
            if( matcher.contains( commandline, m_pluginPattern ) )
            {
                MatchResult res = matcher.getMatch();

                String plugin   = res.group(2);
                String args     = commandline.substring(res.endOffset(0),
                                                        commandline.length() -
                                                        (commandline.charAt(commandline.length()-1) == '}' ? 1 : 0 ) );
                Map<String, String> arglist  = parseArgs( args );

                return execute( context, plugin, arglist );
            }
        }
        catch( NoSuchElementException e )
        {
            String msg =  "Missing parameter in plugin definition: "+commandline;
            log.warn( msg, e );
            throw new PluginException( MessageFormat.format( rb.getString( "plugin.error.missingparameter" ), commandline ) );
        }
        catch( IOException e )
        {
            String msg = "Zyrf.  Problems with parsing arguments: "+commandline;
            log.warn( msg, e );
            throw new PluginException( MessageFormat.format( rb.getString( "plugin.error.parsingarguments" ), commandline ) );
        }

        // FIXME: We could either return an empty string "", or
        // the original line.  If we want unsuccessful requests
        // to be invisible, then we should return an empty string.
        return commandline;
    }

    /**
     *  Parses a plugin invocation and returns a DOM element.
     *  
     *  @param context The WikiContext
     *  @param commandline The line to parse
     *  @param pos The position in the stream parsing.
     *  @return A DOM element
     *  @throws PluginException If plugin invocation is faulty
     */
   public PluginContent parsePluginLine( WikiContext context, String commandline, int pos )
        throws PluginException
    {
        PatternMatcher  matcher  = new Perl5Matcher();

        try
        {
            if( matcher.contains( commandline, m_pluginPattern ) )
            {
                MatchResult res = matcher.getMatch();

                String plugin   = res.group(2);
                String args     = commandline.substring(res.endOffset(0),
                                                        commandline.length() -
                                                        (commandline.charAt(commandline.length()-1) == '}' ? 1 : 0 ) );
                Map<String, String> arglist = parseArgs( args );

                // set wikitext bounds of plugin as '_bounds' parameter, e.g., [345,396]
                if ( pos != -1 )
                {
                    int end = pos + commandline.length() + 2;
                    String bounds = pos + "|" + end;
                    arglist.put( PARAM_BOUNDS, bounds );
                }

                PluginContent result = new PluginContent( plugin, arglist );

                return result;
            }
        }
        catch( ClassCastException e )
        {
            log.error( "Invalid type offered in parsing plugin arguments.", e );
            throw new InternalWikiException("Oops, someone offered !String!");
        }
        catch( NoSuchElementException e )
        {
            String msg =  "Missing parameter in plugin definition: "+commandline;
            log.warn( msg, e );
            throw new PluginException( msg );
        }
        catch( IOException e )
        {
            String msg = "Zyrf.  Problems with parsing arguments: "+commandline;
            log.warn( msg, e );
            throw new PluginException( msg );
        }

        return null;
    }

    /**
     *  Register a plugin.
     */
    private void registerPlugin(WikiPluginInfo pluginClass)
    {
        String name;

        // Registrar the plugin with the className without the package-part
        name = pluginClass.getName();
        if(name != null)
        {
            log.debug("Registering plugin [name]: " + name);
            m_pluginClassMap.put(name, pluginClass);
        }

        // Registrar the plugin with a short convenient name.
        name = pluginClass.getAlias();
        if(name != null)
        {
            log.debug("Registering plugin [shortName]: " + name);
            m_pluginClassMap.put(name, pluginClass);
        }

        // Registrar the plugin with the className with the package-part
        name = pluginClass.getClassName();
        if(name != null)
        {
            log.debug("Registering plugin [className]: " + name);
            m_pluginClassMap.put(name, pluginClass);
        }

        pluginClass.initializePlugin( m_engine );
    }

    private void registerPlugins()
    {
        log.info( "Registering plugins" );

        SAXBuilder builder = new SAXBuilder();

        try
        {
            //
            // Register all plugins which have created a resource containing its properties.
            //
            // Get all resources of all plugins.
            //

            Enumeration resources = getClass().getClassLoader().getResources( PLUGIN_RESOURCE_LOCATION );

            while( resources.hasMoreElements() )
            {
                URL resource = (URL) resources.nextElement();

                try
                {
                    log.debug( "Processing XML: " + resource );

                    Document doc = builder.build( resource );

                    List plugins = XPath.selectNodes( doc, "/modules/plugin");

                    for( Iterator i = plugins.iterator(); i.hasNext(); )
                    {
                        Element pluginEl = (Element) i.next();

                        String className = pluginEl.getAttributeValue("class");

                        WikiPluginInfo pluginInfo = WikiPluginInfo.newInstance( className, pluginEl );

                        if( pluginInfo != null )
                        {
                            registerPlugin( pluginInfo );
                        }
                    }
                }
                catch( java.io.IOException e )
                {
                    log.error( "Couldn't load " + PLUGIN_RESOURCE_LOCATION + " resources: " + resource, e );
                }
                catch( JDOMException e )
                {
                    log.error( "Error parsing XML for plugin: "+PLUGIN_RESOURCE_LOCATION );
                }
            }
        }
        catch( java.io.IOException e )
        {
            log.error( "Couldn't load all " + PLUGIN_RESOURCE_LOCATION + " resources", e );
        }
    }

    /**
     *  Contains information about a bunch of plugins.
     *
     *
     *  @since
     */
    // FIXME: This class needs a better interface to return all sorts of possible
    //        information from the plugin XML.  In fact, it probably should have
    //        some sort of a superclass system.
    public static final class WikiPluginInfo
        extends WikiModuleInfo
    {
        private String m_className;
        private String m_alias;
        private Class  m_clazz;

        private boolean m_initialized = false;

        /**
         *  Creates a new plugin info object which can be used to access a plugin.
         *
         *  @param className Either a fully qualified class name, or a "short" name which is then
         *                   checked against the internal list of plugin packages.
         *  @param el A JDOM Element containing the information about this class.
         *  @return A WikiPluginInfo object.
         */
        protected static WikiPluginInfo newInstance( String className, Element el )
        {
            if( className == null || className.length() == 0 ) return null;
            WikiPluginInfo info = new WikiPluginInfo( className );

            info.initializeFromXML( el );
            return info;
        }
        /**
         *  Initializes a plugin, if it has not yet been initialized.
         *
         *  @param engine The WikiEngine
         */
        protected void initializePlugin( WikiEngine engine )
        {
            if( !m_initialized )
            {
                // This makes sure we only try once per class, even if init fails.
                m_initialized = true;

                try
                {
                    WikiPlugin p = newPluginInstance();
                    if( p instanceof InitializablePlugin )
                    {
                        ((InitializablePlugin)p).initialize( engine );
                    }
                }
                catch( Exception e )
                {
                    log.info( "Cannot initialize plugin "+m_className, e );
                }
            }
        }

        /**
         *  {@inheritDoc}
         */
        @Override
        protected void initializeFromXML( Element el )
        {
            super.initializeFromXML( el );
            m_alias = el.getChildText("alias");
        }

        /**
         *  Create a new WikiPluginInfo based on the Class information.
         *  
         *  @param clazz The class to check
         *  @return A WikiPluginInfo instance
         */
        protected static WikiPluginInfo newInstance( Class clazz )
        {
            WikiPluginInfo info = new WikiPluginInfo( clazz.getName() );

            return info;
        }

        private WikiPluginInfo( String className )
        {
            super(className);
            setClassName( className );
        }

        private void setClassName( String fullClassName )
        {
            m_name = ClassUtils.getShortClassName( fullClassName );
            m_className = fullClassName;
        }

        /**
         *  Returns the full class name of this object.
         *  @return The full class name of the object.
         */
        public String getClassName()
        {
            return m_className;
        }

        /**
         *  Returns the alias name for this object.
         *  @return An alias name for the plugin.
         */
        public String getAlias()
        {
            return m_alias;
        }

        /**
         *  Creates a new plugin instance.
         *
         *  @return A new plugin.
         *  @throws ClassNotFoundException If the class declared was not found.
         *  @throws InstantiationException If the class cannot be instantiated-
         *  @throws IllegalAccessException If the class cannot be accessed.
         */
        public WikiPlugin newPluginInstance()
            throws ClassNotFoundException,
                   InstantiationException,
                   IllegalAccessException
        {
            if( m_clazz == null )
            {
                m_clazz = Class.forName(m_className);
            }

            return (WikiPlugin) m_clazz.newInstance();
        }

        /**
         *  Returns a text for IncludeResources.
         *
         *  @param type Either "script" or "stylesheet"
         *  @return Text, or an empty string, if there is nothing to be included.
         */
        public String getIncludeText(String type)
        {
            try
            {
                if( type.equals("script") )
                {
                    return getScriptText();
                }
                else if( type.equals("stylesheet") )
                {
                    return getStylesheetText();
                }
            }
            catch( Exception ex )
            {
                // We want to fail gracefully here
                return ex.getMessage();
            }

            return null;
        }

        private String getScriptText()
            throws IOException
        {
            if( m_scriptText != null )
            {
                return m_scriptText;
            }

            if( m_scriptLocation == null )
            {
                return "";
            }

            try
            {
                m_scriptText = getTextResource(m_scriptLocation);
            }
            catch( IOException ex )
            {
                // Only throw this exception once!
                m_scriptText = "";
                throw ex;
            }

            return m_scriptText;
        }

        private String getStylesheetText()
            throws IOException
        {
            if( m_stylesheetText != null )
            {
                return m_stylesheetText;
            }

            if( m_stylesheetLocation == null )
            {
                return "";
            }

            try
            {
                m_stylesheetText = getTextResource(m_stylesheetLocation);
            }
            catch( IOException ex )
            {
                // Only throw this exception once!
                m_stylesheetText = "";
                throw ex;
            }

            return m_stylesheetText;
        }

        /**
         *  Returns a string suitable for debugging.  Don't assume that the format
         *  would stay the same.
         *  
         *  @return Something human-readable
         */
        public String toString()
        {
            return "Plugin :[name=" + m_name + "][className=" + m_className + "]";
        }
    } // WikiPluginClass

    /**
     *  {@inheritDoc}
     */
    public Collection modules()
    {
        Set< WikiModuleInfo > ls = new TreeSet< WikiModuleInfo >();
        
        for( Iterator< WikiPluginInfo > i = m_pluginClassMap.values().iterator(); i.hasNext(); )
        {
            WikiModuleInfo wmi = i.next();
            if( !ls.contains( wmi ) ) ls.add( wmi );
        }
        
        return ls;
    }

    /**
     * Creates a {@link WikiPlugin}.
     * 
     * @param pluginName plugin's classname
     * @param rb {@link ResourceBundle} with i18ned text for exceptions.
     * @return a {@link WikiPlugin}.
     * @throws PluginException if there is a problem building the {@link WikiPlugin}.
     */
    public WikiPlugin newWikiPlugin( String pluginName, ResourceBundle rb ) 
        throws PluginException 
    {
        WikiPlugin plugin = null;
        WikiPluginInfo pluginInfo = m_pluginClassMap.get( pluginName );
        try
        {
            if( pluginInfo == null )
            {
                pluginInfo = WikiPluginInfo.newInstance( findPluginClass( pluginName ) );
                registerPlugin( pluginInfo );
            }

            if( !checkCompatibility( pluginInfo ) )
            {
                String msg = "Plugin '" + pluginInfo.getName() + "' not compatible with this version of JSPWiki";
                log.info( msg );
            } else {
                plugin = pluginInfo.newPluginInstance();
            }
        }
        catch( ClassNotFoundException e )
        {
            throw new PluginException( MessageFormat.format( rb.getString( "plugin.error.couldnotfind" ), pluginName ), e );
        }
        catch( InstantiationException e )
        {
            throw new PluginException( MessageFormat.format( rb.getString( "plugin.error.cannotinstantiate" ), pluginName ), e );
        }
        catch( IllegalAccessException e )
        {
            throw new PluginException( MessageFormat.format( rb.getString( "plugin.error.notallowed" ), pluginName ), e );
        }
        catch( Exception e )
        {
            throw new PluginException( MessageFormat.format( rb.getString( "plugin.error.instantationfailed" ), pluginName ), e );
        }
        return plugin;
    }
    
}
