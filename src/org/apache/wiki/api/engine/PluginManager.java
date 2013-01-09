package org.apache.wiki.api.engine;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.parser.PluginContent;

public interface PluginManager
{
    /** The property name defining which packages will be searched for properties. */
    static final String PROP_SEARCHPATH = "jspwiki.plugin.searchPath";
    
    /** This is the default package to try in case the instantiation fails. */
    static final String DEFAULT_PACKAGE = "org.apache.wiki.plugin";

    /**
     *  The name of the body content.  Current value is "_body".
     */
    static final String PARAM_BODY      = "_body";

    /** The name of the command line content parameter. The value is "_cmdline". */
    static final String PARAM_CMDLINE   = "_cmdline";

    /**
     *  The name of the parameter containing the start and end positions in the
     *  read stream of the plugin text (stored as a two-element int[], start
     *  and end resp.).
     */
    static final String PARAM_BOUNDS    = "_bounds";

    /** A special name to be used in case you want to see debug output */
    static final String PARAM_DEBUG     = "debug";

    /**
     * Enables or disables plugin execution.
     * 
     * @param enabled True, if plugins should be globally enabled; false, if disabled.
     */
    void enablePlugins( boolean enabled );

    /**
     * Returns plugin execution status. If false, plugins are not
     * executed when they are encountered on a WikiPage, and an
     * empty string is returned in their place.
     * 
     * @return True, if plugins are enabled; false otherwise.
     */
    boolean pluginsEnabled();

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
    String execute( WikiContext context, String classname, Map< String, String > params )
        throws PluginException;

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
    Map< String, String > parseArgs( String argstring )
        throws IOException;

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
    String execute( WikiContext context, String commandline )
        throws PluginException;

    /**
     *  Parses a plugin invocation and returns a DOM element.
     *  
     *  @param context The WikiContext
     *  @param commandline The line to parse
     *  @param pos The position in the stream parsing.
     *  @return A DOM element
     *  @throws PluginException If plugin invocation is faulty
     */
    PluginContent parsePluginLine( WikiContext context, String commandline, int pos )
        throws PluginException;

    /**
     * Returns a collection of modules currently managed by this ModuleManager.  Each
     * entry is an instance of the WikiModuleInfo class.  This method should return something
     * which is safe to iterate over, even if the underlying collection changes.
     * 
     * @return A Collection of WikiModuleInfo instances.
     */
    Collection modules();

    /**
     *  Executes parse stage, unless plugins are disabled.
     *  
     *  @param content The content item.
     *  @param context A WikiContext
     *  @throws PluginException If something goes wrong.
     */
    void executeParse(PluginContent content, WikiContext context)
        throws PluginException;
    
}
