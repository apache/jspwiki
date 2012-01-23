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
package org.apache.wiki.ui;

import java.util.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.StringUtils;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.event.WikiEngineEvent;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.modules.ModuleManager;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.preferences.Preferences;


/**
 * This class takes care of managing JSPWiki templates. This class also provides
 * the ResourceRequest mechanism.
 * 
 * @since 2.1.62
 */
public class TemplateManager extends ModuleManager
{
    private static final String SKIN_DIR = "skins";

    /**
     * Attribute name for the resource resolver map returned by
     * {@link #getResourceResolver(ServletContext)}. Stored in the
     * servlet context as an attribute.
     */
    private static final String RESOURCE_RESOLVER = "resourceResolver";

    /**
     * Attribute name for the localized JavaScript template strings. Stored in the
     * servlet context as an attribute.
     */
    public static final String TEMPLATE_JAVASCRIPT_STRINGS = "templateJsStrings";

    /** The default directory for template resources. Value is {@value}. */
    public static final String TEMPLATE_DIR = "templates";

    /** The name of the default template. Value is {@value}. */
    public static final String DEFAULT_TEMPLATE = "default";

    protected static final Logger log = LoggerFactory.getLogger( TemplateManager.class );

    /**
     * <p>Resolves requests for resources relative to the
     * <code>templates/<var>template</var></code> path to the actual resources,
     * where <var>template</var> is the configured template returned by
     * {@link WikiEngine#getTemplateDir()}, for example <code>default</code>.
     * Each key refers to a resource path that a JSP might request; each value
     * is the actual path to the resolved resource, relative to the webapp
     * context. For example, if the current template is <code>modern</code>,
     * the key <code>FindContent.jsp</code> might return the value
     * <code>/templates/modern/FindContent.jsp</code> if the resource exists
     * in the template. If not, the value will be
     * <code>/templates/default/FindContent.jsp</code>. It is also possible
     * for certain keys to return <code>null</code>. The map itself is
     * immutable.
     * </p>
     * <p>The resource resolver is guaranteed to initialize,
     * even if the WikiEngine cannot initialize for some reason.
     * If the WikiEngine does not initialize, the default template
     * {@link #DEFAULT_TEMPLATE} will be used for all resource requests.</p>
     * <p>Note that because the resource resolver is stashed as a ServletContext
     * attribute, it is (effectively) lazily initialized once per ServletContext.
     * @param context the servlet context
     * @return the unmodifiable map
     */
    @SuppressWarnings("unchecked")
    public static Map<String,String> getResourceResolver( ServletContext context )
    {
        Map<String,String> resolver = (Map<String,String>)context.getAttribute( RESOURCE_RESOLVER );
        if ( resolver == null )
        {
            // Get the WikiEngine template (or use the default if not available)
            String template = null;
            try
            {
                WikiEngine engine = WikiEngine.getInstance( context, null );
                template = engine.getTemplateDir();
            }
            catch ( Exception e )
            {
                // WikiEngine didn't init!
            }
            if ( template == null )
            {
                template = DEFAULT_TEMPLATE;
            }
            
            // Add all of the resources the template contains
            resolver = new HashMap<String,String>();
            addResources( context, resolver, "/" + TEMPLATE_DIR + "/" + template + "/", null );
            
            // Add resources the template does not contain, but default does
            addResources( context, resolver, "/" + TEMPLATE_DIR + "/" + DEFAULT_TEMPLATE + "/", null );
            resolver = Collections.unmodifiableMap( resolver );
            context.setAttribute( RESOURCE_RESOLVER, resolver );
        }
        return resolver;
    }
    
    /**
     * Returns all message keys and values for all keys prefixed with
     * {@code javascript} from the {@code templates/default*.properties}
     * resource bundle files. The return value is a JavaScript snippet that
     * defines the LocalizedStings array. This method does not depend on
     * WikiEngine initialization. Assuming that the resource bundle is
     * available on the classpath, it will return non-null String even if
     * the WikiEngine does not initialize.
     * 
     * @param session the HTTP session
     * @param locale the Locale for which localized strings are sought
     * @return JavaScript snippet which defines the LocalizedStrings array
    */
    @SuppressWarnings("unchecked")
    public static String getTemplateJSStrings( HttpSession session, Locale locale )
    {
        // If not Locale we support, bail now
        if ( !Preferences.AVAILABLE_LOCALES.containsKey( locale ) )
        {
            // This is probably not the way we should do it.
            return "";
        }
        
        // Retrieve the ServletContext stash
        ServletContext context = session.getServletContext();
        Map<Locale,String> templateStrings = (Map<Locale,String>)context.getAttribute( TEMPLATE_JAVASCRIPT_STRINGS );
        if ( templateStrings == null )
        {
            templateStrings = new HashMap<Locale,String>();
            context.setAttribute( TEMPLATE_JAVASCRIPT_STRINGS, templateStrings );
        }
        
        // Retrieve the JavaScript string for the Locale we want
        String templateString = templateStrings.get( locale );
        if ( templateString == null )
        {
            // Not built yet; go do that now/
            StringBuilder sb = new StringBuilder();
            sb.append( "var LocalizedStrings = {\n" );
            ResourceBundle rb = ResourceBundle.getBundle( InternationalizationManager.TEMPLATES_BUNDLE, locale );

            boolean first = true;
            for( Enumeration<String> en = rb.getKeys(); en.hasMoreElements(); )
            {
                String key = en.nextElement();
                if( key.startsWith( "javascript" ) )
                {
                    if( first )
                    {
                        first = false;
                    }
                    else
                    {
                        sb.append( ",\n" );
                    }
                    sb.append( "\"" + key + "\":\"" + rb.getString( key ) + "\"" );
                }
            }
            sb.append( "\n};\n" );
            templateString = sb.toString();
            templateStrings.put( locale, templateString );
        }
        return templateString;
    }
    
    /**
     * Adds all of the resources under a specified path prefix to the
     * resource resolver map, with the "short name" of the path as the
     * key, and the full path as the value. The short name is the portion
     * of the path after the prefix. If a resource with that short name
     * has already been added to the resource map, it will not be added
     * again. Any resources ending in {@code /} (i.e., a directory path)
     * will be processed recursively.
     * @param context the servlet context
     * @param resolver the resource resolver map
     * @param prefix the path prefix that the search initiates from
     * @param dir the directory to search relative to the path prefix. If not
     * supplied, the path prefix directory itself will be searched
     */
    @SuppressWarnings("unchecked")
    private static void addResources( ServletContext context, Map<String,String> resolver, String prefix, String dir )
    {
        String searchPath = dir == null ? prefix : prefix + dir;
        Set<String> resources = context.getResourcePaths( searchPath );
        if ( resources != null )
        {
            for ( String resource : resources )
            {
                String shortName = resource.substring( prefix.length() ); 
                
                // Directory: process these entries too
                if ( shortName.endsWith( "/" ) )
                {
                    addResources( context, resolver, prefix, shortName );
                }

                // Regular resource: add it if we don't have it already
                else
                {
                    boolean alreadyProcessed = resolver.containsKey( shortName );
                    if ( !alreadyProcessed )
                    {
                        resolver.put( shortName, resource );
                    }
                }
            }
        }
    }

    /**
     * Tries to locate a given resource from the template directory, relative to
     * the root of the JSPWiki webapp context (for example, relative to
     * <code>/JSPWiki/</code>). If the given resource is not found at the 
     * supplied path, returns the path to the corresponding resource in the
     * default template path. If the resource does not exist in the
     * default template path either, <code>null</code> is returned.
     * 
     * 
     * @param servletContext the servlet context
     * @param path the path to the resource; for example,
     * {@code /templates/custom/FindContent.jsp} or {@code jspwiki.css}.
     * If the path starts with a slash (/), the resource is looked up
     * relative to the webapp root
     * @return The name of the resource which was found; for example,
     * <code>/templates/custom/FindContent.jsp</code> (if it exists in the
     * <code>custom</code> template directory), or 
     * <code>/templates/default/FindContent.jsp</code> (if not)
     */
    private static String findResource( ServletContext servletContext, String path )
    {
        if( path.charAt( 0 ) == '/' )
        {
            // This is already a full path
            return findResource( servletContext, path );
        }
        return getResourceResolver( servletContext ).get( path );
    }

    /**
     * Returns a property, as defined in the template. The evaluation is lazy,
     * i.e. the properties are not loaded until the template is actually used
     * for the first time.
     */
    /*
     * public String getTemplateProperty( WikiContext context, String key ) {
     * String template = context.getTemplate(); try { Properties props =
     * (Properties)m_propertyCache.getFromCache( template, -1 ); if( props ==
     * null ) { try { props = getTemplateProperties( template );
     * m_propertyCache.putInCache( template, props ); } catch( IOException e ) {
     * log.warn("IO Exception while reading template properties",e); return
     * null; } } return props.getProperty( key ); } catch( NeedsRefreshException
     * ex ) { // FIXME return null; } }
     */
    /**
     * Returns an absolute path to a given template.
     */
    private static final String getPath( String template )
    {
        return "/" + TEMPLATE_DIR + "/" + template + "/";
    }

    private WikiEngine m_engine;

    /**
     * Creates a new TemplateManager. There is typically one manager per engine.
     * 
     * @param engine The owning engine.
     * @param properties The property list used to initialize this.
     */
    public TemplateManager( WikiEngine engine, Properties properties )
    {
        super( engine );
        m_engine = engine;
        getResourceResolver( engine.getServletContext() );
    }

    /**
     * Listens for the WikiEngine shutdown event, and
     * when received, flushes the stashed localized JavaScript strings
     * and the resource resolver.
     * 
     * @param event The wiki event to inspect.
     */
    public void actionPerformed( WikiEvent event )
    {

        if( event instanceof WikiEngineEvent )
        {
            if( event.getType() == WikiEngineEvent.SHUTDOWN )
            {
                if ( m_engine != null )
                {
                    m_engine.getServletContext().removeAttribute( RESOURCE_RESOLVER );
                    m_engine.getServletContext().removeAttribute( TEMPLATE_JAVASCRIPT_STRINGS );
                }
            }
        }
    }

    /**
     * An utility method for finding a JSP page. It searches only under either
     * current context or by the absolute name.
     * 
     * @param pageContext the JSP PageContext
     * @param name The name of the JSP page to look for (e.g {@code Wiki.jsp})
     * @return The context path to the resource
     * @deprecated use {@link #getResourceResolver(ServletContext)} instead
     */
    public String findJSP( PageContext pageContext, String name )
    {
        ServletContext sContext = pageContext.getServletContext();
        return findResource( sContext, name );
    }

    /**
     * Attempts to locate a resource under the given template. If that template
     * does not exist, or the page does not exist under that template, will
     * attempt to locate a similarly named file under the default template.
     * <p>
     * Even though the name suggests only JSP files can be located, but in fact
     * this method can find also other resources than JSP files.
     * 
     * @param pageContext The JSP PageContext
     * @param template From which template we should seek initially?
     * @param name Which resource are we looking for (e.g. "DefaultLayout.jsp")
     * @return path to the JSP page; null, if it was not found.
     * @deprecated use {@link #getResourceResolver(ServletContext)} instead
     */
    public String findJSP( PageContext pageContext, String template, String name )
    {
        if( name == null || template == null )
        {
            log.error( "findJSP() was asked to find a null template or name (" + template + "," + name + ")." + " JSP page '"
                       + ((HttpServletRequest) pageContext.getRequest()).getRequestURI() + "'" );
            throw new InternalWikiException( "Illegal arguments to findJSP(); please check logs." );
        }
        return findResource( pageContext.getServletContext(), name );
    }

    /**
     * Attempts to locate a resource under the given template. This matches the
     * functionality findJSP(), but uses the WikiContext as the argument. If
     * there is no servlet context (i.e. this is embedded), will just simply
     * return a best-guess.
     * <p>
     * This method is typically used to locate any resource, including JSP
     * pages, images, scripts, etc.
     * 
     * @since 2.6
     * @param ctx the wiki context
     * @param template the name of the template to use
     * @param name the name of the resource to fine
     * @return the path to the resource
     * @deprecated use {@link #getResourceResolver(ServletContext)} instead
     */
    public String findResource( WikiContext ctx, String template, String name )
    {
        if( m_engine.getServletContext() != null )
        {
            return findResource( m_engine.getServletContext(), name );
        }

        return getPath( template ) + "/" + name;
    }

    /**
     * Lists the skins available under the current template.
     * Returns an empty Set, if there are no extra skins available.
     * Note that this method does not check whether there is anything
     * actually in the directories, it just lists them.
     * 
     * @return Set of Strings with the skin names.
     * @since 2.3.26
     */
    @SuppressWarnings( "unchecked" )
    public Set<String> listSkins()
    {
        String skinPath = "/" + TEMPLATE_DIR + "/" + m_engine.getTemplateDir() + "/" + SKIN_DIR;
        Set<String> skinSet = m_engine.getServletContext().getResourcePaths( skinPath );
        TreeSet<String> resultSet = new TreeSet<String>();

        if( log.isDebugEnabled() )
            log.debug( "Listings skins from " + skinPath );

        if( skinSet != null )
        {
            String[] skins = {};

            skins = skinSet.toArray( skins );

            for( int i = 0; i < skins.length; i++ )
            {
                String[] s = StringUtils.split( skins[i], "/" );

                if( s.length > 2 && skins[i].endsWith( "/" ) )
                {
                    String skinName = s[s.length - 1];
                    resultSet.add( skinName );
                    if( log.isDebugEnabled() )
                        log.debug( "...adding skin '" + skinName + "'" );
                }
            }
        }

        return resultSet;
    }

    /**
     * Returns an empty collection, since at the moment the TemplateManager does
     * not manage any modules.
     * 
     * @return {@inheritDoc}
     */
    public Collection<WikiModuleInfo> modules()
    {
        return Collections.emptyList();
    }
}
