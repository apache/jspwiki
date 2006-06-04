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
 *  @author Christoph Sauer
 *  @author Chuck Smith
 *  @since 2.4
 */
public class EditorManager extends ModuleManager
{
    /** The property name for setting the editor.  Current value is "jspwiki.editor" */
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
    
    private             WikiEngine   m_engine;
    
    private             Map          m_editors;
    
    private static      Logger       log             = Logger.getLogger( EditorManager.class );
    
    /**
     *  Initializes the EditorManager.  It also registers any editors it can find.
     *  
     *  @param engine The WikiEngine we're attached to.
     *  @param props  Properties for setup.
     */
    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;
    
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
                        
                        log.debug("Registered editor "+name);
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
     *  1. Attribute in HttpSession: used when an alternative editor has been chosen
     *  2. Editor set in User Preferences (not yet implemented)
     *  3. Default Editor set in jspwiki.properties
     *  <p>
     *  For the PREVIEW context, this method returns the "preview" editor.
     *  
     * @param context The context that is chosen.
     * @return The name of the chosen editor.  If no match could be found, will
     *         revert to the default "plain" editor.
     */
    // FIXME: Look at user preferences
    public String getEditorName( WikiContext context )
    {
        if( context.getRequestContext().equals(WikiContext.PREVIEW) )
            return EDITOR_PREVIEW;
        
        String editor = null;
        
        // If a parameter "editor" is provided, then set it as session attribute, so that following
        // calls can make use of it. This parameter is set by the links created
        // through the EditorIteratorTag
        
        editor = context.getHttpParameter( PARA_EDITOR );
        if (editor != null)
        {
            context.getHttpRequest().getSession().setAttribute( PARA_EDITOR, editor );
        }
        
        // First condition:  
        // an attribute in the Session is provided
        editor = (String) context.getHttpRequest().getSession().getAttribute( PARA_EDITOR );
        if (editor != null)
        {
            return editor;
        }
        
        // Second condition:
        // User has set an editor in preferences
        // TODO...
        
        // Third condition:
        // use the default editor in jspwiki.properties
        try
        {
            
            editor = m_engine.getVariableManager().getValue( context, PROP_EDITORTYPE );
        
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
        catch( NoSuchVariableException e ) {} // This is fine

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
            usertext = (String)ctx.getAttribute( ATTR_EDITEDTEXT, PageContext.REQUEST_SCOPE );
        }
        
        return usertext;
    }
}
