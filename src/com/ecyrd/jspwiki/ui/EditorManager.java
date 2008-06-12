/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.ui;

import java.net.URL;
import java.util.*;

import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import com.ecyrd.jspwiki.NoSuchVariableException;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.modules.ModuleManager;
import com.ecyrd.jspwiki.modules.WikiModuleInfo;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.ecyrd.jspwiki.preferences.Preferences;

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
 *  @author Christoph Sauer
 *  @author Chuck Smith
 *  @author Dirk Frederickx
 *  @since 2.4
 */
public class EditorManager extends ModuleManager
{
    /** The property name for setting the editor.  Current value is "jspwiki.editor" */
    /* not used anymore -- replaced by defaultpref.template.editor */
    public static final String       PROP_EDITORTYPE = "jspwiki.editor";

    /** Parameter for changing editors at run-time */
    public static final String       PARA_EDITOR     = "editor";

    /** Known name for the plain wikimarkup editor. */
    public static final String       EDITOR_PLAIN    = "plain";

    /** Known name for the preview editor component. */
    public static final String       EDITOR_PREVIEW  = "preview";

    /** Known attribute name for storing the user edited text inside a HTTP parameter. */
    public static final String       REQ_EDITEDTEXT  = "_editedtext";

    /** Known attribute name for storing the user edited text inside a session or a page context */
    public static final String       ATTR_EDITEDTEXT = REQ_EDITEDTEXT;

    private             Map<String, WikiEditorInfo>  m_editors;

    private static      Logger       log             = Logger.getLogger( EditorManager.class );

    public EditorManager( WikiEngine engine )
    {
        super(engine);
    }

    /**
     *  Initializes the EditorManager.  It also registers any editors it can find.
     *
     *  @param props  Properties for setup.
     */
    public void initialize( Properties props )
    {
        registerEditors();
    }

    /**
     *  This method goes through the jspwiki_module.xml files and hunts for editors.
     *  Any editors found are put in the registry.
     *
     */
    private void registerEditors()
    {
        log.info( "Registering editor modules" );

        m_editors = new HashMap<String, WikiEditorInfo>();
        SAXBuilder builder = new SAXBuilder();

        try
        {
            //
            // Register all editors which have created a resource containing its properties.
            //
            // Get all resources of all modules
            //

            Enumeration resources = getClass().getClassLoader().getResources( PLUGIN_RESOURCE_LOCATION );

            while( resources.hasMoreElements() )
            {
                URL resource = (URL) resources.nextElement();

                try
                {
                    log.debug( "Processing XML: " + resource );

                    Document doc = builder.build( resource );

                    List plugins = XPath.selectNodes( doc, "/modules/editor");

                    for( Iterator i = plugins.iterator(); i.hasNext(); )
                    {
                        Element pluginEl = (Element) i.next();

                        String name = pluginEl.getAttributeValue("name");

                        WikiEditorInfo info = WikiEditorInfo.newInstance(name, pluginEl);

                        if( checkCompatibility(info) )
                        {
                            m_editors.put( name, info );

                            log.debug("Registered editor "+name);
                        }
                        else
                        {
                            log.info("Editor '"+name+"' not compatible with this version of JSPWiki.");
                        }
                    }
                }
                catch( java.io.IOException e )
                {
                    log.error( "Couldn't load " + PluginManager.PLUGIN_RESOURCE_LOCATION + " resources: " + resource, e );
                }
                catch( JDOMException e )
                {
                    log.error( "Error parsing XML for plugin: "+PluginManager.PLUGIN_RESOURCE_LOCATION );
                }
            }
        }
        catch( java.io.IOException e )
        {
            log.error( "Couldn't load all " + PLUGIN_RESOURCE_LOCATION + " resources", e );
        }
    }

    /**
     *  Returns an editor for the current context.  The editor names are matched in
     *  a case insensitive manner.  At the moment, the only place that this method
     *  looks in is the property file, but in the future this will also look at
     *  user preferences.
     *  <p>
     *  Determines the editor to use by the following order of conditions:
     *  1. Editor set in User Preferences
     *  2. Default Editor set in jspwiki.properties
     *  <p>
     *  For the PREVIEW context, this method returns the "preview" editor.
     *
     * @param context The context that is chosen.
     * @return The name of the chosen editor.  If no match could be found, will
     *         revert to the default "plain" editor.
     */
    public String getEditorName( WikiContext context )
    {
        if( context.getRequestContext().equals(WikiContext.PREVIEW) )
            return EDITOR_PREVIEW;

        String editor = null;

        // User has set an editor in preferences
        editor = Preferences.getPreference( context, PARA_EDITOR );

        /* FIXME: actual default 'editor' property is read by the Preferences class */
        if (editor == null)
        {
            // or use the default editor in jspwiki.properties
            try
            {
                editor = m_engine.getVariableManager().getValue( context, PROP_EDITORTYPE );
            }
            catch( NoSuchVariableException e ) {} // This is fine
        }

        if (editor != null)
        {
            String[] editorlist = getEditorList();

            editor = editor.trim();

            for( int i = 0; i < editorlist.length; i++ )
            {
                if( editorlist[i].equalsIgnoreCase(editor))
                {
                    return editorlist[i];
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
    public String[] getEditorList()
    {
        String[] editors = new String[m_editors.size()];

        Set<String> keys = m_editors.keySet();

        return keys.toArray( editors );
    }

    /**
     *  Convenience method for getting the path to the editor JSP file.
     *
     *  @param context
     *  @return e.g. "editors/plain.jsp"
     */
    public String getEditorPath( WikiContext context )
    {
        String path = null;

        String editor = getEditorName( context );

        WikiEditorInfo ed = m_editors.get( editor );

        if( ed != null )
        {
            path = ed.getPath();
        }
        else
        {
            path = "editors/"+editor+".jsp";
        }

        return path;
    }

    /**
     *  Convenience function which examines the current context and attempts to figure
     *  out whether the edited text is in the HTTP request parameters or somewhere in
     *  the session.
     *
     *  @param ctx the JSP page context
     *  @return the edited text, if present in the session page context or as a parameter
     */
    public static String getEditedText( PageContext ctx )
    {
        String usertext = ctx.getRequest().getParameter( REQ_EDITEDTEXT );

        if( usertext == null )
        {
            usertext = (String)ctx.findAttribute( ATTR_EDITEDTEXT );
        }

        return usertext;
    }

    /**
     *  Contains info about an editor.
     *
     */
    private static final class WikiEditorInfo
        extends WikiModuleInfo
    {
        private String m_path;

        protected static WikiEditorInfo newInstance( String name, Element el )
        {
            if( name == null || name.length() == 0 ) return null;
            WikiEditorInfo info = new WikiEditorInfo( name );

            info.initializeFromXML( el );
            return info;
        }

        protected void initializeFromXML( Element el )
        {
            super.initializeFromXML( el );
            m_path = el.getChildText("path");
        }

        private WikiEditorInfo( String name )
        {
            super(name);
        }

        public String getPath()
        {
            return m_path;
        }
    }

    public Collection modules()
    {
        ArrayList<WikiModuleInfo> ls = new ArrayList<WikiModuleInfo>();

        ls.addAll( m_editors.values() );

        return ls;
    }

}
