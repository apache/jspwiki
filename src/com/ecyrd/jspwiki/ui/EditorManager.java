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
import com.ecyrd.jspwiki.plugin.PluginManager;

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
 *  @author jalkanen
 *
 */

// FIXME: This class is slightly kludgish at the moment.
// FIXME: Pull up the WikiPluginInfo; make it a generic for plugins, filters, etc.
public class EditorManager extends ModuleManager
{
    public static final String PROP_EDITORTYPE = "jspwiki.editor";
    
    public static final String EDITOR_PLAIN = "plain";
    public static final String EDITOR_FCK   = "FCK";
    public static final String EDITOR_PREVIEW = "preview";
    
    public static final String REQ_EDITEDTEXT = "_editedtext";
    public static final String ATTR_EDITEDTEXT = REQ_EDITEDTEXT;
    
    private           WikiEngine     m_engine;
    
    private Map m_editors;
    
    private static Logger log = Logger.getLogger( EditorManager.class );
    
    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;
    
        registerEditors();
    }
    
    private void registerEditors()
    {
        log.info( "Registering modules" );

        m_editors = new HashMap();
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

                    List plugins = XPath.selectNodes( doc, "/modules/editor");
                    
                    for( Iterator i = plugins.iterator(); i.hasNext(); )
                    {
                        Element pluginEl = (Element) i.next();
                        
                        String name = pluginEl.getAttributeValue("name");

                        String[] parms = new String[3];
                        parms[0] = pluginEl.getChildText("path");
                        parms[1] = pluginEl.getChildText("script");
                        parms[2] = pluginEl.getChildText("stylesheet");
                        m_editors.put( name, parms );
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
     *  Returns an editor for the current context.
     *  
     * @param context
     * @return The name of the chosen editor.
     */
    public String getEditorName( WikiContext context )
    {
        if( context.getRequestContext().equals(WikiContext.PREVIEW) )
            return EDITOR_PREVIEW;
        
        try
        {
            String editor = m_engine.getVariableManager().getValue( context, PROP_EDITORTYPE );
        
            if( EDITOR_FCK.equalsIgnoreCase(editor) )
                return EDITOR_FCK;
        }
        catch( NoSuchVariableException e ) {} // This is fine

        return EDITOR_PLAIN;
    }

    /**
     *  Returns a list of editors as Strings of editor names.
     *  
     *  @return
     */
    public String[] getEditorList()
    {
        String[] editors = new String[m_editors.size()];
        
        Set keys = m_editors.keySet();

        return (String[]) keys.toArray( editors );
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
        
        String[] ed = (String[])m_editors.get( editor );
        
        if( ed != null )
        {
            path = ed[0];
        }
        else
        {
            path = "editors/"+editor+".jsp";
        }
        
        return path;
    }

    /**
     *  Convinience function which examines the current context and attempts to figure
     *  out whether the edited text is in the HTTP request parameters or somewhere in
     *  the session.
     *  
     *  @param ctx
     *  @return
     */
    public static String getEditedText( PageContext ctx )
    {
        String usertext = ctx.getRequest().getParameter( REQ_EDITEDTEXT );
        
        if( usertext == null )
        {
            usertext = (String)ctx.getAttribute( ATTR_EDITEDTEXT, PageContext.REQUEST_SCOPE );
        }
        
        return usertext;
    }


}
