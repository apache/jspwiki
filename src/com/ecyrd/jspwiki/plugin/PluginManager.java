/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.plugin;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.apache.commons.lang.ClassUtils;
import org.apache.ecs.xhtml.*;
import org.apache.log4j.Logger;
import org.apache.oro.text.regex.*;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import com.ecyrd.jspwiki.FileUtil;
import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.modules.ModuleManager;
import com.ecyrd.jspwiki.modules.WikiModuleInfo;
import com.ecyrd.jspwiki.parser.PluginContent;
import com.ecyrd.jspwiki.util.ClassUtil;

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
 *  [{INSERT com.ecyrd.jspwiki.plugin.FunnyPlugin  foo='bar'
 *  blob='goo'
 *
 *  abcdefghijklmnopqrstuvw
 *  01234567890}]
 *  </pre>
 *
 *  The plugin class is "com.ecyrd.jspwiki.plugin.FunnyPlugin", the
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
 *  "com.ecyrd.jspwiki.plugin.FunnyPlugin" automatically.  It is also
 *  possible to defined other packages, by setting the
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
 *  [{INSERT com.ecyrd.jspwiki.plugin.Counter WHERE name='foo'}]
 *  </pre>
 *
 *  is the same as
 *  <pre>
 *  [{Counter name='foo'}]
 *  </pre>
 *  <p>
 *  Since 2.3.25 you can also define a generic plugin XML properties file per
 *  each JAR file.
 *  <pre>
 *  <modules>
 *   <plugin class="com.ecyrd.jspwiki.foo.TestPlugin">
 *       <author>Janne Jalkanen</author>
 *       <script>foo.js</script>
 *       <stylesheet>foo.css</stylesheet>
 *       <alias>code</alias>
 *   </plugin>
 *   <plugin class="com.ecyrd.jspwiki.foo.TestPlugin2">
 *       <author>Janne Jalkanen</author>
 *   </plugin>
 *   </modules>
 *  </pre>
 *  
 *  @author Janne Jalkanen
 *  @since 1.6.1
 */
public class PluginManager extends ModuleManager
{
    private static final String PLUGIN_INSERT_PATTERN = "\\{?(INSERT)?\\s*([\\w\\._]+)[ \\t]*(WHERE)?[ \\t]*";

    private static Logger log = Logger.getLogger( PluginManager.class );

    /**
     *  This is the default package to try in case the instantiation
     *  fails.
     */
    public static final String DEFAULT_PACKAGE = "com.ecyrd.jspwiki.plugin";
 
    public static final String DEFAULT_FORMS_PACKAGE = "com.ecyrd.jspwiki.forms";

    /**
     *  The property name defining which packages will be searched for properties.
     */
    public static final String PROP_SEARCHPATH = "jspwiki.plugin.searchPath";

    /**
     *  The name of the body content.  Current value is "_body".
     */
    public static final String PARAM_BODY      = "_body";

    /**
     *  The name of the command line content parameter. The value is "_cmdline".
     */
    public static final String PARAM_CMDLINE   = "_cmdline";

    /**
     *  A special name to be used in case you want to see debug output
     */
    public static final String PARAM_DEBUG     = "debug";

    Vector  m_searchPath = new Vector();

    Pattern m_pluginPattern;

    private boolean m_pluginsEnabled = true;
    private boolean m_initStage      = false;

    /** 
     *  Keeps a list of all known plugin classes.
     */
    private Map m_pluginClassMap = new HashMap();


    /**
     *  Create a new PluginManager.
     *
     *  @param props Contents of a "jspwiki.properties" file.
     */
    public PluginManager( Properties props )
    {
        String packageNames = props.getProperty( PROP_SEARCHPATH );

        if( packageNames != null )
        {
            StringTokenizer tok = new StringTokenizer( packageNames, "," );

            while( tok.hasMoreTokens() )
            {
                m_searchPath.add( tok.nextToken() );
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
     */
    public void enablePlugins( boolean enabled )
    {
        m_pluginsEnabled = enabled;
    }

    /**
     *  Sets the initialization stage for the initial page scan.
     */
    public void setInitStage( boolean value )
    {
        m_initStage = value;
    }

    /**
     * Returns plugin execution status. If false, plugins are not 
     * executed when they are encountered on a WikiPage, and an
     * empty string is returned in their place.
     */
    public boolean pluginsEnabled()
    {
        return( m_pluginsEnabled );
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
     */
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
     *  "com.ecyrd.jspwiki.plugin.Counter" or just plain "Counter").
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
    private String stackTrace( Map params, Throwable t )
    {
        div d = new div();
        d.setClass("debug");
        d.addElement("Plugin execution failed, stack trace follows:");
        StringWriter out = new StringWriter();
        t.printStackTrace( new PrintWriter(out) );
        d.addElement( new pre( out.toString() ) );
        d.addElement( new b( "Parameters to the plugin" ) );
        
        ul list = new ul();
        for( Iterator i = params.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry e = (Map.Entry) i.next();
            String key = (String)e.getKey();

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
                           Map params )
        throws PluginException
    {
        if( !m_pluginsEnabled )
            return( "" );

        try
        {
            WikiPlugin plugin;

            boolean debug = TextUtil.isPositive( (String) params.get( PARAM_DEBUG ) );

            WikiPluginInfo pluginInfo = (WikiPluginInfo) m_pluginClassMap.get(classname);
            
            if(pluginInfo == null)
            {
                pluginInfo = WikiPluginInfo.newInstance(findPluginClass( classname ));
                registerPlugin(pluginInfo);
            }

            //
            //   Create...
            //
            try
            {
                plugin = (WikiPlugin) pluginInfo.newPluginInstance();
            }
            catch( InstantiationException e )
            {
                throw new PluginException( "Cannot instantiate plugin "+classname, e );
            }
            catch( IllegalAccessException e )
            {
                throw new PluginException( "Not allowed to access plugin "+classname, e );
            }
            catch( Exception e )
            {
                throw new PluginException( "Instantiation of plugin "+classname+" failed.", e );
            }

            //
            //  ...and launch.
            //
            try
            {
                if( m_initStage )
                {
                    if( plugin instanceof InitializablePlugin )
                    {
                        ((InitializablePlugin)plugin).initialize( context, params );
                    }
                    return "";
                }
                
                return plugin.execute( context, params );
            }
            catch( PluginException e )
            {
                if( debug )
                {
                    return stackTrace( params, e );
                }

                // Just pass this exception onward.
                throw (PluginException) e.fillInStackTrace();
            }
            catch( Throwable t )
            {
                // But all others get captured here.
                log.info( "Plugin failed while executing:", t );
                if( debug )
                {
                    return stackTrace( params, t );
                }

                throw new PluginException( "Plugin failed", t );
            }
            
        }
        catch( ClassNotFoundException e )
        {
            throw new PluginException( "Could not find plugin "+classname, e );
        }
        catch( ClassCastException e )
        {
            throw new PluginException( "Class "+classname+" is not a Wiki plugin.", e );
        }
    }


    /**
     *  Parses plugin arguments.  Handles quotes and all other kewl
     *  stuff.
     *  
     *  @param argstring The argument string to the plugin.  This is
     *  typically a list of key-value pairs, using "'" to escape
     *  spaces in strings, followed by an empty line and then the
     *  plugin body.  In case the parameter is null, will return an
     *  empty parameter list.
     *
     *  @return A parsed list of parameters.  The plugin body is put
     *  into a special parameter defined by PluginManager.PARAM_BODY.
     *
     *  @throws IOException If the parsing fails.
     */

    public Map parseArgs( String argstring )
        throws IOException
    {
        HashMap         arglist = new HashMap();

        //
        //  Protection against funny users.
        //
        if( argstring == null ) return arglist;

        arglist.put( PARAM_CMDLINE, argstring );

        StringReader    in      = new StringReader(argstring);
        StreamTokenizer tok     = new StreamTokenizer(in);
        int             type;


        String param = null, value = null;

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
                s = Integer.toString( new Double(tok.nval).intValue() );
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
     */
    public String execute( WikiContext context,
                           String commandline )
        throws PluginException
    {
        if( !m_pluginsEnabled )
            return( "" );

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
                Map arglist     = parseArgs( args );

                return execute( context, plugin, arglist );
            }
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

        // FIXME: We could either return an empty string "", or
        // the original line.  If we want unsuccessful requests
        // to be invisible, then we should return an empty string.
        return commandline;
    }

   public Content parsePluginLine( WikiContext context, String commandline )
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
                Map arglist     = parseArgs( args );

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
    
    /** Registrar a plugin.
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
     *  @author Kees Kuip
     *  @author Janne Jalkanen
     *
     *  @since
     */
    // FIXME: This class needs a better interface to return all sorts of possible
    //        information from the plugin XML.  In fact, it probably should have
    //        some sort of a superclass system.
    protected static class WikiPluginInfo
        extends WikiModuleInfo
    {       
        private String m_className;
        private String m_alias;
        private Class  m_clazz;

        protected static WikiPluginInfo newInstance( String className, Element el )
        {
            if( className == null || className.length() == 0 ) return null;
            WikiPluginInfo info = new WikiPluginInfo( className );
            
            info.initializeFromXML( el );
            return info;
        }
        
        protected void initializeFromXML( Element el )
        {
            super.initializeFromXML( el );
            m_alias = el.getChildText("alias");
        }
        
        protected static WikiPluginInfo newInstance( Class clazz )
        {
            WikiPluginInfo info = new WikiPluginInfo( clazz.getName() );
            
            return info;
        }
        
        private WikiPluginInfo(String className)
        {
            setClassName(className);
        }
        
        private void setClassName(String fullClassName)
        {
            m_name = ClassUtils.getShortClassName( fullClassName );
            m_className = fullClassName;
        }

        public String getClassName()
        {
            return m_className;
        }

        public String getAlias()
        {
            return m_alias;
        }

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
            catch(Exception ex)
            {
                return ex.getMessage();
            }

            return null;
        }

        private String getScriptText()
            throws IOException
        {
            if (m_scriptText != null)
            {
                return m_scriptText;
            }

            if (m_scriptLocation == null)
            {
                return "";
            }

            try
            {
                m_scriptText = getTextResource(m_scriptLocation);
            }
            catch(IOException ex)
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
            if (m_stylesheetText != null)
            {
                return m_stylesheetText;
            }

            if (m_stylesheetLocation == null)
            {
                return "";
            }

            try
            {
                m_stylesheetText = getTextResource(m_stylesheetLocation);
            }
            catch(IOException ex)
            {
                // Only throw this exception once!
                m_stylesheetText = "";
                throw ex;
            }

            return m_stylesheetText;
        }
        
        public String toString()
        {
            return "Plugin :[name=" + m_name + "][className=" + m_className + "]";
        }
    } // WikiPluginClass
}
