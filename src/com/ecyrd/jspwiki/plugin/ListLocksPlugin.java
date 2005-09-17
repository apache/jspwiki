/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.plugin;

import org.apache.log4j.Logger;
import com.ecyrd.jspwiki.*;
import java.util.*;

/**
 *  This is a plugin for the administrator: It allows him to see in a single
 *  glance who is editing what.
 *
 *  @author Janne Jalkanen
 *  @since 2.0.22.
 */
public class ListLocksPlugin
    implements WikiPlugin
{
    private static Logger log = Logger.getLogger( ListLocksPlugin.class );

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        StringBuffer result = new StringBuffer();

        PageManager mgr = context.getEngine().getPageManager();
        List locks = mgr.getActiveLocks();

        result.append("<table class=\"listlocksplugin\" border=1>\n");
        result.append("<tr>\n");
        result.append("<th>Page</th><th>Locked by</th><th>Acquired</th><th>Expires</th>\n");
        result.append("</tr>");

        if( locks.size() == 0 )
        {
            result.append("<tr><td colspan=4>No locks exist currently.</td></tr>\n");
        }
        else
        {
            for( Iterator i = locks.iterator(); i.hasNext(); )
            {
                PageLock lock = (PageLock) i.next();

                result.append("<tr>");
                result.append("<td>"+lock.getPage()+"</td>");
                result.append("<td>"+lock.getLocker()+"</td>");
                result.append("<td>"+lock.getAcquisitionTime()+"</td>");
                result.append("<td>"+lock.getExpiryTime()+"</td>");
                result.append("</tr>\n");
            }
        }

        result.append("</table>");

        return result.toString();
    }

}
