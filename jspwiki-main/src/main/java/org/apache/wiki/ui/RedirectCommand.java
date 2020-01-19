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
package org.apache.wiki.ui;

import java.security.Permission;

/**
 * <p>Defines Commands for redirections to off-site special pages. RedirectCommands do not have associated permissions; the
 * {@link #requiredPermission()} method will always return <code>null</code>. When combined with a supplied String url,
 * the {@link #getTarget()} method will return a String, the {@link #getURLPattern()} method will return the supplied target URL,
 * and {@link #getJSP()} method will return the "cleansed" URL.</p>
 *
 * @since 2.4.22
 */
public final class RedirectCommand extends AbstractCommand {

    public static final Command REDIRECT = new RedirectCommand( "", "%u%n", null, null );

    /**
     * Constructs a new Command with a specified wiki context, URL pattern, type, and content template. The WikiPage for this action is
     * initialized to <code>null</code>.
     *
     * @param requestContext the request context
     * @param urlPattern the URL pattern
     * @param contentTemplate the content template; may be <code>null</code>
     * @param target the target of the command
     * @throws IllegalArgumentException if the request content, URL pattern, or type is <code>null</code>
     */
    private RedirectCommand( final String requestContext, final String urlPattern, final String contentTemplate, final String target ) {
        super( requestContext, urlPattern, contentTemplate, target );
    }
    
    /**
     * Creates and returns a targeted Command by combining a URL (as String) with this Command. The supplied <code>target</code>
     * object must be non-<code>null</code> and of type String. The URL passed to the constructor is actually an URL pattern, but it
     * will be converted to a JSP page if it is a partial URL. If it is a full URL (beginning with <code>http://</code> or
     * <code>https://</code>), it will be "passed through" without conversion, and the URL pattern will be <code>null</code>.
     *
     * @param target the object to combine
     * @throws IllegalArgumentException if the target is not of the correct type
     */
    public Command targetedCommand( final Object target ) {
        if ( !( target instanceof String ) ) {
            throw new IllegalArgumentException( "Target must non-null and of type String." );
        }
        return new RedirectCommand( getRequestContext(), ( String )target, getContentTemplate(), ( String )target );
    }
    
    /**
     * @see org.apache.wiki.ui.Command#getName()
     */
    public String getName() {
        final Object target = getTarget();
        if ( target == null ) {
            return getJSPFriendlyName();
        }
        return target.toString();
    }

    /**
     * No-op; always returns <code>null</code>.
     *
     * @see org.apache.wiki.ui.Command#requiredPermission()
     */
    public Permission requiredPermission() {
        return null;
    }

}
