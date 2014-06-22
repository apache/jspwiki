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
package org.apache.wiki.ui.admin.beans;

import java.util.Collection;

import javax.management.NotCompliantMBeanException;

import org.apache.commons.lang.StringUtils;
import org.apache.wiki.Release;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.engine.PluginManager;
import org.apache.wiki.plugin.DefaultPluginManager.WikiPluginInfo;
import org.apache.wiki.ui.admin.SimpleAdminBean;
import org.apache.wiki.util.XHTML;
import org.apache.wiki.util.XhtmlUtil;
import org.jdom2.Element;

public class PluginBean extends SimpleAdminBean {
	
    private WikiEngine m_engine;
    
    private static final String VER_WARNING = "<span class='warning'>This module is not compatible with this version of JSPWiki.</span>";
    
    public PluginBean( WikiEngine engine ) throws NotCompliantMBeanException {
        m_engine = engine;
    }

    public String[] getAttributeNames() {
        return new String[0];
    }

    public String[] getMethodNames() {
        return new String[0];
    }

    public String getTitle() {
        return "Plugins";
    }

    public int getType() {
        return CORE;
    }

    @SuppressWarnings("unchecked")
    public String doGet(WikiContext context) {
        PluginManager pm = m_engine.getPluginManager();
        Collection< WikiPluginInfo > plugins = pm.modules();
        
        Element root = XhtmlUtil.element( XHTML.div );
        Element tb =  XhtmlUtil.element( XHTML.table ).setAttribute( "border", "1" );
        
        root.addContent( XhtmlUtil.element( XHTML.h4 ).addContent( "Plugins") )
            .addContent( tb );
        
        Element trHead = XhtmlUtil.element( XHTML.tr );
        trHead.addContent( XhtmlUtil.element( XHTML.th ).addContent( "Name" ) )
              .addContent( XhtmlUtil.element( XHTML.th ).addContent( "Alias" ) )
              .addContent( XhtmlUtil.element( XHTML.th ).addContent( "Author" ) )
              .addContent( XhtmlUtil.element( XHTML.th ).addContent( "Notes" ) );
        
        tb.addContent( trHead );
        
        for( WikiPluginInfo info : plugins ) {
            Element tr = XhtmlUtil.element( XHTML.tr );
            tr.addContent( XhtmlUtil.element( XHTML.td ).addContent( info.getName() ) ) 
              .addContent( XhtmlUtil.element( XHTML.td ).addContent( info.getAlias() ) ) 
              .addContent( XhtmlUtil.element( XHTML.td ).addContent( info.getAuthor() ) )
              .addContent( XhtmlUtil.element( XHTML.td ).addContent( validPluginVersion( info ) ) );
                
            tb.addContent( tr );
        }

        return XhtmlUtil.serialize( root, XhtmlUtil.EXPAND_EMPTY_NODES );
    }

    String validPluginVersion( WikiPluginInfo info ) {
        return Release.isNewerOrEqual( info.getMinVersion() ) && Release.isOlderOrEqual( info.getMaxVersion() ) 
               ? StringUtils.EMPTY 
               : VER_WARNING;
    }

}
