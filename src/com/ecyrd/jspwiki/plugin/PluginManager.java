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

import org.apache.oro.text.regex.*;
import org.apache.log4j.Category;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.HashMap;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.FileUtil;
import com.ecyrd.jspwiki.InternalWikiException;

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
 *  "abcdefghijklmnopqrstuvw\n01234567890". 
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
 *
 *  @author Janne Jalkanen
 *  @since 1.6.1
 */
public class PluginManager
{
    private static Category log = Category.getInstance( PluginManager.class );

    /**
     *  This is the default package to try in case the instantiation
     *  fails.
     */
    public static final String DEFAULT_PACKAGE = "com.ecyrd.jspwiki.plugin";

    /**
     *  The property name defining which packages will be searched for properties.
     */
    public static final String PROP_SEARCHPATH = "jspwiki.plugin.searchPath";

    /**
     *  The name of the body content.  Current value is "_body".
     */
    public static final String PARAM_BODY      = "_body";

    Vector  m_searchPath = new Vector();

    Pattern m_pluginPattern;

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

        //
        //  The default package is always added.
        //
        m_searchPath.add( DEFAULT_PACKAGE );

        PatternCompiler compiler = new Perl5Compiler();

        try
        {
            m_pluginPattern = compiler.compile( "\\{?(INSERT)?\\s*([\\w\\._]+)[ \\t]*(WHERE)?[ \\t]*" );
        }
        catch( MalformedPatternException e )
        {
            log.fatal("Internal error: someone messed with pluginmanager patterns.", e );
            throw new InternalWikiException( "PluginManager patterns are broken" );
        }

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
        ClassLoader loader = getClass().getClassLoader();

        try
        {
            return loader.loadClass( classname );
        }
        catch( ClassNotFoundException e )
        {
            for( Iterator i = m_searchPath.iterator(); i.hasNext(); )
            {
                String packageName = (String)i.next();

                try
                {
                    return loader.loadClass( packageName + "." + classname );
                }
                catch( ClassNotFoundException ex )
                {
                    // This is okay, we go to the next package.
                }
            }

            //
            //  Nope, it wasn't on the normal plugin path.  Let's try tags.
            //  We'll only accept tags that implement the WikiPluginTag
            //
            /*
              FIXME: Not functioning, need to create a PageContext
            try
            {
                Class c = loader.loadClass( "com.ecyrd.jspwiki.tags."+classname );

                if( c instanceof WikiPluginTag )
                {
                    return new TagPlugin( c );
                }
            }
            catch( ClassNotFoundException exx )
            {
                // Just fall through, and let the execution end with stuff.
            }
            */
        }

        throw new ClassNotFoundException("Plugin not in "+PROP_SEARCHPATH);
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
        try
        {
            Class      pluginClass;
            WikiPlugin plugin;

            pluginClass = findPluginClass( classname );

            //
            //   Create...
            //
            try
            {
                plugin = (WikiPlugin) pluginClass.newInstance();
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
                return plugin.execute( context, params );
            }
            catch( PluginException e )
            {
                // Just pass this exception onward.
                throw (PluginException) e.fillInStackTrace();
            }
            catch( Exception e )
            {
                // But all others get captured here.
                log.warn( "Plugin failed while executing:", e );
                throw new PluginException( "Plugin failed", e );
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
     *  plugin body.
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

    /*
      // FIXME: Not functioning, needs to create or fetch PageContext from somewhere.
    public class TagPlugin implements WikiPlugin
    {
        private Class m_tagClass;
        
        public TagPlugin( Class tagClass )
        {
            m_tagClass = tagClass;
        }
        
        public String execute( WikiContext context, Map params )
            throws PluginException
        {
            WikiPluginTag plugin = m_tagClass.newInstance();

            
        }
    }
    */
}
