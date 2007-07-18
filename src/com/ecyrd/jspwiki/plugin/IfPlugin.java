/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.WikiContext;

/**
 *  The IfPlugin allows parts of a WikiPage to be executed conditionally.
 *  You can also use shorthand "If" to run it.
 *  Parameters:
 *  <ul>
 *    <li>group - A "|" -separated list of group names.
 *    <li>user  - A "|" -separated list of user names.
 *    <li>ip    - A "|" -separated list of ip addresses.
 *  </ul>
 *  
 *  <p>If any of them match, the body of the plugin is executed.  You can
 *  negate the content by prefixing it with a "!".  For example, to greet
 *  all admins, put the following in your LeftMenu:</p>
 *  <pre>
 *  [{If group='Admin'
 *  
 *  Hello, Admin, and your mighty powers!}]
 *  </pre>
 *  
 *  <p>In order to send a message to everybody except Jack use</p>
 *  <pre>
 *  [{If user='!Jack'
 *  
 *  %%warning
 *  Jack's surprise birthday party at eleven!
 *  %%}]
 *  </pre>
 *  
 *  <p>Note that you can't use "!Jack|!Jill", because for Jack, !Jill matches;
 *  and for Jill, !Jack matches.  These are not regular expressions (though
 *  they might become so in the future).<p>
 *  
 *  <p>Another caveat is that the plugin body content is not counted
 *  towards ReferenceManager links.  So any links do not appear on any reference
 *  lists.  Depending on your position, this may be a good or a bad
 *  thing.</p>
 *  
 *  @author Janne Jalkanen
 *  @since 2.6
 */
public class IfPlugin implements WikiPlugin
{
    /**
     *  {@inheritDoc}
     */
    public String execute(WikiContext context, Map params) throws PluginException
    {
        boolean include = false;
        
        String group = (String)params.get("group");
        String user  = (String)params.get("user");
        String ip    = (String)params.get("ip");
        
        include |= checkGroup(context, group);
        include |= checkUser(context, user);
        include |= checkIP(context, ip);

        if( include )
        {
            String ztuff = (String) params.get( PluginManager.PARAM_BODY );
            
            return context.getEngine().textToHTML( context, ztuff );
        }
        
        return "";
    }
    
    private boolean checkGroup(WikiContext context, String group)
    {
        if( group == null ) return false;
        String[] groupList = StringUtils.split(group,'|');
        boolean include = false;
        
        for( int i = 0; i < groupList.length; i++ )
        {
            String gname = groupList[i];
            boolean invert = false;
            if( groupList[i].startsWith("!") )
            {
                gname = groupList[i].substring(1);
                invert = true;
            }
            
            Principal g = context.getEngine().getAuthorizationManager().resolvePrincipal(gname);
            
            include |= context.getEngine().getAuthorizationManager().isUserInRole( context.getWikiSession(), g ) ^ invert;
        }
        return include;
    }

    private boolean checkUser(WikiContext context, String user)
    {
        if( user == null || context.getCurrentUser() == null ) return false;
        
        String[] list = StringUtils.split(user,'|');
        boolean include = false;
        
        for( int i = 0; i < list.length; i++ )
        {
            boolean invert = false;
            if( list[i].startsWith("!") )
            {
                invert = true;
            }
            
            include |= user.equals( context.getCurrentUser().getName() ) ^ invert;
        }
        return include;
    }
    
    // TODO: Add subnetwork matching, e.g. 10.0.0.0/8
    private boolean checkIP(WikiContext context, String ipaddr)
    {
        if( ipaddr == null || context.getHttpRequest() == null ) return false;
        
        String[] list = StringUtils.split(ipaddr,'|');
        boolean include = false;
        
        for( int i = 0; i < list.length; i++ )
        {
            boolean invert = false;
            if( list[i].startsWith("!") )
            {
                invert = true;
            }
            
            include |= ipaddr.equals( context.getHttpRequest().getRemoteAddr() ) ^ invert;
        }
        return include;
    }
}
