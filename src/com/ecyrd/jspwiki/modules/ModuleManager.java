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

import java.util.Collection;

import com.ecyrd.jspwiki.Release;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Superclass for all JSPWiki managers for modules (plugins, etc).
 *  @author jalkanen
 */
public abstract class ModuleManager
{

    /**
     * Location of the property-files of plugins.
     *  (Each plugin should include this property-file in its jar-file)
     */
    public static final String PLUGIN_RESOURCE_LOCATION = "ini/jspwiki_module.xml";
    
    public static final String LOAD_INCOMPATIBLE_MODULES = "jspwiki.loadIncompatibleModules";
    
    protected WikiEngine m_engine;
    
    private boolean m_loadIncompatibleModules = false;
    
    public ModuleManager( WikiEngine engine )
    {
        m_engine = engine;
    }
    
    public boolean checkCompatibility( WikiModuleInfo info )
    {
        if( !m_loadIncompatibleModules )
        {
            String minVersion = info.getMinVersion();
            String maxVersion = info.getMaxVersion();
            
            return Release.isNewerOrEqual( minVersion ) && Release.isOlderOrEqual( maxVersion );
        }
        
        return true;
    }
    
    /**
     * Returns a collection of modules currently managed by this ModuleManager.  Each
     * entry is an instance of the WikiModuleInfo class.  This method should return something
     * which is safe to iterate over, even if the underlying collection changes.
     * 
     * @return A Collection of WikiModuleInfo instances.
     */
    public abstract Collection modules();
}
