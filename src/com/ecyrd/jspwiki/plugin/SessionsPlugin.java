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
import java.util.Map;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;

/**
 *  <p>Displays information about active wiki sessions. The parameter
 *  <code>property</code> specifies what information is displayed.
 *  If omitted, the number of sessions is returned.
 *  Valid values for the <code>property</code> parameter
 *  include:</p>
 *  <ul>
 *    <li><code>users</code> - returns a comma-separated list of
 *    users</li>
 *  </ul>
 *  @since 2.3.84
 *  @author Andrew Jaquith
 */
public class SessionsPlugin
    implements WikiPlugin
{
    public static final String PARAM_PROP = "property";
    
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        WikiEngine engine = context.getEngine();
        String prop = (String) params.get( PARAM_PROP );
        
        if ( "users".equals( prop ) )
        {
            Principal[] principals = WikiSession.userPrincipals( engine );
            StringBuffer s = new StringBuffer();
            for ( int i = 0; i < principals.length; i++ )
            {
                s.append( principals[i].getName() );
                if ( i < ( principals.length - 1 ) )
                {
                    s.append( ',' );
                    s.append( ' ' );
                }
            }
            return s.toString();
        }

        return String.valueOf( WikiSession.sessions( engine ) );
    }
}
