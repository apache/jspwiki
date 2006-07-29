package com.ecyrd.jspwiki.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiProvider;
import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.url.URLConstructor;

/**
 * <p>Resolves special pages, JSPs and Commands on behalf of a
 * WikiEngine. CommandResolver will automatically resolve page names
 * with singular/plural variants. It can also detect the correct Command 
 * based on parameters supplied in an HTTP request, or due to the
 * JSP being accessed.</p>
 * <p>
 * <p>CommandResolver's static {@link #findCommand(String)} method is
 * the simplest method; it looks up and returns the Command matching 
 * a supplied wiki context. For example, looking up the request context 
 * <code>view</code> returns {@link PageCommand#VIEW}. Use this method
 * to obtain static Command instances that aren't targeted at a particular
 * page or group.</p>
 * <p>For more complex lookups in which the caller supplies an HTTP 
 * request, {@link #findCommand(HttpServletRequest, String)} will
 * look up and return the correct Command. The String parameter
 * <code>defaultContext</code> supplies the request context to use
 * if it cannot be detected. However, note that the default wiki 
 * context may be over-ridden if the request was for a "special page."</p>
 * <p>For example, suppose the WikiEngine's properties specify a 
 * special page called <code>UserPrefs</code>
 * that redirects to <code>UserPreferences.jsp</code>. The ordinary
 * lookup method {@linkplain #findCommand(String)} using a supplied
 * context <code>view</code> would return {@link PageCommand#VIEW}. But
 * the {@linkplain #findCommand(HttpServletRequest, String)} method, 
 * when passed the same context (<code>view</code>) and an HTTP request
 * containing the page parameter value <code>UserPrefs</code>, 
 * will instead return {@link WikiCommand#PREFS}.</p>
 * @author Andrew Jaquith
 * @version $Revision: 1.3 $ $Date: 2006-07-29 19:31:11 $
 * @since 2.4.22
 */
public final class CommandResolver
{
    /** Prefix in jspwiki.properties signifying special page keys. */
    private static final String PROP_SPECIALPAGE = "jspwiki.specialPage.";

    /** Private map with request contexts as keys, Commands as values */
    private static final Map    c_contexts;

    /** Private map with JSPs as keys, Commands as values */
    private static final Map    c_jsps;

    /** Store the JSP-to-Command and context-to-Command mappings */
    static
    {
        c_contexts = new HashMap();
        c_jsps = new HashMap();
        Command[] commands = AbstractCommand.allCommands();
        for( int i = 0; i < commands.length; i++ )
        {
            c_jsps.put( commands[i].getJSP(), commands[i] );
            c_contexts.put( commands[i].getRequestContext(), commands[i] );
        }
    }

    private final Logger        log = Logger.getLogger( CommandResolver.class );

    private final WikiEngine    m_engine;

    /** If true, we'll also consider english plurals (+s) a match. */
    private final boolean       m_matchEnglishPlurals;

    /** Stores special page names as keys, and Commands as values. */
    private final Map           m_specialPages;

    /**
     * Constructs a CommandResolver for a given WikiEngine. This constructor
     * will extract the special page references for this wiki and store them in
     * a cache used for resolution.
     * @param engine the wiki engine
     * @param properties the properties used to initialize the wiki
     */
    public CommandResolver( WikiEngine engine, Properties properties )
    {
        m_engine = engine;
        m_specialPages = new HashMap();

        // Skim through the properties and look for anything with
        // the "special page" prefix. Create maps that allow us
        // look up the correct Command based on special page name.
        // If a matching command isn't found, create a RedirectCommand.
        for( Iterator i = properties.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            String key = (String) entry.getKey();
            if ( key.startsWith( PROP_SPECIALPAGE ) )
            {
                String specialPage = key.substring( PROP_SPECIALPAGE.length() );
                String jsp = (String) entry.getValue();
                if ( specialPage != null && jsp != null )
                {
                    specialPage = specialPage.trim();
                    jsp = jsp.trim();
                    Command command = (Command) c_jsps.get( jsp );
                    if ( command == null )
                    {
                        Command redirect = RedirectCommand.REDIRECT;
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
     * Attempts to locate a wiki command for a supplied request context.
     * The resolution technique is simple: we examine the list of
     * Commands returned by {@link AbstractCommand#allCommands()} and
     * return the one whose <code>requestContext</code> matches the
     * supplied context.
     * @param context the request context
     * @return the resolved context
     * @throws IllegalArgumentException if the supplied request context does not
     *             resolve to a known Command
     */
    public static Command findCommand( String context )
    {
        Command command = (Command) c_contexts.get( context );
        if ( command == null )
        {
            throw new IllegalArgumentException( "Unsupported wiki context: " + context + "." );
        }
        return command;
    }

    /**
     * <p>
     * Attempts to locate a Command for a supplied wiki context and HTTP
     * request, incorporating the correct WikiPage into the command if reqiured.
     * This method will first determine what page the user requested by
     * delegating to {@link #extractPageFromParameter(String, HttpServletRequest)}. If
     * this page equates to a special page, we return the Command
     * corresponding to that page. Otherwise, this method simply returns the
     * Command for the supplied request context.
     * </p>
     * <p>
     * The reason this method attempts to resolve against special pages is
     * because some of them resolve to contexts that may be different from the
     * one supplied. For example, a VIEW request context for the special page
     * "UserPreferences" should return a PREFS context instead.
     * </p>
     * <p>
     * When the caller supplies a request context and HTTP request that
     * specifies an actual wiki page (rather than a special page), this method
     * will return a "targeted" Command that includes the resolved WikiPage
     * as the target. (See {@link #resolvePage(HttpServletRequest, String)}
     * for the resolution algorithm). Specifically, the Command will 
     * return a non-<code>null</code> value for its {@link AbstractCommand#getTarget()} method.
     * </p>
     * @param request the HTTP request; if <code>null</code>, delegates
     * to {@link #findCommand(String)}
     * @param defaultContext the request context to use by default
     * @return the resolved wiki command
     */
    public final Command findCommand( HttpServletRequest request, String defaultContext )
    {
        // Corner case if request is null
        if ( request == null )
        {
            return findCommand( defaultContext );
        }
        
        Command command = null;
        
        // Determine the name of the page (which may be null)
        String pageName = extractPageFromParameter( defaultContext, request );

        // Can we find a special-page command matching the extracted page?
        if ( pageName != null )
        {
            command = (AbstractCommand) m_specialPages.get( pageName );
        }

        // If we haven't found a matching command yet, extract the JSP path
        // and compare to our list of special pages
        if ( command == null )
        {
            command = extractCommandFromPath( request );
            
            // Otherwise: use the default context
            if ( command == null )
            {
                command = (AbstractCommand) c_contexts.get( defaultContext );
                if ( command == null )
                {
                    throw new IllegalArgumentException( "Wiki context " + defaultContext + " is illegal." );
                }
            }
        }
        
        // These next blocks handle targeting requirements
        
        // If we were passed a page parameter, try to resolve it
        if ( command instanceof PageCommand && pageName != null )
        {
            // If there's a matching WikiPage, "wrap" the command
            WikiPage page = resolvePage( request, pageName );
            if ( page != null )
            {
                return command.targetedCommand( page );
            }
        }
        
        // If "create group" command, target this wiki
        String wiki = m_engine.getApplicationName();
        if ( WikiCommand.CREATE_GROUP.equals( command ) )
        {
            return WikiCommand.CREATE_GROUP.targetedCommand( wiki );
        }
        
        // If group command, see if we were passed a group name
        if ( command instanceof GroupCommand )
        {
            String groupName = request.getParameter( "group" );
            if ( groupName != null && groupName.length() > 0 )
            {
                GroupPrincipal group = new GroupPrincipal( wiki, groupName );
                return command.targetedCommand( group );
            }
        }
        
        // No page provided; return an "ordinary" command
        return command;
    }

    /**
     * <p>
     * Returns the correct page name, or <code>null</code>, if no such page can be found.
     * Aliases are considered.
     * </p>
     * <p>
     * In some cases, page names can refer to other pages. For example, when you
     * have matchEnglishPlurals set, then a page name "Foobars" will be
     * transformed into "Foobar", should a page "Foobars" not exist, but the
     * page "Foobar" would. This method gives you the correct page name to refer
     * to.
     * </p>
     * <p>
     * This facility can also be used to rewrite any page name, for example, by
     * using aliases. It can also be used to check the existence of any page.
     * </p>
     * @since 2.4.20
     * @param page the page name.
     * @return The rewritten page name, or <code>null</code>, if the page does not exist.
     */
    public final String getFinalPageName( String page ) throws ProviderException
    {
        boolean isThere = simplePageExists( page );

        if ( !isThere && m_matchEnglishPlurals )
        {
            if ( page.endsWith( "s" ) )
            {
                page = page.substring( 0, page.length() - 1 );
            }
            else
            {
                page += "s";
            }

            isThere = simplePageExists( page );
        }

        return isThere ? page : null;
    }

    /**
     * <p>
     * If the page is a special page, this method returns a direct URL to that
     * page; otherwise, it returns <code>null</code>.
     * </p>
     * <p>
     * Special pages are non-existant references to other pages. For example,
     * you could define a special page reference "RecentChanges" which would
     * always be redirected to "RecentChanges.jsp" instead of trying to find a
     * Wiki page called "RecentChanges".
     * </p>
     */
    public final String getSpecialPageReference( String page )
    {
        Command command = (Command) m_specialPages.get( page );

        if ( command != null )
        {
            return m_engine.getURLConstructor()
                    .makeURL( command.getRequestContext(), command.getURLPattern(), true, null );
        }

        return null;
    }

    /**
     * Extracts a Command based on the JSP path of an HTTP request.
     * If the JSP requested matches a Command's <code>getJSP()</code>
     * value, that Command is returned.
     * @param request the HTTP request
     * @return the resolved Command, or <code>null</code> if not found
     */
    protected final Command extractCommandFromPath( HttpServletRequest request )
    {
        String jsp = request.getServletPath();

        // Take everything to right of initial / and left of # or ?
        int hashMark = jsp.indexOf( '#' );
        if ( hashMark != -1 )
        {
            jsp = jsp.substring( 0, hashMark );
        }
        int questionMark = jsp.indexOf( '?' );
        if ( questionMark != -1 )
        {
            jsp = jsp.substring( 0, questionMark );
        }
        if ( jsp.startsWith( "/" ) )
        {
            jsp = jsp.substring( 1 );
        }
        
        // Find special page reference?
        for( Iterator i = m_specialPages.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            Command specialCommand = (Command) entry.getValue();
            if ( specialCommand.getJSP().equals( jsp ) )
            {
                return specialCommand;
            }
        }
        
        // Still haven't found a matching command? 
        // Ok, see if we match against our standard list of JSPs
        if ( jsp.length() > 0 && c_jsps.containsKey( jsp ) )
        {
            return (Command)c_jsps.get( jsp );
        }
        
        return null;
    }
    
    /**
     * Determines the correct wiki page based on a supplied request context and
     * HTTP request. This method attempts to determine the page requested by a
     * user, taking into acccount special pages. The resolution algorithm will:
     * <ul>
     * <li>Extract the page name from the URL according to the rules for the
     * current {@link URLConstructor}. If a page name was
     * passed in the request, return the correct name after taking into account
     * potential plural matches.</li>
     * <li>If the extracted page name is <code>null</code>, attempt to see
     * if a "special page" was intended by examining the servlet path. For
     * example, the request path "/UserPreferences.jsp" will resolve to
     * "UserPreferences."</li>
     * <li>If neither of these methods work, this method returns 
     * <code>null</code></li>
     * </ul>
     * @param requestContext the request context
     * @param request the HTTP request
     * @return the resolved page name
     */
    protected final String extractPageFromParameter( String requestContext, HttpServletRequest request )
    {
        String page;

        // Extract the page name from the URL directly
        try
        {
            page = m_engine.getURLConstructor().parsePage( requestContext, request, m_engine.getContentEncoding() );
            if ( page != null )
            {
                try
                {
                    // Look for singular/plural variants; if one
                    // not found, take the one the user supplied
                    String finalPage = getFinalPageName( page );
                    if ( finalPage != null )
                    {
                        page = finalPage;
                    }
                }
                catch( ProviderException e )
                {
                    // FIXME: Should not ignore!
                }
                return page;
            }
        }
        catch( IOException e )
        {
            log.error( "Unable to create context", e );
            throw new InternalWikiException( "Big internal booboo, please check logs." );
        }

        // Didn't resolve; return null
        return null;
    }
    
    /**
     * Looks up and returns the correct, versioned WikiPage based on a supplied
     * page name and optional <code>version</code> parameter passed in an HTTP
     * request. If the <code>version</code> parameter does not exist in the
     * request, the latest version is returned.
     * @param request the HTTP request
     * @param page the name of the page to look up; this page <em>must</em> exist
     * @return the wiki page
     */
    protected final WikiPage resolvePage( HttpServletRequest request, String page )
    {
        // See if the user included a version parameter
        WikiPage wikipage;
        int version = WikiProvider.LATEST_VERSION;
        String rev = request.getParameter( "version" );

        if ( rev != null )
        {
            version = Integer.parseInt( rev );
        }

        wikipage = m_engine.getPage( page, version );

        if ( wikipage == null )
        {
            page = MarkupParser.cleanLink( page );
            wikipage = new WikiPage( m_engine, page );
        }
        return wikipage;
    }

    /**
     * Determines whether a "page" exists by examining the list of special pages
     * and querying the page manager.
     * @param page the page to seek
     * @return <code>true</code> if the page exists, <code>false</code>
     *         otherwise
     */
    protected final boolean simplePageExists( String page ) throws ProviderException
    {
        if ( m_specialPages.containsKey( page ) )
        {
            return true;
        }
        return m_engine.getPageManager().pageExists( page );
    }

}
