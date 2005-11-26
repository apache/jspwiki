package com.ecyrd.jspwiki.modules;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import org.jdom.Element;

import com.ecyrd.jspwiki.FileUtil;

/**
 *  A WikiModule describes whatever JSPWiki plugin there is: it can be a plugin,
 *  an editor, a filter, etc.
 *  @author jalkanen
 *  @since 2.4
 */
public class WikiModuleInfo
{
    protected String m_name;
    protected String m_scriptLocation;
    protected String m_scriptText;
    protected String m_stylesheetLocation;
    protected String m_stylesheetText;
    protected String m_author;
    protected URL    m_resource;

    protected void initializeFromXML( Element el )
    {
        m_scriptLocation = el.getChildText("script");
        m_stylesheetLocation = el.getChildText("stylesheet");
        m_author = el.getChildText("author");            
    }

    /**
     *  Returns the common name for this particular module.  Note that
     *  this is not the class name, nor is it an alias.  For different modules
     *  the name may have different meanings.
     *  <p>
     *  Every module defines a name, so this method should never return null.
     *  
     *  @return A module name.
     */
    public String getName()
    {
        return m_name;
    }

    public String getStylesheetLocation()
    {
        return m_stylesheetLocation;
    }

    public String getScriptLocation()
    {
        return m_scriptLocation;
    }

    /**
     *  Returns the name of the author of this plugin (if defined).
     * @return Author name, or null.
     */
    public String getAuthor()
    {
        return m_author;
    }

    protected String getTextResource(String resourceLocation) 
        throws IOException
    {
        if(m_resource == null)
        {
            return "";
        }
    
        // The text of this resource should be loaded from the same
        //   jar-file as the jspwiki_modules.xml -file! This is because 2 plugins
        //   could have the same name of the resourceLocation!
        //   (2 plugins could have their stylesheet-files in 'ini/jspwiki.css')
    
        // So try to construct a resource that loads this resource from the
        //   same jar-file.
        String spec = m_resource.toString();
    
        // Replace the 'PLUGIN_RESOURCE_LOCATION' with the requested
        //   resourceLocation.
        int length = ModuleManager.PLUGIN_RESOURCE_LOCATION.length();
        spec = spec.substring(0, spec.length() - length) + resourceLocation;
    
        URL url = new URL(spec);
        BufferedInputStream   in  = new BufferedInputStream(url.openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        
        FileUtil.copyContents( in, out );
    
        in.close();
        String text = out.toString();
        out.close();
        
        return text;
    }

}
