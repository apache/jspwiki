package com.ecyrd.jspwiki.ui.admin.beans;

import java.util.Collection;
import java.util.Iterator;

import javax.management.NotCompliantMBeanException;

import org.apache.ecs.xhtml.*;

import com.ecyrd.jspwiki.Release;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.modules.WikiModuleInfo;
import com.ecyrd.jspwiki.plugin.WikiPlugin;
import com.ecyrd.jspwiki.plugin.PluginManager.WikiPluginInfo;
import com.ecyrd.jspwiki.ui.admin.SimpleAdminBean;

public class PluginBean extends SimpleAdminBean
{
    private WikiEngine m_engine;
    
    public PluginBean(WikiEngine engine) throws NotCompliantMBeanException
    {
        m_engine = engine;
    }

    public String[] getAttributeNames()
    {
        return new String[0];
    }

    public String[] getMethodNames()
    {
        return new String[0];
    }

    public String getTitle()
    {
        return "Plugins";
    }

    public int getType()
    {
        return CORE;
    }

    public String doGet(WikiContext context)
    {
        Collection plugins = m_engine.getPluginManager().modules();
        
        div root = new div();
        
        root.addElement( new h4("Plugins") );
        
        table tb = new table().setBorder(1);
        root.addElement(tb);
        
        tr head = new tr();
        head.addElement( new th("Name") );
        head.addElement( new th("Alias") );
        head.addElement( new th("Author") );
        head.addElement( new th("Notes") );
        
        tb.addElement(head);
        
        for( Iterator i = plugins.iterator(); i.hasNext(); )
        {
            tr  row = new tr();
            tb.addElement( row );
            
            WikiPluginInfo info = (WikiPluginInfo)i.next();

            row.addElement( new td(info.getName()) );
            row.addElement( new td(info.getAlias()) );
            row.addElement( new td(info.getAuthor()) );
            
            String verWarning = "";
            if( !(Release.isNewerOrEqual(info.getMinVersion()) && Release.isOlderOrEqual(info.getMaxVersion())) )
            {
                verWarning = "<span class='warning'>This module is not compatible with this version of JSPWiki.</span>";
            }
            
            row.addElement( new td(verWarning) );
        }
        
        return root.toString();
    }

}
