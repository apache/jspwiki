/*
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
package org.apache.wiki.plugin;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.util.TextUtil;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 *  <p>Displays information about active wiki sessions. The parameter <code>property</code> specifies what information is displayed.
 *  If omitted, the number of sessions is returned.
 *
 *  <p>Parameters : </p>
 *  <ul>
 *  <li><b>property</b> - specify what output to display, valid values are:</li>
 *  <ul>
 *    <li><code>users</code> - returns a comma-separated list of users</li>
 *    <li><code>distinctUsers</code> - will only show distinct users.</li>
 *  </ul>
 *  </ul>
 *  @since 2.3.84
 */
public class SessionsPlugin implements Plugin {

    /** The parameter name for setting the property value. */
    public static final String PARAM_PROP = "property";

    /**
     *  {@inheritDoc}
     */
    @Override
    public String execute( final Context context, final Map<String, String> params ) throws PluginException {
        final Engine engine = context.getEngine();
        final String prop = params.get( PARAM_PROP );

        if( "users".equals( prop ) ) {
            final Principal[] principals = SessionMonitor.getInstance( engine ).userPrincipals();
            final StringBuilder s = new StringBuilder();
            for( final Principal principal : principals ) {
                s.append( principal.getName() ).append( ", " );
            }
            // remove the last comma and blank :
            return TextUtil.replaceEntities( s.substring( 0, s.length() - ( s.length() > 2 ? 2 : 0 ) ) );
        }

        // show each user session only once (with a counter that indicates the number of sessions for each user)
        if( "distinctUsers".equals( prop ) ) {
            final Principal[] principals = SessionMonitor.getInstance( engine ).userPrincipals();
            // we do not assume that the principals are sorted, so first count them :
            final HashMap< String, Integer > distinctPrincipals = new HashMap<>();
            for( final Principal principal : principals ) {
                final String principalName = principal.getName();

                if( distinctPrincipals.containsKey( principalName ) ) {
                    // we already have an entry, increase the counter:
                    int numSessions = distinctPrincipals.get( principalName );
                    // store the new value:
                    distinctPrincipals.put( principalName, ++numSessions );
                } else {
                    // first time we see this entry, add entry to HashMap with value 1
                    distinctPrincipals.put( principalName, 1 );
                }
            }

            final StringBuilder s = new StringBuilder();
            for( final Map.Entry< String, Integer > entry : distinctPrincipals.entrySet() ) {
                s.append( entry.getKey() ).append( "(" ).append( entry.getValue().toString() ).append( "), " );
            }
            // remove the last comma and blank :
            return TextUtil.replaceEntities( s.substring( 0, s.length() - ( s.length() > 2 ? 2 : 0 ) ) );

        }

        return String.valueOf( SessionMonitor.getInstance( engine ).sessions() );
    }
}
