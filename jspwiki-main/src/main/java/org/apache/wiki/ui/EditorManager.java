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
package org.apache.wiki.ui;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.NoSuchVariableException;
import org.apache.wiki.modules.ModuleManager;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.util.XmlUtil;
import org.jdom2.Element;

import javax.servlet.jsp.PageContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 *  Defines an editor manager.  An editor can be added by adding a
 *  suitable JSP file under templates/default/editors
 *  If you want your editor to include any scripts or something, you
 *  can simply request it by adding the following in your
 *  ini/jspwiki_module.xml:
 *
 *  <pre>
 *  <modules>
 *   <editor name="myeditor">
 *       <author>Janne Jalkanen</author>
 *       <script>foo.js</script>
 *       <stylesheet>foo.css</stylesheet>
 *       <path>editors/myeditor.jsp</path>
 *   </editor>
 *  </modules>
 *  </pre>
 *
 *  @since 2.4
 */
public class EditorManager extends ModuleManager {

    /** The property name for setting the editor. Current value is "jspwiki.editor" - not used anymore: replaced by defaultpref.template.editor */
    public static final String PROP_EDITORTYPE = "jspwiki.editor";

    /** Parameter for changing editors at run-time */
    public static final String PARA_EDITOR = "editor";

    /** Known name for the plain wikimarkup editor. */
    public static final String EDITOR_PLAIN = "plain";

    /** Known name for the preview editor component. */
    public static final String EDITOR_PREVIEW = "preview";

    /** Known attribute name for storing the user edited text inside a HTTP parameter. */
    public static final String REQ_EDITEDTEXT = "_editedtext";

    /** Known attribute name for storing the user edited text inside a session or a page context */
    public static final String ATTR_EDITEDTEXT = REQ_EDITEDTEXT;

    private Map< String, WikiEditorInfo > m_editors;

    private static final Logger log = Logger.getLogger( EditorManager.class );

    public EditorManager( final WikiEngine engine ) {
        super( engine );
    }

    /**
     *  Initializes the EditorManager.  It also registers any editors it can find.
     *
     *  @param props  Properties for setup.
     */
    public void initialize( final Properties props ) {
        registerEditors();
    }

    /**
     *  This method goes through the jspwiki_module.xml files and hunts for editors.
     *  Any editors found are put in the registry.
     *
     */
    private void registerEditors() {
        log.info( "Registering editor modules" );
        m_editors = new HashMap<>();

        //
        // Register all editors which have created a resource containing its properties.
        //
        // Get all resources of all modules
        //
        final List< Element > editors = XmlUtil.parse( PLUGIN_RESOURCE_LOCATION, "/modules/editor" );
        for( final Element pluginEl : editors ) {
            final String name = pluginEl.getAttributeValue( "name" );
            final WikiEditorInfo info = WikiEditorInfo.newInstance( name, pluginEl );

            if( checkCompatibility( info ) ) {
                m_editors.put( name, info );
                log.debug( "Registered editor " + name );
            } else {
                log.info( "Editor '" + name + "' not compatible with this version of JSPWiki." );
            }
        }
    }

    /**
     *  Returns an editor for the current context.  The editor names are matched in a case insensitive manner.  At the moment, the only
     *  place that this method looks in is the property file, but in the future this will also look at user preferences.
     *  <p>
     *  Determines the editor to use by the following order of conditions:
     *  1. Editor set in User Preferences
     *  2. Default Editor set in jspwiki.properties
     *  <p>
     *  For the PREVIEW context, this method returns the "preview" editor.
     *
     * @param context The context that is chosen.
     * @return The name of the chosen editor. If no match could be found, will revert to the default "plain" editor.
     */
    public String getEditorName( final WikiContext context ) {
        if( context.getRequestContext().equals( WikiContext.PREVIEW ) ) {
            return EDITOR_PREVIEW;
        }

        // User has set an editor in preferences
        String editor = Preferences.getPreference( context, PARA_EDITOR );

        /* FIXME: actual default 'editor' property is read by the Preferences class */
        if( editor == null ) {
            // or use the default editor in jspwiki.properties
            try {
                editor = m_engine.getVariableManager().getValue( context, PROP_EDITORTYPE );
            } catch( final NoSuchVariableException e ) {} // This is fine
        }

        if( editor != null ) {
            final String[] editorlist = getEditorList();
            editor = editor.trim();
            for( final String s : editorlist ) {
                if( s.equalsIgnoreCase( editor ) ) {
                    return s;
                }
            }
        }

        return EDITOR_PLAIN;
    }

    /**
     *  Returns a list of editors as Strings of editor names.
     *
     *  @return the list of available editors
     */
    public String[] getEditorList() {
        final String[] editors = new String[ m_editors.size() ];
        final Set< String > keys = m_editors.keySet();

        return keys.toArray( editors );
    }

    /**
     *  Convenience method for getting the path to the editor JSP file.
     *
     *  @param context WikiContext from where the editor name is retrieved.
     *  @return e.g. "editors/plain.jsp"
     */
    public String getEditorPath( final WikiContext context ) {
        final String editor = getEditorName( context );
        final WikiEditorInfo ed = m_editors.get( editor );
        final String path;
        if( ed != null ) {
            path = ed.getPath();
        } else {
            path = "editors/"+editor+".jsp";
        }

        return path;
    }

    /**
     *  Convenience function which examines the current context and attempts to figure out whether the edited text is in the HTTP
     *  request parameters or somewhere in the session.
     *
     *  @param ctx the JSP page context
     *  @return the edited text, if present in the session page context or as a parameter
     */
    public static String getEditedText( final PageContext ctx ) {
        String usertext = ctx.getRequest().getParameter( REQ_EDITEDTEXT );
        if( usertext == null ) {
            usertext = ( String )ctx.findAttribute( ATTR_EDITEDTEXT );
        }

        return usertext;
    }

    /**  Contains info about an editor. */
    private static final class WikiEditorInfo extends WikiModuleInfo {
        private String m_path;

        protected static WikiEditorInfo newInstance( final String name, final Element el ) {
            if( name == null || name.length() == 0 ) {
                return null;
            }
            final WikiEditorInfo info = new WikiEditorInfo( name );
            info.initializeFromXML( el );
            return info;
        }

        protected void initializeFromXML( final Element el ) {
            super.initializeFromXML( el );
            m_path = el.getChildText("path");
        }

        private WikiEditorInfo( final String name ) {
            super( name );
        }

        public String getPath() {
            return m_path;
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection< WikiModuleInfo > modules() {
        return modules( m_editors.values().iterator() );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public WikiEditorInfo getModuleInfo( final String moduleName ) {
        return m_editors.get( moduleName );
    }

}
