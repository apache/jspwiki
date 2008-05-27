/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.tags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ecyrd.jspwiki.ui.admin.AdminBean;
import com.ecyrd.jspwiki.ui.admin.AdminBeanManager;

public class AdminBeanIteratorTag extends IteratorTag
{
    private static final long serialVersionUID = 1L;

    private int m_type;

    public void setType(String type)
    {
        m_type = AdminBeanManager.getTypeFromString( type );
    }

    public void resetIterator()
    {
        AdminBeanManager mgr = m_wikiContext.getEngine().getAdminBeanManager();

        Collection beans = mgr.getAllBeans();

        ArrayList typedBeans = new ArrayList();

        for( Iterator i = beans.iterator(); i.hasNext(); )
        {
            AdminBean ab = (AdminBean)i.next();

            if( ab.getType() == m_type )
            {
                typedBeans.add( ab );
            }
        }

        setList( typedBeans );
    }
}
