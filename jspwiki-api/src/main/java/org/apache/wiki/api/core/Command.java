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
package org.apache.wiki.api.core;

import java.security.Permission;


/**
 * <p>Represents a logical "unit of work" that includes a request context, JSP, URLPattern, content template and (optionally) a target and
 * required security permission. Examples of Commands include "view a page," "create a group," and "edit user preferences." </p>
 * <p> Commands come in two flavors: "static" and "targeted." </p>
 * <ul>
 * <li><strong>Static commands</strong> are exactly what they sound like: static. They are <code>final</code>, threadsafe, and immutable.
 * They have no intrinsic idea of the context they are acting in. For example, the static command {@link org.apache.wiki.ui.PageCommand#VIEW} embodies the
 * idea of viewing a page &#8212; but exactly <em>which</em> page is left undefined. Static commands exist so that they can be freely
 * shared and passed around without incurring the penalties of object creation. Static commands are a lot like naked request contexts
 * ("edit", "view", etc.) except that they include additional, essential properties such as the associated URL pattern and content JSP.</li>
 * <li><strong>Targeted commands</strong> "decorate" static commands by scoping a static Command at a specific target such as a WikiPage or
 * GroupPrincipal. Targeted commands are created by calling an existing Command's {@link #targetedCommand(Object)} and supplying the target
 * object. Implementing classes generally require a specific target type. For example, the {@link org.apache.wiki.ui.PageCommand} class requires that the
 * target object be of type {@link org.apache.wiki.api.core.Page}.</li>
 * </ul>
 * <p> Concrete implementations of Command include: </p>
 * <ul>
 * <li><strong>PageCommand</strong>: commands for editing, renaming, and viewing pages</li>
 * <li><strong>GroupCommand</strong>: commands for viewing, editing and
 * deleting wiki groups</li>
 * <li><strong>WikiCommand</strong>: commands for wiki-wide operations such as
 * creating groups, editing preferences and profiles, and logging in/out</li>
 * <li><strong>RedirectCommand</strong>: commands for redirections to off-site
 * special pages</li>
 * </ul>
 * <p>
 * For a given targeted Command, its {@link #getTarget()} method will return a non-<code>null</code> value. In addition, its
 * {@link #requiredPermission()} method will generally also return a non-<code>null</code> value. It is each implementation's responsibility
 * to construct and store the correct Permission for a given Command and Target. For example, when PageCommand.VIEW is targeted at the
 * WikiPage <code>Main</code>, the Command's associated permission is <code>PagePermission "<em>theWiki</em>:Main", "view".</code></p>
 * <p>Static Commands, and targeted Commands that do not require specific permissions to execute, return a <code>null</code> result for
 * {@link #requiredPermission()}.</p>
 * @since 2.4.22
 */
public interface Command {

    /**
     * Creates and returns a targeted Command by combining a target, such as a WikiPage or GroupPrincipal into the existing Command.
     * Subclasses should check to make sure the supplied <code>target</code> object is of the correct type. This method is guaranteed
     * to return a non-<code>null</code> Command (unless the target is an incorrect type).
     *
     * @param target the object to combine, such as a GroupPrincipal or WikiPage
     * @return the new, targeted Command
     * @throws IllegalArgumentException if the target is not of the correct type
     */
    Command targetedCommand( Object target );

    /**
     * Returns the content template associated with a Command, such as <code>PreferencesContent.jsp</code>. For Commands that are not
     * page-related, this method will always return <code>null</code>. <em>Calling methods should always check to see if the result
     * of this method is <code>null</code></em>.
     *
     * @return the content template
     */
    String getContentTemplate();

    /**
     * Returns the JSP associated with the Command. The JSP is a "local" JSP within the JSPWiki webapp; it is not a general HTTP URL.
     * If it exists, the JSP will be expressed relative to the webapp root, without a leading slash. This method is guaranteed to return
     * a non-<code>null</code> result, although in some cases the result may be an empty string.
     *
     * @return the JSP or url associated with the wiki command
     */
    String getJSP();

    /**
     * Returns the human-friendly name for this command.
     *
     * @return the name
     */
    String getName();

    /**
     * Returns the name of the request context (e.g. VIEW) associated with this Command. This method is guaranteed to return a
     * non-<code>null</code> String.
     *
     * @return the request context
     */
    String getRequestContext();

    /**
     * Returns the Permission required to successfully execute this Command. If no Permission is requred, this method returns
     * <code>null</code>. For example, the static command {@link org.apache.wiki.ui.PageCommand#VIEW} doesn't require a permission because
     * it isn't referring to a particular WikiPage. However, if this command targets a WikiPage called <code>Main</code>(via
     * {@link org.apache.wiki.ui.PageCommand#targetedCommand(Object)}, the resulting Command would require the permission
     * <code>PagePermission "<em>yourWiki</em>:Main", "view"</code>.
     *
     * @return the required permission, or <code>null</code> if not required
     */
    Permission requiredPermission();

    /**
     * Returns the target associated with a Command, if it was created with one. Commands created with {@link #targetedCommand(Object)} will
     * <em>always</em> return a non-<code>null</code> object. <em>Calling methods should always check to see if the result of this method
     * is <code>null</code></em>.
     *
     * @return the wiki page
     */
    Object getTarget();

    /**
     * Returns the URL pattern associated with this Command. This method is guaranteed to return a non-<code>null</code> String.
     *
     * @return the URL pattern
     */
    String getURLPattern();

}
