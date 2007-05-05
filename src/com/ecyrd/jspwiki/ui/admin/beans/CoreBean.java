/**
 * 
 */
package com.ecyrd.jspwiki.ui.admin.beans;

import javax.management.NotCompliantMBeanException;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.ui.admin.SimpleAdminBean;

/**
 *  An AdminBean which manages the JSPWiki core operations.
 *  
 *  @author jalkanen
 */
public class CoreBean
    extends SimpleAdminBean
{
    private static final String[] ATTRIBUTES = { "pages" };
    private static final String[] METHODS = { };
    private WikiEngine m_engine;
    
    public CoreBean( WikiEngine engine ) throws NotCompliantMBeanException
    {
        m_engine = engine;
    }
       
    /**
     *  Return the page count in the Wiki.
     *  
     *  @return
     */
    public int getPages()
    {
        return m_engine.getPageCount();
    }
    
    public String getPagesDescription()
    {
        return "The total number of pages in this wiki";
    }
    
    public String getTitle()
    {
        return "Core bean";
    }

    public int getType()
    {
        return CORE;
    }


    public String getId()
    {
        return "corebean";
    }
    
    public String[] getAttributeNames()
    {
        return ATTRIBUTES;
    }

    public String[] getMethodNames()
    {
        return METHODS;
    }
    
}