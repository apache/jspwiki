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
    protected String m_minVersion;
    protected String m_maxVersion;
    protected String m_adminBeanClass;
    
    public WikiModuleInfo( String name )
    {
        m_name = name;
    }
    
    protected void initializeFromXML( Element el )
    {
        m_scriptLocation     = el.getChildText("script");
        m_stylesheetLocation = el.getChildText("stylesheet");
        m_author             = el.getChildText("author");
        m_minVersion         = el.getChildText("minVersion");
        m_maxVersion         = el.getChildText("maxVersion");
        m_adminBeanClass     = el.getChildText("adminBean");
    }

    public String getAdminBeanClass()
    {
        return m_adminBeanClass;
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


    public String getMinVersion()
    {
        return m_minVersion;
    }
    
    public String getMaxVersion()
    {
        return m_maxVersion;
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
