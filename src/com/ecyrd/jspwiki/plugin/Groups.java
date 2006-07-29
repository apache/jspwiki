/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.security.Principal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.PrincipalComparator;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;

/**
 *  <p>Prints the groups managed by this wiki, separated by commas.
 *  The groups are sorted in ascending order, and are hyperlinked
 *  to the page that displays the group's members.</p>
 *  @since 2.4.19
 *  @author Andrew Jaquith
 */
public class Groups
    implements WikiPlugin
{
    private static final Comparator COMPARATOR = new PrincipalComparator();
    
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        // Retrieve groups, and sort by name
        WikiEngine engine = context.getEngine();
        GroupManager groupMgr = engine.getGroupManager();
        Principal[] groups = groupMgr.getRoles();
        Arrays.sort( groups, COMPARATOR );

        StringBuffer s = new StringBuffer();
        
        for ( int i = 0; i < groups.length; i++ )
        {
            String name = groups[i].getName();
            
            // Make URL
            String url = engine.getURLConstructor().makeURL( WikiContext.VIEW_GROUP, name, false, null );
            
            // Create hyperlink
            s.append( "<a href=\"" );
            s.append( url );
            s.append( "\">" );
            s.append( name );
            s.append( "</a>" );
            
            // If not the last one, add a comma and space
            if ( i < ( groups.length - 1 ) )
            {
                s.append( ',' );
                s.append( ' ' );
            }
        }
        return s.toString();
    }
}
