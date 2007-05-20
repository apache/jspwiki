/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 JSPWiki development group

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
package com.ecyrd.jspwiki.ui.admin.beans;

import javax.management.NotCompliantMBeanException;

import com.ecyrd.jspwiki.Release;
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
    private static final String[] ATTRIBUTES = { "pages", "version" };
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

    public String getVersion()
    {
        return Release.VERSTR;
    }

    public String getVersionDescription()
    {
        return "The JSPWiki engine version";
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
