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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.lang.ClassUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.ParserStagePlugin;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.parser.PluginContent;
import org.jdom.Element;

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
 *  
 *  @deprecated will be removed in 2.10 scope. Consider using {@link DefaultPluginManager} instead
 */
@Deprecated
public class PluginManager extends DefaultPluginManager
{

    public PluginManager( WikiEngine engine, Properties props )
    {
        super(engine, props);
    }
    
    /**
     * {@inheritDoc}
     * @deprecated Remains because of old API compatobility; will be removed in 2.10 scope. 
     * Consider using {@link DefaultPluginManager#execute(WikiContext, String, Map)} instead
     */
    @Deprecated
    public String execute( WikiContext context,
                           String classname,
                           Map< String, String > params )
        throws PluginException {
        String str = null;
        try
        {
            str = super.execute(context, classname, params);
        }
        catch (org.apache.wiki.api.exceptions.PluginException e)
        {
            throw new PluginException( e.getMessage(), e );
        }
        return str;
    }
    
    /**
     * {@inheritDoc}
     * @deprecated Remains because of old API compatobility; will be removed in 2.10 scope. 
     * Consider using {@link DefaultPluginManager#execute(WikiContext, String)} instead
     */
    @Deprecated
    public String execute( WikiContext context,
                           String commandline )
        throws PluginException {
        String str = null;
        try
        {
            str = super.execute(context, commandline );
        }
        catch (org.apache.wiki.api.exceptions.PluginException e)
        {
            throw new PluginException( e.getMessage(), e );
        }
        return str;
    }
    
    /**
     * {@inheritDoc}
     * @deprecated Remains because of old API compatobility; will be removed in 2.10 scope. 
     * Consider using {@link DefaultPluginManager#parsePluginLine(WikiContext, String, int)} instead
     */
    @Deprecated
    public PluginContent parsePluginLine( WikiContext context, String commandline, int pos )
        throws PluginException
    {
       return super.parsePluginLine(context, commandline, pos );
    }
    
    /**
     *  Executes parse stage, unless plugins are disabled.
     *  
     *  @param content The content item.
     *  @param context A WikiContext
     *  @throws PluginException If something goes wrong.
     *  @deprecated Remains because of old API compatobility; will be removed in 2.10 scope. 
     *  Consider using {@link PluginContent#executeParse(WikiContext)} instead
     */
    @Deprecated
    public void executeParse(PluginContent content, WikiContext context)
        throws PluginException
    {
        if( !pluginsEnabled() )
            return;

        ResourceBundle rb = context.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);
        Map<String, String> params = content.getParameters();
        WikiPlugin plugin = newWikiPlugin( content.getPluginName(), rb );
        try
        {
            if( plugin != null && plugin instanceof ParserStagePlugin )
            {
                ( ( ParserStagePlugin )plugin ).executeParser( content, context, params );
            }
        }
        catch( ClassCastException e )
        {
            throw new PluginException( MessageFormat.format( rb.getString( "plugin.error.notawikiplugin" ), content.getPluginName() ), e );
        }
    }
    
    /**
     *  Contains information about a bunch of plugins.
     *
     *
     *  @since
     *  @deprecated  will be removed in 2.10 scope. Consider using 
     *  {@link DefaultPluginManager.WikiPluginInfo} instead.
     */
    // FIXME: This class needs a better interface to return all sorts of possible
    //        information from the plugin XML.  In fact, it probably should have
    //        some sort of a superclass system.
    @Deprecated
    public static final class WikiPluginInfo
        extends WikiModuleInfo
    {
        private String m_className;
        private String m_alias;
        private Class  m_clazz;
        
        private static Logger log = Logger.getLogger( WikiPluginInfo.class );

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
    
}
