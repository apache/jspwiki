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

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.ui.admin.SimpleAdminBean;

/**
 *  This class is still experimental.
 *
 * @author jalkanen
 *
 */
public class WikiWizardAdminBean
    extends SimpleAdminBean
{
    private static final String[] ATTRIBUTES = {};
    private static final String[] METHODS    = {};

    public WikiWizardAdminBean() throws NotCompliantMBeanException
    {
    }


    public String getTitle()
    {
        return "WikiWizard";
    }

    public int getType()
    {
        return EDITOR;
    }

    public boolean isEnabled()
    {
        return true;
    }

    public String getId()
    {
        return "editor.wikiwizard";
    }

    public String[] getAttributeNames()
    {
        return ATTRIBUTES;
    }

    public String[] getMethodNames()
    {
        return METHODS;
    }

    public String doGet()
    {
        return "(C) i3G Institut Hochschule Heilbronn";
    }

    public void initialize(WikiEngine engine)
    {
        // TODO Auto-generated method stub

    }
}
