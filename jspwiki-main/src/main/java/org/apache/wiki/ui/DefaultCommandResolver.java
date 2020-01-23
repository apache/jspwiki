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

import org.apache.log4j.Logger;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.util.TextUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * <p>Default implementation for {@link CommandResolver}</p>
 *
 * @since 2.4.22
 */
public final class DefaultCommandResolver implements CommandResolver {

    /** Private map with request contexts as keys, Commands as values */
    private static final Map< String, Command > CONTEXTS;

    /** Private map with JSPs as keys, Commands as values */
    private static final Map< String, Command > JSPS;

    /* Store the JSP-to-Command and context-to-Command mappings */
    static {
        CONTEXTS = new HashMap<>();
        JSPS = new HashMap<>();
        final Command[] commands = AbstractCommand.allCommands();
        for( final Command command : commands ) {
            JSPS.put( command.getJSP(), command );
            CONTEXTS.put( command.getRequestContext(), command );
        }
    }

    private static final Logger LOG = Logger.getLogger( DefaultCommandResolver.class );

    private final WikiEngine m_engine;

    /** If true, we'll also consider english plurals (+s) a match. */
    private final boolean m_matchEnglishPlurals;

    /** Stores special page names as keys, and Commands as values. */
    private final Map<String, Command> m_specialPages;

    /**
     * Constructs a CommandResolver for a given WikiEngine. This constructor will extract the special page references for this wiki and
     * store them in a cache used for resolution.
     *
     * @param engine the wiki engine
     * @param properties the properties used to initialize the wiki
     */
    public DefaultCommandResolver( final WikiEngine engine, final Properties properties ) {
        m_engine = engine;
        m_specialPages = new HashMap<>();

        // Skim through the properties and look for anything with the "special page" prefix. Create maps that allow us look up
        // the correct Command based on special page name. If a matching command isn't found, create a RedirectCommand.
        for( final String key : properties.stringPropertyNames() ) {
            if ( key.startsWith( PROP_SPECIALPAGE ) ) {
                String specialPage = key.substring( PROP_SPECIALPAGE.length() );
                String jsp = properties.getProperty( key );
                if ( jsp != null ) {
                    specialPage = specialPage.trim();
                    jsp = jsp.trim();
                    Command command = JSPS.get( jsp );
                    if ( command == null ) {
                        final Command redirect = RedirectCommand.REDIRECT;
                        command = redirect.targetedCommand( jsp );
                    }
                    m_specialPages.put( specialPage, command );
                }
            }
        }

        // Do we match plurals?
        m_matchEnglishPlurals = TextUtil.getBooleanProperty( properties, WikiEngine.PROP_MATCHPLURALS, true );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Command findCommand( final HttpServletRequest request, final String defaultContext ) {
        // Corner case if request is null
        if ( request == null ) {
            return CommandResolver.findCommand( defaultContext );
        }

        Command command = null;

        // Determine the name of the page (which may be null)
        String pageName = extractPageFromParameter( defaultContext, request );

        // Can we find a special-page command matching the extracted page?
        if ( pageName != null ) {
            command = m_specialPages.get( pageName );
        }

        // If we haven't found a matching command yet, extract the JSP path and compare to our list of special pages
        if ( command == null ) {
            command = extractCommandFromPath( request );

            // Otherwise: use the default context
            if ( command == null ) {
                command = CONTEXTS.get( defaultContext );
                if ( command == null ) {
                    throw new IllegalArgumentException( "Wiki context " + defaultContext + " is illegal." );
                }
            }
        }

        // For PageCommand.VIEW, default to front page if a page wasn't supplied
        if( PageCommand.VIEW.equals( command ) && pageName == null ) {
            pageName = m_engine.getFrontPage();
        }

        // These next blocks handle targeting requirements

        // If we were passed a page parameter, try to resolve it
        if ( command instanceof PageCommand && pageName != null ) {
            // If there's a matching WikiPage, "wrap" the command
            final WikiPage page = resolvePage( request, pageName );
            return command.targetedCommand( page );
        }

        // If "create group" command, target this wiki
        final String wiki = m_engine.getApplicationName();
        if ( WikiCommand.CREATE_GROUP.equals( command ) ) {
            return WikiCommand.CREATE_GROUP.targetedCommand( wiki );
        }

        // If group command, see if we were passed a group name
        if( command instanceof GroupCommand ) {
            String groupName = request.getParameter( "group" );
            groupName = TextUtil.replaceEntities( groupName );
            if ( groupName != null && groupName.length() > 0 ) {
                final GroupPrincipal group = new GroupPrincipal( groupName );
                return command.targetedCommand( group );
            }
        }

        // No page provided; return an "ordinary" command
        return command;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFinalPageName( final String page ) throws ProviderException {
        boolean isThere = simplePageExists( page );
        String  finalName = page;

        if ( !isThere && m_matchEnglishPlurals ) {
            if ( page.endsWith( "s" ) ) {
                finalName = page.substring( 0, page.length() - 1 );
            } else {
                finalName += "s";
            }

            isThere = simplePageExists( finalName );
        }

        if( !isThere ) {
            finalName = MarkupParser.wikifyLink( page );
            isThere = simplePageExists(finalName);

            if( !isThere && m_matchEnglishPlurals ) {
                if( finalName.endsWith( "s" ) ) {
                    finalName = finalName.substring( 0, finalName.length() - 1 );
                } else {
                    finalName += "s";
                }

                isThere = simplePageExists( finalName );
            }
        }

        return isThere ? finalName : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSpecialPageReference( final String page ) {
        final Command command = m_specialPages.get( page );
        if ( command != null ) {
            return m_engine.getURLConstructor().makeURL( command.getRequestContext(), command.getURLPattern(), null );
        }

        return null;
    }

    /**
     * Extracts a Command based on the JSP path of an HTTP request. If the JSP requested matches a Command's <code>getJSP()</code>
     * value, that Command is returned.
     *
     * @param request the HTTP request
     * @return the resolved Command, or <code>null</code> if not found
     */
    protected Command extractCommandFromPath( final HttpServletRequest request ) {
        String jsp = request.getServletPath();

        // Take everything to right of initial / and left of # or ?
        final int hashMark = jsp.indexOf( '#' );
        if ( hashMark != -1 ) {
            jsp = jsp.substring( 0, hashMark );
        }
        final int questionMark = jsp.indexOf( '?' );
        if ( questionMark != -1 ) {
            jsp = jsp.substring( 0, questionMark );
        }
        if ( jsp.startsWith( "/" ) ) {
            jsp = jsp.substring( 1 );
        }

        // Find special page reference?
        for( final Map.Entry< String, Command > entry : m_specialPages.entrySet() ) {
            final Command specialCommand = entry.getValue();
            if( specialCommand.getJSP().equals( jsp ) ) {
                return specialCommand;
            }
        }

        // Still haven't found a matching command? Ok, see if we match against our standard list of JSPs
        if ( jsp.length() > 0 && JSPS.containsKey( jsp ) ) {
            return JSPS.get( jsp );
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String extractPageFromParameter( final String requestContext, final HttpServletRequest request ) {
        // Extract the page name from the URL directly
        try {
            String page = m_engine.getURLConstructor().parsePage( requestContext, request, m_engine.getContentEncoding() );
            if ( page != null ) {
                try {
                    // Look for singular/plural variants; if one not found, take the one the user supplied
                    final String finalPage = getFinalPageName( page );
                    if ( finalPage != null ) {
                        page = finalPage;
                    }
                } catch( final ProviderException e ) {
                    // FIXME: Should not ignore!
                }
                return page;
            }
        } catch( final IOException e ) {
            LOG.error( "Unable to create context", e );
            throw new InternalWikiException( "Big internal booboo, please check logs." , e );
        }

        // Didn't resolve; return null
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WikiPage resolvePage( final HttpServletRequest request, String page ) {
        // See if the user included a version parameter
        int version = WikiProvider.LATEST_VERSION;
        final String rev = request.getParameter( "version" );
        if ( rev != null ) {
            try {
                version = Integer.parseInt( rev );
            } catch( final NumberFormatException e ) {
                // This happens a lot with bots or other guys who are trying to test if we are vulnerable to e.g. XSS attacks.  We catch
                // it here so that the admin does not get tons of mail.
            }
        }

        WikiPage wikipage = m_engine.getPageManager().getPage( page, version );
        if ( wikipage == null ) {
            page = MarkupParser.cleanLink( page );
            wikipage = new WikiPage( m_engine, page );
        }
        return wikipage;
    }

    /**
     * Determines whether a "page" exists by examining the list of special pages and querying the page manager.
     *
     * @param page the page to seek
     * @return <code>true</code> if the page exists, <code>false</code> otherwise
     * @throws ProviderException if the underlyng page provider that locates pages
     * throws an exception
     */
    protected boolean simplePageExists( final String page ) throws ProviderException {
        if ( m_specialPages.containsKey( page ) ) {
            return true;
        }
        return m_engine.getPageManager().pageExists( page );
    }

}
