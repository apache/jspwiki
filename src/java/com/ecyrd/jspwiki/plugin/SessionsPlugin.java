/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
package com.ecyrd.jspwiki.plugin;

import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.jspwiki.api.PluginException;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;

/**
 *  <p>Displays information about active wiki sessions. The parameter
 *  <code>property</code> specifies what information is displayed.
 *  If omitted, the number of sessions is returned.
 *  
 *  <p>Parameters : </p>
 *  <ul>
 *  <li><b>property</b> - specify what output to display, valid values are:</li>
 *  <ul>
 *    <li><code>users</code> - returns a comma-separated list of
 *    users</li>
 *    <li><code>distinctUsers</code> - will only show
 *    distinct users.
 *  </ul>
 *  </ul>
 *  @since 2.3.84
 *  @author Andrew Jaquith
 */
public class SessionsPlugin
    implements WikiPlugin
{
    /** The parameter name for setting the property value. */
    public static final String PARAM_PROP = "property";
    
    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        WikiEngine engine = context.getEngine();
        String prop = (String) params.get( PARAM_PROP );
        
        if ( "users".equals( prop ) )
        {
            Principal[] principals = WikiSession.userPrincipals( engine );
            StringBuilder s = new StringBuilder();
            for ( int i = 0; i < principals.length; i++ )
            {
                s.append(principals[i].getName() + ", ");
            }
            // remove the last comma and blank :
            return s.substring(0, s.length() - (s.length() > 2 ? 2 : 0) );
        }

        //
        // show each user session only once (with a counter that indicates the
        // number of sessions for each user)
        if ("distinctUsers".equals(prop))
        {
            Principal[] principals = WikiSession.userPrincipals(engine);
            // we do not assume that the principals are sorted, so first count
            // them :
            HashMap<String,Integer> distinctPrincipals = new HashMap<String,Integer>();
            for (int i = 0; i < principals.length; i++)
            {
                String principalName = principals[i].getName();

                if (distinctPrincipals.containsKey(principalName))
                {
                    // we already have an entry, increase the counter:
                    int numSessions = distinctPrincipals.get(principalName).intValue();
                    // store the new value:
                    distinctPrincipals.put(principalName, ++numSessions);
                }
                else
                {
                    // first time we see this entry, add entry to HashMap with
                    // value 1
                    distinctPrincipals.put(principalName, 1);
                }
            }
            //
            //
            StringBuilder s = new StringBuilder();
            Iterator entries = distinctPrincipals.entrySet().iterator();
            while (entries.hasNext())
            {
                Map.Entry entry = (Map.Entry)entries.next();
                s.append( entry.getKey().toString() + "(" + entry.getValue().toString() + "), " );
            }
            // remove the last comma and blank :
            return s.substring(0, s.length() - 2);
        }

        return String.valueOf( WikiSession.sessions( engine ) );
    }
}
