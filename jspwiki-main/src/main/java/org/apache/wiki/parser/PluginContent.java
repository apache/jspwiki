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
package org.apache.wiki.parser;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.engine.PluginManager;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.ParserStagePlugin;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.preferences.Preferences;
import org.jdom2.Text;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;


/**
 * Stores the contents of a plugin in a WikiDocument DOM tree.
 * <p/>
 * If the WikiContext.VAR_WYSIWYG_EDITOR_MODE is set to Boolean.TRUE in the context, then the plugin is rendered as WikiMarkup.
 * This allows an HTML editor to work without rendering the plugin each time as well.
 * <p/>
 * If WikiContext.VAR_EXECUTE_PLUGINS is set to Boolean.FALSE, then the plugin is not executed.
 *
 * @since 2.4
 */
public class PluginContent extends Text {

    private static final String BLANK = "";
    private static final String CMDLINE = "_cmdline";
    private static final String ELEMENT_BR = "<br/>";
    private static final String EMITTABLE_PLUGINS = "Image|FormOpen|FormClose|FormInput|FormTextarea|FormSelect";
    private static final String LINEBREAK = "\n";
    private static final String PLUGIN_START = "[{";
    private static final String PLUGIN_END = "}]";
    private static final String SPACE = " ";

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(PluginContent.class);

    private String m_pluginName;
    private Map<String, String> m_params;

    /**
     * Creates a new DOM element with the given plugin name and a map of parameters.
     *
     * @param pluginName The FQN of a plugin.
     * @param parameters A Map of parameters.
     */
    public PluginContent( final String pluginName, final Map< String, String > parameters) {
        m_pluginName = pluginName;
        m_params = parameters;
    }

    /**
     * Returns the name of the plugin invoked by the DOM element.
     *
     * @return Name of the plugin
     * @since 2.5.7
     */
    public String getPluginName() {
        return m_pluginName;
    }

    /**
     * Returns a parameter value from the parameter map.
     *
     * @param name the name of the parameter.
     * @return The value from the map, or null, if no such parameter exists.
     */
    public String getParameter( final String name) {
        return m_params.get(name);
    }

    /**
     * Returns the parameter map given in the constructor.
     *
     * @return The parameter map.
     */
    public Map< String, String > getParameters() {
        return m_params;
    }

    /**
     * Returns the rendered plugin.  Only calls getText().
     *
     * @return HTML
     */
    public String getValue() {
        return getText();
    }

    /**
     * The main invocation for the plugin.  When the getText() is called, it
     * invokes the plugin and returns its contents.  If there is no Document
     * yet, only returns the plugin name itself.
     *
     * @return The plugin rendered according to the options set in the WikiContext.
     */
    public String getText() {
        final WikiDocument doc = ( WikiDocument )getDocument();
        if( doc == null ) {
            //
            // This element has not yet been attached anywhere, so we simply assume there is no rendering and return the plugin name.
            // This is required e.g. when the paragraphify() checks whether the element is empty or not.  We can't of course know
            // whether the rendering would result in an empty string or not, but let us assume it does not.
            //
            return getPluginName();
        }

        final WikiContext context = doc.getContext();
        if( context == null ) {
            log.info( "WikiContext garbage-collected, cannot proceed" );
            return getPluginName();
        }

        return invoke( context );
    }

    /**
     * Performs plugin invocation and return its contents.
     * 
     * @param context WikiContext in which the plugin is executed. Must NOT be null.
     * @return plugin contents.
     */
	public String invoke( final WikiContext context ) {
		String result;
		final Boolean wysiwygVariable = ( Boolean )context.getVariable( WikiContext.VAR_WYSIWYG_EDITOR_MODE );
        boolean wysiwygEditorMode = false;
        if( wysiwygVariable != null ) {
            wysiwygEditorMode = wysiwygVariable;
        }

        try {
            //
            //  Determine whether we should emit the actual code for this plugin or
            //  whether we should execute it.  For some plugins we always execute it,
            //  since they can be edited visually.
            //
            // FIXME: The plugin name matching should not be done here, but in a per-editor resource
            if (wysiwygEditorMode && !m_pluginName.matches(EMITTABLE_PLUGINS)) {
                result = PLUGIN_START + m_pluginName + SPACE;

                // convert newlines to <br> in case the plugin has a body.
                final String cmdLine = m_params.get( CMDLINE ).replaceAll( LINEBREAK, ELEMENT_BR );
                result = result + cmdLine + PLUGIN_END;
            } else {
                final Boolean b = ( Boolean )context.getVariable( WikiContext.VAR_EXECUTE_PLUGINS );
                if (b != null && !b ) {
                    return BLANK;
                }

                final WikiEngine engine = context.getEngine();
                final Map< String, String > parsedParams = new HashMap<>();

                //  Parse any variable instances from the string
                for( final Map.Entry< String, String > e : m_params.entrySet() ) {
                    String val = e.getValue();
                    val = engine.getVariableManager().expandVariables( context, val );
                    parsedParams.put( e.getKey(), val );
                }
                final PluginManager pm = engine.getPluginManager();
                result = pm.execute( context, m_pluginName, parsedParams );
            }
        } catch( final Exception e ) {
            if( wysiwygEditorMode ) {
                result = "";
            } else {
                // log.info("Failed to execute plugin",e);
                final ResourceBundle rb = Preferences.getBundle( context, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );
                result = MarkupParser.makeError( MessageFormat.format( rb.getString( "plugin.error.insertionfailed" ), 
                		                                               context.getRealPage().getWiki(), 
                		                                               context.getRealPage().getName(), 
                		                                               e.getMessage() ) ).getText();
            }
        }

        return result;
	}

    /**
     * Executes the executeParse() method.
     *
     * @param context The WikiContext
     * @throws PluginException If something goes wrong.
     */
    public void executeParse( final WikiContext context ) throws PluginException {
        final PluginManager pm = context.getEngine().getPluginManager();
        if( pm.pluginsEnabled() ) {
            final ResourceBundle rb = Preferences.getBundle(context, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);
            final Map< String, String > params = getParameters();
            final WikiPlugin plugin = pm.newWikiPlugin( getPluginName(), rb );
            try {
                if( plugin instanceof ParserStagePlugin ) {
                    ( ( ParserStagePlugin )plugin ).executeParser(this, context, params );
                }
            } catch( final ClassCastException e ) {
                throw new PluginException( MessageFormat.format( rb.getString("plugin.error.notawikiplugin"), getPluginName() ), e );
            }
        }
    }

    /**
     * Parses a plugin invocation and returns a DOM element.
     *
     * @param context     The WikiContext
     * @param commandline The line to parse
     * @param pos         The position in the stream parsing.
     * @return A DOM element
     * @throws PluginException If plugin invocation is faulty
     * @since 2.10.0
     */
    public static PluginContent parsePluginLine( final WikiContext context, final String commandline, final int pos ) throws PluginException {
        final PatternMatcher matcher = new Perl5Matcher();

        try {
            final PluginManager pm = context.getEngine().getPluginManager();
            if( matcher.contains( commandline, pm.getPluginPattern() ) ) {
                final MatchResult res = matcher.getMatch();
                final String plugin = res.group( 2 );
                final String args = commandline.substring( res.endOffset( 0 ),
                                                           commandline.length() - ( commandline.charAt( commandline.length() - 1 ) == '}' ? 1 : 0 ) );
                final Map< String, String > arglist = pm.parseArgs( args );

                // set wikitext bounds of plugin as '_bounds' parameter, e.g., [345,396]
                if( pos != -1 ) {
                    final int end = pos + commandline.length() + 2;
                    final String bounds = pos + "|" + end;
                    arglist.put( PluginManager.PARAM_BOUNDS, bounds );
                }

                return new PluginContent( plugin, arglist );
            }
        } catch( final ClassCastException e ) {
            log.error( "Invalid type offered in parsing plugin arguments.", e );
            throw new InternalWikiException( "Oops, someone offered !String!", e );
        } catch( final NoSuchElementException e ) {
            final String msg = "Missing parameter in plugin definition: " + commandline;
            log.warn( msg, e );
            throw new PluginException( msg );
        } catch( final IOException e ) {
            final String msg = "Zyrf.  Problems with parsing arguments: " + commandline;
            log.warn( msg, e );
            throw new PluginException( msg );
        }

        return null;
    }

}
