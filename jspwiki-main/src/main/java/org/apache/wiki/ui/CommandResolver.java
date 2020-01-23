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

import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;


/**
 * <p>Resolves special pages, JSPs and Commands on behalf of a WikiEngine. CommandResolver will automatically resolve page names
 * with singular/plural variants. It can also detect the correct Command based on parameters supplied in an HTTP request, or due to the
 * JSP being accessed.</p>
 * <p>
 * <p>CommandResolver's static {@link #findCommand(String)} method is the simplest method; it looks up and returns the Command matching
 * a supplied wiki context. For example, looking up the request context <code>view</code> returns {@link PageCommand#VIEW}. Use this method
 * to obtain static Command instances that aren't targeted at a particular page or group.</p>
 * <p>For more complex lookups in which the caller supplies an HTTP request, {@link #findCommand(HttpServletRequest, String)} will
 * look up and return the correct Command. The String parameter <code>defaultContext</code> supplies the request context to use
 * if it cannot be detected. However, note that the default wiki context may be overridden if the request was for a "special page."</p>
 * <p>For example, suppose the WikiEngine's properties specify a special page called <code>UserPrefs</code> that redirects to
 * <code>UserPreferences.jsp</code>. The ordinary lookup method {@linkplain #findCommand(String)} using a supplied context <code>view</code>
 * would return {@link PageCommand#VIEW}. But the {@linkplain #findCommand(HttpServletRequest, String)} method, when passed the same context
 * (<code>view</code>) and an HTTP request containing the page parameter value <code>UserPrefs</code>, will instead return
 * {@link WikiCommand#PREFS}.</p>
 *
 * @since 2.4.22
 */
public interface CommandResolver {

    /** Prefix in jspwiki.properties signifying special page keys. */
    String PROP_SPECIALPAGE = "jspwiki.specialPage.";

    /**
     * Attempts to locate a wiki command for a supplied request context. The resolution technique is simple: we examine the list of
     * Commands returned by {@link AbstractCommand#allCommands()} and return the one whose <code>requestContext</code> matches the
     * supplied context. If the supplied context does not resolve to a known Command, this method throws an {@link IllegalArgumentException}.
     *
     * @param context the request context
     * @return the resolved context
     */
    static Command findCommand( final String context ) {
        return Arrays.stream( AbstractCommand.allCommands() )
                     .filter( c -> c.getRequestContext().equals( context ) )
                     .findFirst()
                     .orElseThrow( () -> new IllegalArgumentException( "Unsupported wiki context: " + context + "." ) );
    }

    /**
     * <p>Attempts to locate a Command for a supplied wiki context and HTTP request, incorporating the correct WikiPage into the command
     * if required. This method will first determine what page the user requested by delegating to {@link #extractPageFromParameter(String, HttpServletRequest)}.
     * If this page equates to a special page, we return the Command corresponding to that page. Otherwise, this method simply returns the
     * Command for the supplied request context.</p>
     * <p>The reason this method attempts to resolve against special pages is because some of them resolve to contexts that may be different
     * from the one supplied. For example, a VIEW request context for the special page "UserPreferences" should return a PREFS context instead.</p>
     * <p>When the caller supplies a request context and HTTP request that specifies an actual wiki page (rather than a special page),
     * this method will return a "targeted" Command that includes the resolved WikiPage as the target. (See {@link #resolvePage(HttpServletRequest, String)}
     * for the resolution algorithm). Specifically, the Command will return a non-<code>null</code> value for
     * its {@link AbstractCommand#getTarget()} method.</p>
     * <p><em>Note: if this method determines that the Command is the VIEW PageCommand, then the Command returned will always be targeted to
     * the front page.</em></p>
     *
     * @param request the HTTP request; if <code>null</code>, delegates to {@link #findCommand(String)}
     * @param defaultContext the request context to use by default
     * @return the resolved wiki command
     */
    Command findCommand( HttpServletRequest request, String defaultContext );

    /**
     * <p>Returns the correct page name, or <code>null</code>, if no such page can be found. Aliases are considered.</p>
     * <p>In some cases, page names can refer to other pages. For example, when you have matchEnglishPlurals set, then a page name
     * "Foobars" will be transformed into "Foobar", should a page "Foobars" not exist, but the page "Foobar" would. This method gives
     * you the correct page name to refer to. </p>
     * <p>This facility can also be used to rewrite any page name, for example, by using aliases. It can also be used to check the
     * existence of any page.</p>
     *
     * @since 2.4.20
     * @param page the page name.
     * @return The rewritten page name, or <code>null</code>, if the page does not exist.
     * @throws ProviderException if the underlyng page provider that locates pages throws an exception
     */
    String getFinalPageName( String page ) throws ProviderException;

    /**
     * <p>If the page is a special page, this method returns a direct URL to that page; otherwise, it returns <code>null</code>.</p>
     * <p>Special pages are non-existant references to other pages. For example, you could define a special page reference "RecentChanges"
     * which would always be redirected to "RecentChanges.jsp" instead of trying to find a Wiki page called "RecentChanges".</p>
     *
     * @param page the page name ro search for
     * @return the URL of the special page, if the supplied page is one, or <code>null</code>
     */
    String getSpecialPageReference( final String page );

    /**
     * Determines the correct wiki page based on a supplied request context and HTTP request. This method attempts to determine the page
     * requested by a user, taking into acccount special pages. The resolution algorithm will:
     * <ul>
     * <li>Extract the page name from the URL according to the rules for the current {@link org.apache.wiki.url.URLConstructor}. If a
     * page name was passed in the request, return the correct name after taking into account potential plural matches.</li>
     * <li>If the extracted page name is <code>null</code>, attempt to see if a "special page" was intended by examining the servlet path.
     * For example, the request path "/UserPreferences.jsp" will resolve to "UserPreferences."</li>
     * <li>If neither of these methods work, this method returns <code>null</code></li>
     * </ul>
     *
     * @param requestContext the request context
     * @param request the HTTP request
     * @return the resolved page name
     */
    String extractPageFromParameter( String requestContext, HttpServletRequest request );

    /**
     * Looks up and returns the correct, versioned WikiPage based on a supplied page name and optional <code>version</code> parameter
     * passed in an HTTP request. If the <code>version</code> parameter does not exist in the request, the latest version is returned.
     *
     * @param request the HTTP request
     * @param page the name of the page to look up; this page <em>must</em> exist
     * @return the wiki page
     */
    WikiPage resolvePage( HttpServletRequest request, String page );

}
