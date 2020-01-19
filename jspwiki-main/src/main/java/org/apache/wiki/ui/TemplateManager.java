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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.modules.ModuleManager;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.preferences.Preferences.TimeFormat;
import org.apache.wiki.util.ClassUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;


/**
 *  This class takes care of managing JSPWiki templates.  This class also provides the ResourceRequest mechanism.
 *
 *  @since 2.1.62
 */
public class TemplateManager extends ModuleManager {

    private static final String SKIN_DIRECTORY = "skins";

    /** Requests a JavaScript function to be called during window.onload. Value is {@value}. */
    public static final String RESOURCE_JSFUNCTION = "jsfunction";

    /** Requests a JavaScript associative array with all localized strings. */
    public static final String RESOURCE_JSLOCALIZEDSTRINGS = "jslocalizedstrings";

    /** Requests a stylesheet to be inserted. Value is {@value}. */
    public static final String RESOURCE_STYLESHEET = "stylesheet";

    /** Requests a script to be loaded. Value is {@value}. */
    public static final String RESOURCE_SCRIPT = "script";

    /** Requests inlined CSS. Value is {@value}. */
    public static final String RESOURCE_INLINECSS = "inlinecss";

    /** The default directory for the properties. Value is {@value}. */
    public static final String DIRECTORY = "templates";

    /** The name of the default template. Value is {@value}. */
    public static final String DEFAULT_TEMPLATE = "default";

    /** Name of the file that contains the properties. */
    public static final String PROPERTYFILE = "template.properties";

    /** Location of I18N Resource bundles, and path prefix and suffixes */
    public static final String I18NRESOURCE_PREFIX = "templates/default_";

    public static final String I18NRESOURCE_SUFFIX = ".properties";

    /** The default (en) RESOURCE name and id. */
    public static final String I18NRESOURCE_EN = "templates/default.properties";
    public static final String I18NRESOURCE_EN_ID = "en";

    /** I18N string to mark the default locale */
    public static final String I18NDEFAULT_LOCALE = "prefs.user.language.default";

    /** I18N string to mark the server timezone */
    public static final String I18NSERVER_TIMEZONE = "prefs.user.timezone.server";

    /** Prefix of the default timeformat properties. */
    public static final String TIMEFORMATPROPERTIES = "jspwiki.defaultprefs.timeformat.";

    /** The name under which the resource includes map is stored in the  WikiContext. */
    public static final String RESOURCE_INCLUDES = "jspwiki.resourceincludes";

    // private Cache m_propertyCache;

    private static final Logger log = Logger.getLogger( TemplateManager.class );

    /** Requests a HTTP header. Value is {@value}. */
    public static final String RESOURCE_HTTPHEADER = "httpheader";

    /**
     *  Creates a new TemplateManager.  There is typically one manager per engine.
     *
     *  @param engine The owning engine.
     *  @param properties The property list used to initialize this.
     */
    public TemplateManager( final WikiEngine engine, final Properties properties ) {
        super( engine );

        //
        //  Uses the unlimited cache.
        //
        // m_propertyCache = new Cache( true, false );
    }

    /**
     *  Check the existence of a template.
     */
    // FIXME: Does not work yet
    public boolean templateExists( final String templateName ) {
        final ServletContext context = m_engine.getServletContext();
        try( final InputStream in = context.getResourceAsStream( getPath( templateName ) + "ViewTemplate.jsp" ) ) {
            if( in != null ) {
                return true;
            }
        } catch( final IOException e ) {
            log.error( e.getMessage(), e );
        }
        return false;
    }

    /**
     *  Tries to locate a given resource from the template directory. If the given resource is not found under the current name, returns the
     *  path to the corresponding one in the default template.
     *
     *  @param sContext The servlet context
     *  @param name The name of the resource
     *  @return The name of the resource which was found.
     */
    private static String findResource( final ServletContext sContext, final String name ) {
        String resourceName = name;
        try( final InputStream is = sContext.getResourceAsStream( resourceName ) ) {
            if( is == null ) {
                final String defname = makeFullJSPName( DEFAULT_TEMPLATE, removeTemplatePart( resourceName ) );
                try( final InputStream iis = sContext.getResourceAsStream( defname ) ) {
                    resourceName = iis != null ? defname : null;
                }
            }
        } catch( final IOException e ) {
            log.error( "unable to open " + name + " as resource stream", e );
        }
        return resourceName;
    }

    /**
     *  Attempts to find a resource from the given template, and if it's not found
     *  attempts to locate it from the default template.
     * @param sContext
     * @param template
     * @param name
     * @return the Resource for the given template and name.
     */
    private static String findResource( final ServletContext sContext, final String template, final String name ) {
        if( name.charAt(0) == '/' ) {
            // This is already a full path
            return findResource( sContext, name );
        }
        final String fullname = makeFullJSPName( template, name );
        return findResource( sContext, fullname );
    }

    /**
     *  An utility method for finding a JSP page.  It searches only under either current context or by the absolute name.
     *
     *  @param pageContext the JSP PageContext
     *  @param name The name of the JSP page to look for (e.g "Wiki.jsp")
     *  @return The context path to the resource
     */
    public String findJSP( final PageContext pageContext, final String name ) {
        final ServletContext sContext = pageContext.getServletContext();
        return findResource( sContext, name );
    }

    /**
     *  Removes the template part of a name.
     */
    private static String removeTemplatePart( String name ) {
        int idx = 0;
        if( name.startsWith( "/" ) ) {
            idx = 1;
        }

        idx = name.indexOf('/', idx);
        if( idx != -1 ) {
            idx = name.indexOf('/', idx+1); // Find second "/"
            if( idx != -1 ) {
                name = name.substring( idx+1 );
            }
        }

        if( log.isDebugEnabled() ) {
            log.debug( "Final name = "+name );
        }
        return name;
    }

    /**
     *  Returns the full name (/templates/foo/bar) for name=bar, template=foo.
     *
     * @param template The name of the template.
     * @param name The name of the resource.
     * @return The full name for a template.
     */
    private static String makeFullJSPName( final String template, final String name ) {
        return "/" + DIRECTORY + "/" + template + "/" + name;
    }

    /**
     *  Attempts to locate a resource under the given template.  If that template does not exist, or the page does not exist under that
     *  template, will attempt to locate a similarly named file under the default template.
     *  <p>
     *  Even though the name suggests only JSP files can be located, but in fact this method can find also other resources than JSP files.
     *
     *  @param pageContext The JSP PageContext
     *  @param template From which template we should seek initially?
     *  @param name Which resource are we looking for (e.g. "ViewTemplate.jsp")
     *  @return path to the JSP page; null, if it was not found.
     */
    public String findJSP( final PageContext pageContext, final String template, final String name ) {
        if( name == null || template == null ) {
            log.fatal("findJSP() was asked to find a null template or name (" + template + "," + name + ")." + " JSP page '" +
                      ( ( HttpServletRequest )pageContext.getRequest() ).getRequestURI() + "'" );
            throw new InternalWikiException( "Illegal arguments to findJSP(); please check logs." );
        }

        return findResource( pageContext.getServletContext(), template, name );
    }

    /**
     *  Attempts to locate a resource under the given template.  This matches the functionality findJSP(), but uses the WikiContext as
     *  the argument.  If there is no servlet context (i.e. this is embedded), will just simply return a best-guess.
     *  <p>
     *  This method is typically used to locate any resource, including JSP pages, images, scripts, etc.
     *
     *  @since 2.6
     *  @param ctx the wiki context
     *  @param template the name of the template to use
     *  @param name the name of the resource to fine
     *  @return the path to the resource
     */
    public String findResource( final WikiContext ctx, final String template, final String name ) {
        if( m_engine.getServletContext() != null ) {
            return findResource( m_engine.getServletContext(), template, name );
        }

        return getPath(template)+"/"+name;
    }

    /**
     *  Returns a property, as defined in the template.  The evaluation is lazy, i.e. the properties are not loaded until the template is
     *  actually used for the first time.
     */
    /*
    public String getTemplateProperty( WikiContext context, String key )
    {
        String template = context.getTemplate();

        try
        {
            Properties props = (Properties)m_propertyCache.getFromCache( template, -1 );

            if( props == null )
            {
                try
                {
                    props = getTemplateProperties( template );

                    m_propertyCache.putInCache( template, props );
                }
                catch( IOException e )
                {
                    log.warn("IO Exception while reading template properties",e);

                    return null;
                }
            }

            return props.getProperty( key );
        }
        catch( NeedsRefreshException ex )
        {
            // FIXME
            return null;
        }
    }
*/
    /**
     *  Returns an absolute path to a given template.
     */
    private static String getPath( final String template ) {
        return "/" + DIRECTORY + "/" + template + "/";
    }

    /**
     *   Lists the skins available under this template.  Returns an empty Set, if there are no extra skins available.  Note that
     *   this method does not check whether there is anything actually in the directories, it just lists them.  This may change
     *   in the future.
     *
     *   @param pageContext the JSP PageContext
     *   @param template The template to search
     *   @return Set of Strings with the skin names.
     *   @since 2.3.26
     */
    public Set< String > listSkins( final PageContext pageContext, final String template ) {
        final String place = makeFullJSPName( template, SKIN_DIRECTORY );
        final ServletContext sContext = pageContext.getServletContext();
        final Set< String > skinSet = sContext.getResourcePaths( place );
        final Set< String > resultSet = new TreeSet<>();

        if( log.isDebugEnabled() ) {
            log.debug( "Listings skins from " + place );
        }

        if( skinSet != null ) {
            final String[] skins = skinSet.toArray( new String[]{} );
            for( final String skin : skins ) {
                final String[] s = StringUtils.split( skin, "/" );
                if( s.length > 2 && skin.endsWith( "/" ) ) {
                    final String skinName = s[ s.length - 1 ];
                    resultSet.add( skinName );
                    if( log.isDebugEnabled() ) {
                        log.debug( "...adding skin '" + skinName + "'" );
                    }
                }
            }
        }

        return resultSet;
    }

    /**
     * List all installed i18n language properties by classpath searching for files like :
     *    templates/default_*.properties
     *    templates/default.properties
     *
     * @param pageContext
     * @return map of installed Languages
     * @since 2.7.x
     */
    public Map< String, String > listLanguages( final PageContext pageContext ) {
        final Map< String, String > resultMap = new LinkedHashMap<>();
        final String clientLanguage = pageContext.getRequest().getLocale().toString();
        final List< String > entries = ClassUtil.classpathEntriesUnder( DIRECTORY );
        for( String name : entries ) {
            if ( name.equals( I18NRESOURCE_EN ) || (name.startsWith( I18NRESOURCE_PREFIX ) && name.endsWith( I18NRESOURCE_SUFFIX ) ) ) {
                if( name.equals( I18NRESOURCE_EN ) ) {
                    name = I18NRESOURCE_EN_ID;
                } else {
                    name = name.substring( I18NRESOURCE_PREFIX.length(), name.lastIndexOf( I18NRESOURCE_SUFFIX ) );
                }
                final Locale locale = new Locale( name.substring( 0, 2 ), ( ( name.indexOf( "_" ) == -1 ) ? "" : name.substring( 3, 5 ) ) );
                String defaultLanguage = "";
                if( clientLanguage.startsWith( name ) ) {
                    defaultLanguage = LocaleSupport.getLocalizedMessage( pageContext, I18NDEFAULT_LOCALE );
                }
                resultMap.put( name, locale.getDisplayName( locale ) + " " + defaultLanguage );
            }
        }

        return resultMap;
    }


    /**
     * List all available timeformats, read from the jspwiki.properties
     *
     * @param pageContext
     * @return map of TimeFormats
     * @since 2.7.x
     */
    public Map< String, String > listTimeFormats( final PageContext pageContext ) {
        final WikiContext context = WikiContext.findContext( pageContext );
        final Properties props = m_engine.getWikiProperties();
        final ArrayList< String > tfArr = new ArrayList<>(40);
        final LinkedHashMap< String, String > resultMap = new LinkedHashMap<>();

        /* filter timeformat properties */
        for( final Enumeration< ? > e = props.propertyNames(); e.hasMoreElements(); ) {
            final String name = ( String )e.nextElement();
            if( name.startsWith( TIMEFORMATPROPERTIES ) ) {
                tfArr.add( name );
            }
        }

        /* fetch actual formats */
        if( tfArr.size() == 0 )  {/* no props found - make sure some default formats are avail */
            tfArr.add( "dd-MMM-yy" );
            tfArr.add( "d-MMM-yyyy" );
            tfArr.add( "EEE, dd-MMM-yyyy, zzzz" );
        } else {
            Collections.sort( tfArr );

            for (int i = 0; i < tfArr.size(); i++) {
                tfArr.set(i, props.getProperty(tfArr.get(i)));
            }
        }

        final String prefTimeZone = Preferences.getPreference( context, "TimeZone" );
        //TimeZone tz = TimeZone.getDefault();
        final TimeZone tz = TimeZone.getTimeZone(prefTimeZone);
        /*try
        {
            tz.setRawOffset(Integer.parseInt(prefTimeZone));
        }
        catch (Exception e)
        {
        }*/

        final Date d = new Date(); // current date
        try {
            // dummy format pattern
            final SimpleDateFormat fmt = Preferences.getDateFormat( context, TimeFormat.DATETIME );
            fmt.setTimeZone( tz );

            for( int i = 0; i < tfArr.size(); i++ ) {
                try {
                    final String f = tfArr.get( i );
                    fmt.applyPattern( f );
                    resultMap.put( f, fmt.format( d ) );
                } catch( final IllegalArgumentException e ) {} // skip parameter
            }
        }
        catch( final IllegalArgumentException e ) {} // skip parameter

        return resultMap;
    }

    /**
     * List all timezones, with special marker for server timezone
     *
     * @param pageContext
     * @return map of TimeZones
     * @since 2.7.x
     */
    public Map< String, String > listTimeZones( final PageContext pageContext ) {
        final Map<String,String> resultMap = new LinkedHashMap<>();
        final String[][] tzs = {
                          { "GMT-12", "Enitwetok, Kwajalien" },
                          { "GMT-11", "Nome, Midway Island, Samoa" },
                          { "GMT-10", "Hawaii" },
                          { "GMT-9", "Alaska" },
                          { "GMT-8", "Pacific Time" },
                          { "GMT-7", "Mountain Time" },
                          { "GMT-6", "Central Time, Mexico City" },
                          { "GMT-5", "Eastern Time, Bogota, Lima, Quito" },
                          { "GMT-4", "Atlantic Time, Caracas, La Paz" },
                          { "GMT-3:30", "Newfoundland" },
                          { "GMT-3", "Brazil, Buenos Aires, Georgetown, Falkland Is." },
                          { "GMT-2", "Mid-Atlantic, Ascention Is., St Helena" },
                          { "GMT-1", "Azores, Cape Verde Islands" },
                          { "GMT", "Casablanca, Dublin, Edinburgh, London, Lisbon, Monrovia" },
                          { "GMT+1", "Berlin, Brussels, Copenhagen, Madrid, Paris, Rome" },
                          { "GMT+2", "Helsinki, Athens, Kaliningrad, South Africa, Warsaw" },
                          { "GMT+3", "Baghdad, Riyadh, Moscow, Nairobi" },
                          { "GMT+3:30", "Tehran" },
                          { "GMT+4", "Adu Dhabi, Baku, Muscat, Tbilisi" },
                          { "GMT+4:30", "Kabul" },
                          { "GMT+5", "Islamabad, Karachi, Tashkent" },
                          { "GMT+5:30", "Bombay, Calcutta, Madras, New Delhi" },
                          { "GMT+6", "Almaty, Colomba, Dhakra" },
                          { "GMT+7", "Bangkok, Hanoi, Jakarta" },
                          { "GMT+8", "Beijing, Hong Kong, Perth, Singapore, Taipei" },
                          { "GMT+9", "Osaka, Sapporo, Seoul, Tokyo, Yakutsk" },
                          { "GMT+9:30", "Adelaide, Darwin" },
                          { "GMT+10", "Melbourne, Papua New Guinea, Sydney, Vladivostok" },
                          { "GMT+11", "Magadan, New Caledonia, Solomon Islands" },
                          { "GMT+12", "Auckland, Wellington, Fiji, Marshall Island" } };

        final TimeZone servertz = TimeZone.getDefault();
        for( final String[] strings : tzs ) {
            String tzID = strings[ 0 ];
            final TimeZone tz = TimeZone.getTimeZone( tzID );
            String serverTimeZone = "";
            if( servertz.getRawOffset() == tz.getRawOffset() ) {
                serverTimeZone = LocaleSupport.getLocalizedMessage( pageContext, I18NSERVER_TIMEZONE );
                tzID = servertz.getID();
            }

            resultMap.put( tzID, "(" + strings[ 0 ] + ") " + strings[ 1 ] + " " + serverTimeZone );
        }

        return resultMap;
    }

    /**
     *  Always returns a valid property map.
     */
    /*
    private Properties getTemplateProperties( String templateName )
        throws IOException
    {
        Properties p = new Properties();

        ServletContext context = m_engine.getServletContext();

        InputStream propertyStream = context.getResourceAsStream(getPath(templateName)+PROPERTYFILE);

        if( propertyStream != null )
        {
            p.load( propertyStream );

            propertyStream.close();
        }
        else
        {
            log.debug("Template '"+templateName+"' does not have a propertyfile '"+PROPERTYFILE+"'.");
        }

        return p;
    }
*/
    /**
     *  Returns the include resources marker for a given type.  This is in a
     *  HTML or Javascript comment format.
     *
     *  @param context the wiki context
     *  @param type the marker
     *  @return the generated marker comment
     */
    public static String getMarker( final WikiContext context, final String type ) {
        if( type.equals( RESOURCE_JSLOCALIZEDSTRINGS ) ) {
            return getJSLocalizedStrings( context );
        } else if( type.equals( RESOURCE_JSFUNCTION ) ) {
            return "/* INCLUDERESOURCES ("+type+") */";
        }
        return "<!-- INCLUDERESOURCES ("+type+") -->";
    }

    /**
     *  Extract all i18n strings in the javascript domain. (javascript.*) Returns a javascript snippet which defines the LocalizedStings array.
     *
     *  @param context the {@link WikiContext}
     *  @return Javascript snippet which defines the LocalizedStrings array
     *  @since 2.5.108
     */
    private static String getJSLocalizedStrings( final WikiContext context ) {
        final StringBuilder sb = new StringBuilder();
        sb.append( "var LocalizedStrings = {\n");
        final ResourceBundle rb = Preferences.getBundle( context, InternationalizationManager.DEF_TEMPLATE );
        boolean first = true;

        for( final Enumeration< String > en = rb.getKeys(); en.hasMoreElements(); ) {
            final String key = en.nextElement();
            if( key.startsWith("javascript") ) {
                if( first ) {
                    first = false;
                } else {
                    sb.append( ",\n" );
                }
                sb.append( "\""+key+"\":\""+rb.getString(key)+"\"" );
            }
        }
        sb.append("\n};\n");

        return( sb.toString() );
    }

    /**
     *  Adds a resource request to the current request context. The content will be added at the resource-type marker
     *  (see IncludeResourcesTag) in WikiJSPFilter.
     *  <p>
     *  The resources can be of different types.  For RESOURCE_SCRIPT and RESOURCE_STYLESHEET this is an URI path to the resource
     *  (a script file or an external stylesheet) that needs to be included.  For RESOURCE_INLINECSS the resource should be something
     *  that can be added between &lt;style>&lt;/style> in the header file (commonheader.jsp).  For RESOURCE_JSFUNCTION it is the name
     *  of the Javascript function that should be run at page load.
     *  <p>
     *  The IncludeResourceTag inserts code in the template files, which is then filled by the WikiFilter after the request has been
     *  rendered but not yet sent to the recipient.
     *  <p>
     *  Note that ALL resource requests get rendered, so this method does not check if the request already exists in the resources.
     *  Therefore, if you have a plugin which makes a new resource request every time, you'll end up with multiple resource requests
     *  rendered.  It's thus a good idea to make this request only once during the page life cycle.
     *
     *  @param ctx The current wiki context
     *  @param type What kind of a request should be added?
     *  @param resource The resource to add.
     */
    @SuppressWarnings("unchecked")
    public static void addResourceRequest( final WikiContext ctx, final String type, final String resource ) {
        HashMap< String, Vector< String > > resourcemap = ( HashMap< String, Vector< String > > ) ctx.getVariable( RESOURCE_INCLUDES );
        if( resourcemap == null ) {
            resourcemap = new HashMap<>();
        }

        Vector< String > resources = resourcemap.get( type );
        if( resources == null ) {
            resources = new Vector<>();
        }

        String resourceString = null;
        switch( type ) {
        case RESOURCE_SCRIPT:
            resourceString = "<script type='text/javascript' src='" + resource + "'></script>";
            break;
        case RESOURCE_STYLESHEET:
            resourceString = "<link rel='stylesheet' type='text/css' href='" + resource + "' />";
            break;
        case RESOURCE_INLINECSS:
            resourceString = "<style type='text/css'>\n" + resource + "\n</style>\n";
            break;
        case RESOURCE_JSFUNCTION:
        case RESOURCE_HTTPHEADER:
            resourceString = resource;
            break;
        }

        if( resourceString != null ) {
            resources.add( resourceString );
        }

        log.debug( "Request to add a resource: " + resourceString );

        resourcemap.put( type, resources );
        ctx.setVariable( RESOURCE_INCLUDES, resourcemap );
    }

    /**
     *  Returns resource requests for a particular type.  If there are no resources, returns an empty array.
     *
     *  @param ctx WikiContext
     *  @param type The resource request type
     *  @return a String array for the resource requests
     */
    @SuppressWarnings("unchecked")
    public static String[] getResourceRequests( final WikiContext ctx, final String type ) {
        final HashMap< String, Vector< String > > hm = ( HashMap< String, Vector< String > > ) ctx.getVariable( RESOURCE_INCLUDES );
        if( hm == null ) {
            return new String[0];
        }

        final Vector<String> resources = hm.get( type );
        if( resources == null ){
            return new String[0];
        }

        final String[] res = new String[resources.size()];
        return resources.toArray( res );
    }

    /**
     *  Returns all those types that have been requested so far.
     *
     * @param ctx the wiki context
     * @return the array of types requested
     */
    @SuppressWarnings("unchecked")
    public static String[] getResourceTypes( final WikiContext ctx ) {
        String[] res = new String[0];
        if( ctx != null ) {
            final HashMap< String, String > hm = ( HashMap< String, String > ) ctx.getVariable( RESOURCE_INCLUDES );
            if( hm != null ) {
                final Set< String > keys = hm.keySet();
                res = keys.toArray( res );
            }
        }

        return res;
    }

    /**
     *  Returns an empty collection, since at the moment the TemplateManager does not manage any modules.
     *
     *  @return {@inheritDoc}
     */
    @Override
    public Collection< WikiModuleInfo > modules() {
        return new ArrayList<>();
    }

    /**
     *  Returns null!
     *  {@inheritDoc}
     */
    @Override
    public WikiModuleInfo getModuleInfo( final String moduleName ) {
    	return null;
    }

}
