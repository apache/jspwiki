/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
package org.apache.wiki.modules;

import java.net.URL;
import java.util.*;

import org.apache.wiki.Release;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;


/**
 *  Superclass for all JSPWiki managers for modules (plugins, etc).
 */
public abstract class ModuleManager
{

    /**
     *  The property name defining which packages will be searched for modules.
     */
    public static final String PROP_SEARCHPATH = "jspwiki.plugin.searchPath";

    /**
     * Location of the property-files of plugins.
     *  (Each plugin should include this property-file in its jar-file)
     */
    public static final String MODULE_RESOURCE_LOCATION = "ini/jspwiki_module.xml";
        
    protected WikiEngine m_engine;
    
    private static Logger log = LoggerFactory.getLogger( ModuleManager.class );
    
    private boolean m_loadIncompatibleModules = false;
    
    /**
     *  Constructs the ModuleManager.
     *  
     *  @param engine The WikiEngine which owns this manager.
     */
    public ModuleManager( WikiEngine engine )
    {
        m_engine = engine;
    }
    
    /**
     *  Returns true, if the given module is compatible with this version of JSPWiki.
     *  
     *  @param info The module to check
     *  @return True, if the module is compatible.
     */
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

    /**
     *  Builds a search path from three components:
     *  <ol>
     *  <li>The default packages.
     *  <li>The contents of {@value #PROP_SEARCHPATH}
     *  <li>Whatever can be located from the contents of the ini-files
     *  </ol>
     *  
     *  @param props
     *  @return A List of package names which should be scanned for modules.
     */
    protected List<String> buildPluginSearchPath( Properties props )
    {
        ArrayList<String> list = new ArrayList<String>();
        list.add( "org.apache.wiki" );
        list.add( "org.apache.jspwiki" );
        
        String packageNames = props.getProperty( PROP_SEARCHPATH );
    
        if( packageNames != null )
        {
            StringTokenizer tok = new StringTokenizer( packageNames, "," );
    
            while( tok.hasMoreTokens() )
            {
                list.add( tok.nextToken().trim() );
            }
        }
    
        SAXBuilder builder = new SAXBuilder();
    
        try
        {
            Enumeration resources = getClass().getClassLoader().getResources( MODULE_RESOURCE_LOCATION );
    
            while( resources.hasMoreElements() )
            {
                URL resource = (URL) resources.nextElement();
    
                try
                {
                    Document doc = builder.build( resource );
    
                    List packages = XPath.selectNodes( doc, "/modules/@package");
    
                    for( Iterator i = packages.iterator(); i.hasNext(); )
                    {
                        Attribute a = (Attribute) i.next();
    
                        list.add( a.getValue().trim() );
                    }
                }
                catch( java.io.IOException e )
                {
                    log.error( "Couldn't load " + MODULE_RESOURCE_LOCATION + " resources: " + resource, e );
                }
                catch( JDOMException e )
                {
                    log.error( "Error parsing XML for plugin: "+MODULE_RESOURCE_LOCATION );
                }
            }
        }
        catch( java.io.IOException e )
        {
            log.error( "Couldn't load all " + MODULE_RESOURCE_LOCATION + " resources", e );
        }
        
        return list;
    }
}
