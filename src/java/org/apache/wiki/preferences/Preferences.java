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
package org.apache.wiki.preferences;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.TypeConverterFactory;
import net.sourceforge.stripes.validation.ValidationError;

import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.TemplateManager;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;

/**
 * <p>
 * Represents user preferences for language, timezone, preferred template skin
 * and other customizable user interface items. Preferences are initialized by
 * JSPWiki by reading a series of cookies from the user's HTTP request. This
 * happens once per request, during the execution of
 * {@link org.apache.wiki.ui.stripes.WikiInterceptor}. The method
 * {@link #initialize(HttpServletRequest)} performs the actual initialization.
 * <p>
 * After initialization, Preferences are stashed in the user's session as an
 * attribute named {@code prefs}. Preferences may be subsequently changed by
 * calling {@link org.apache.wiki.action.UserPreferencesActionBean#save()} with
 * appropriate parameters, generally from a JSP.
 * </p>
 */
public class Preferences implements Map<String, Object>
{
    /**
     * Enumeration of the orientation formats for favorites.
     */
    public enum Orientation
    {
        /** Favorites to the left. */
        LEFT,
        /** Favorites to the right. */
        RIGHT
    }

    /**
     * <p>
     * Formatting style for displaying times and dates.
     * </p>
     * <ul>
     * <li>TIME: A time format, without date</li>
     * <li>DATE: A date format, without a time</li>
     * <li>DATETIME: A date format, with a time</li>
     * </ul>
     * 
     * @since 2.8
     */
    public enum TimeFormat
    {
        /** Time only; no date. */
        TIME,

        /** Date only; no time. */
        DATE,

        /** Date plus time. */
        DATETIME
    }

    /**
     * Cookie name for the user's preference for displaying dates and times.
     */
    public static final String PREFS_TIME_FORMAT = "TimeFormat";

    /**
     * Cookie name for the user's preference for displaying time zones.
     */
    public static final String PREFS_TIME_ZONE = "TimeZone";

    /**
     * Cookie name for the user's preference for text orientation.
     */
    public static final String PREFS_ORIENTATION = "Orientation";

    /**
     * Cookie name for the user's preference for section editing (on/off).
     */
    public static final String PREFS_SECTION_EDITING = "SectionEditing";

    /**
     * Cookie name for the user's preferred {@link org.apache.wiki.ui.Editor}.
     */
    public static final String PREFS_EDITOR = "Editor";

    /**
     * Cookie name for the user's preferred {@link java.util.Locale}.
     */
    public static final String PREFS_LOCALE = "Locale";

    /**
     * Cookie name for the user's preferred template skin.
     */
    public static final String PREFS_SKIN = "Skin";

    public static final Map<Locale, String> AVAILABLE_LOCALES;

    public static void main( String[] args )
    {
        String[] ids = TimeZone.getAvailableIDs();
        Map<Integer, Set<TimeZone>> tzs = new TreeMap<Integer, Set<TimeZone>>();
        long now = System.currentTimeMillis();
        for( String id : ids )
        {
            TimeZone tz = TimeZone.getTimeZone( id );
            int offset = tz.getOffset( now ) / (1000 * 60);
            Set<TimeZone> tzsForOffset = tzs.get( offset );
            if( tzsForOffset == null )
            {
                tzsForOffset = new HashSet<TimeZone>();
                tzs.put( offset, tzsForOffset );
            }
            if( tzsForOffset.size() < 10 )
            {
                tzsForOffset.add( tz );
            }
        }
        System.out.println( "Foo!" );
        for( Map.Entry<Integer, Set<TimeZone>> entry : tzs.entrySet() )
        {
            System.out.print( entry.getKey() + ": " );
            for( TimeZone tz : entry.getValue() )
            {
                String city = tz.getID();
                int firstSlash = city.indexOf( "/" );
                if( firstSlash >= 0 )
                {
                    city = city.substring( firstSlash + 1 );
                }
                city = city.replace( '_', ' ' );
                System.out.print( city + ", " );
            }
            System.out.println( "." );
        }
    }

    private final Collection<ValidationError> m_errors;

    /**
     * List of time zones, used by
     * {@link #getAvailableTimeZones(HttpServletRequest)}.
     */
    public static final List<TimeZone> AVAILABLE_TIME_ZONES;

    private static final Map<String, Class<? extends Object>> TYPE_CONVERTERS;

    /** Prefix of the default timeformat properties. */
    public static final String TIMEFORMATPROPERTIES = "jspwiki.defaultprefs.timeformat.";

    /**
     * The name under which a Preferences object is stored in the HttpSession.
     * Its value is {@value} .
     */
    private static final String SESSIONPREFS = "prefs";

    private static final long serialVersionUID = 2L;

    private static Logger log = LoggerFactory.getLogger( Preferences.class );

    static
    {
        // Init time zones
        List<TimeZone> zones = new ArrayList<TimeZone>();
        String[] ids = { "Pacific/Midway", "Pacific/Honolulu", "America/Anchorage", "America/Los_Angeles", "US/Mountain",
                        "America/Chicago", "America/New_York", "America/Caracas", "Brazil/East", "Atlantic/South_Georgia",
                        "Atlantic/Cape_Verde", "Etc/Greenwich", "Europe/Rome", "ART", "EAT", "Asia/Dubai", "IST", "BST", "VST",
                        "CTT", "JST", "Australia/Sydney", "SST", "NZ", "Pacific/Tongatapu", "Pacific/Kiritimati" };
        for( String id : ids )
        {
            java.util.TimeZone zone = java.util.TimeZone.getTimeZone( id );
            zones.add( zone );
        }
        AVAILABLE_TIME_ZONES = Collections.unmodifiableList( zones );

        // Init locales
        Locale[] locales = Locale.getAvailableLocales();
        Map<Locale, String> foundLocales = new HashMap<Locale, String>();
        for( Locale locale : locales )
        {
            URL url = TemplateManager.class.getClassLoader().getResource( "CoreResources_" + locale.toString() + ".properties" );
            if( url != null )
            {
                foundLocales.put( locale, locale.getDisplayName( locale ) );
            }
        }
        AVAILABLE_LOCALES = Collections.unmodifiableMap( foundLocales );

        // Init type converter map; default is String unless added here
        TYPE_CONVERTERS = new HashMap<String, Class<?>>();
        TYPE_CONVERTERS.put( PREFS_LOCALE, Locale.class );
        TYPE_CONVERTERS.put( PREFS_ORIENTATION, Orientation.class );
        TYPE_CONVERTERS.put( PREFS_SECTION_EDITING, Boolean.class );
        TYPE_CONVERTERS.put( PREFS_TIME_ZONE, TimeZone.class );

    }

    /**
     * Looks up the Preferences object in the user's HTTP session, lazily
     * creating and initializing one if it does not already exist.
     * 
     * @param request the HTTP request
     * @return the preferences object
     */
    public static Preferences getPreferences( HttpServletRequest request )
    {
        Preferences prefs = (Preferences) request.getSession().getAttribute( SESSIONPREFS );
        if( prefs == null )
        {
            WikiEngine engine = WikiEngine.getInstance( request.getSession().getServletContext(), null );
            prefs = new Preferences( engine );
            prefs.initialize( request );
            request.getSession().setAttribute( SESSIONPREFS, prefs );
        }
        return prefs;
    }

    private final WikiEngine m_engine;

    private final Map<String, Object> m_prefs = new HashMap<String, Object>();

    /**
     * Private constructor to prevent direct instantiation.
     * 
     * @param engine the wiki engine
     */
    private Preferences( WikiEngine engine )
    {
        m_engine = engine;
        m_errors = new ArrayList<ValidationError>();
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        m_prefs.clear();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey( Object key )
    {
        return m_prefs.containsKey( key );
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue( Object value )
    {
        return m_prefs.containsKey( value );
    }

    /**
     * {@inheritDoc}
     */
    public Set<java.util.Map.Entry<String, Object>> entrySet()
    {
        return m_prefs.entrySet();
    }

    /**
     * {@inheritDoc}. The special key names {@code availableLocales}, {@code
     * availableSkins}, {@code availableTimeFormats}, {@code availableTimeZones}
     * return what you would expect.
     */
    public Object get( Object key )
    {
        if( "availableLocales".equals( key ) )
        {
            return AVAILABLE_LOCALES;
        }
        else if( "availableSkins".equals( key ) )
        {
            return m_engine.getTemplateManager().listSkins( m_engine.getServletContext(), m_engine.getTemplateDir() );
        }
        else if( "availableTimeFormats".equals( key ) )
        {
            return availableTimeFormats();
        }
        else if( "availableTimeZones".equals( key ) )
        {
            return AVAILABLE_TIME_ZONES;
        }
        return m_prefs.get( key );
    }

    /**
     * Get SimpleTimeFormat according to user browser locale and preferred time
     * formats. If not found, it will revert to whichever format is set for the
     * default
     * 
     * @param tf Which version of the dateformat you are looking for?
     * @return A SimpleTimeFormat object which you can use to render
     * @since 2.8
     */
    public SimpleDateFormat getDateFormat( TimeFormat tf )
    {
        String prefDateFormat;
        InternationalizationManager i18n = m_engine.getInternationalizationManager();

        log.debug( "Checking for preferences..." );

        switch( tf )
        {
            case DATETIME:
                prefDateFormat = (String) get( PREFS_TIME_FORMAT );
                log.debug( "Preferences fmt = " + prefDateFormat );
                if( prefDateFormat == null )
                {
                    Locale locale = (Locale) get( PREFS_LOCALE );
                    prefDateFormat = i18n.get( InternationalizationManager.CORE_BUNDLE, locale, "common.datetimeformat" );
                    log.debug( "Using locale-format = " + prefDateFormat );
                }
                break;

            case TIME:
                prefDateFormat = i18n.get( "common.timeformat" );
                break;

            case DATE:
                prefDateFormat = i18n.get( "common.dateformat" );
                break;

            default:
                throw new InternalWikiException( "Got a TimeFormat for which we have no value!" );
        }

        try
        {
            Locale locale = (Locale) get( PREFS_LOCALE );
            SimpleDateFormat fmt = new SimpleDateFormat( prefDateFormat, locale );
            TimeZone tz = (TimeZone) get( PREFS_TIME_ZONE );
            if( tz != null )
            {
                fmt.setTimeZone( tz );
            }
            return fmt;
        }
        catch( Exception e )
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return m_prefs.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> keySet()
    {
        return m_prefs.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public Object put( String key, Object value )
    {
        return m_prefs.put( key, value );
    }

    /**
     * {@inheritDoc}
     */
    public void putAll( Map<? extends String, ? extends Object> m )
    {
        m_prefs.putAll( m );
    }

    /**
     * {@inheritDoc}
     */
    public Object remove( Object key )
    {
        return m_prefs.remove( key );
    }

    /**
     * Saves all preferences. At the moment the only persistence mechanism
     * is to save preferences to cookies. However, future versions will
     * save authenticated users' preferences to the JCR.
     * 
     * @param context the ActionBeanContext FIXME: make this use JCR for
     *            authenticated users
     */
    public void save( WikiActionBeanContext context )
    {
        HttpServletResponse response = context.getResponse();
        for( Map.Entry<String, Object> item : m_prefs.entrySet() )
        {
            String key = item.getKey();
            Object value = item.getValue();
            if( key != null && value != null )
            {
                String stringValue = null;
                if( value instanceof Enum )
                {
                    stringValue = ((Enum<?>) value).name();
                }
                else if( value instanceof Boolean )
                {
                    stringValue = Boolean.toString( (Boolean) value );
                }
                else if( value instanceof TimeZone )
                {
                    stringValue = ((TimeZone) value).getID();
                }
                else
                {
                    stringValue = value.toString();
                }
                Cookie cookie = new Cookie( key, stringValue );
                response.addCookie( cookie );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return m_prefs.size();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Object> values()
    {
        return m_prefs.values();
    }

    /**
     * List all available timeformats, read from the jspwiki.properties
     * 
     * @return map of TimeFormats
     * @since 2.7.x
     */
    private Map<String, String> availableTimeFormats()
    {
        Properties props = m_engine.getWikiProperties();
        ArrayList<String> timeFormats = new ArrayList<String>( 40 );
        LinkedHashMap<String, String> resultMap = new LinkedHashMap<String, String>();

        /* filter timeformat properties */
        for( Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();

            if( name.startsWith( TIMEFORMATPROPERTIES ) )
            {
                timeFormats.add( name );
            }
        }

        /* fetch actual formats */
        if( timeFormats.size() == 0 ) /*
                                       * no props found - make sure some default
                                       * formats are avail
                                       */
        {
            timeFormats.add( "dd-MMM-yy" );
            timeFormats.add( "d-MMM-yyyy" );
            timeFormats.add( "EEE, dd-MMM-yyyy, zzzz" );
        }
        else
        {
            Collections.sort( timeFormats );

            for( int i = 0; i < timeFormats.size(); i++ )
            {
                timeFormats.set( i, props.getProperty( timeFormats.get( i ) ) );
            }
        }

        TimeZone timeZone = (TimeZone) m_prefs.get( PREFS_TIME_ZONE );

        Date d = new Date(); // current date
        try
        {
            // dummy format pattern
            SimpleDateFormat format = getDateFormat( TimeFormat.DATETIME );
            format.setTimeZone( timeZone );

            for( int i = 0; i < timeFormats.size(); i++ )
            {
                try
                {
                    String f = timeFormats.get( i );
                    format.applyPattern( f );

                    resultMap.put( f, format.format( d ) );
                }
                catch( IllegalArgumentException e )
                {
                } // skip parameter
            }
        }
        catch( IllegalArgumentException e )
        {
        } // skip parameter

        return resultMap;
    }

    @SuppressWarnings( "unchecked" )
    private Object coerceCookieValue( HttpServletRequest request, TypeConverterFactory converters, String cookieName,
                                      String defaultValue )
    {
        // Get the cookie value as a String
        String cookieValue = null;
        Cookie[] cookies = request.getCookies();
        if( cookies != null )
        {
            for( Cookie cookie : request.getCookies() )
            {
                if( cookieName.equals( cookie.getName() ) )
                {
                    cookieValue = cookie.getValue();
                    break;
                }
            }
        }
        if( cookieValue == null )
        {
            cookieValue = defaultValue;
        }

        // Coerce the value into the target format
        Locale locale = (Locale) get( PREFS_LOCALE );
        Class targetClass = TYPE_CONVERTERS.get( cookieName );
        if( targetClass == null )
        {
            targetClass = String.class;
        }
        Object value = null;
        try
        {
            TypeConverter<?> converter = converters.getTypeConverter( targetClass, locale );
            converter.setLocale( locale );
            value = converter.convert( cookieValue, targetClass, m_errors );
        }
        catch( Exception e )
        {
            log.error( "Could not coerce cookie " + cookieName + "\"" );
        }

        // If not coerced, return the default value
        return value == null ? defaultValue : value;
    }

    /**
     * <p>
     * This method checks to see if user preferences have been loaded from
     * request cookies, and if not, loads them. This method is called from three
     * places: from
     * {@link org.apache.wiki.ui.stripes.WikiInterceptor#intercept(net.sourceforge.stripes.controller.ExecutionContext)},
     * {@link org.apache.wiki.action.WikiContextFactory#newContext(HttpServletRequest, javax.servlet.http.HttpServletResponse, String)}
     * and
     * {@link org.apache.wiki.action.WikiContextFactory#newViewContext(HttpServletRequest, javax.servlet.http.HttpServletResponse, org.apache.wiki.api.WikiPage)}
     * .
     * </p>
     * <p>
     * Every user preference is read from a unique cookie. This method parses
     * each value from the respective cookie, and applies a default value if the
     * cookie is not found in the request. Once all preference values are
     * parsed, this method stashes the Preferences object in the user's
     * HTTPSession so that as an attribute named {@link #SESSIONPREFS}, so that
     * it can be recalled later. So that JSPWiki does not waste excessive cycles
     * parsing preference cookies on every request, if this method finds the
     * SESSIONPREFS already present in the session, it returns silently without
     * doing any more work.
     * </p>
     * <p>
     * To change user preference values after the initial parse, users normally
     * invoke {@link org.apache.wiki.action.UserPreferencesActionBean#save()}.
     * </p>
     * 
     * @param request the HTTP request
     * @throws WikiException if the preferences cannot be initialized
     */
    private void initialize( HttpServletRequest request )
    {
        // Set up the Locale before we do anything else
        Properties props = m_engine.getWikiProperties();
        Locale locale = request.getLocale();
        m_prefs.put( PREFS_LOCALE, locale );

        // Get the type converter factory
        Configuration stripesConfig = StripesFilter.getConfiguration();
        if( stripesConfig == null )
        {
            throw new InternalWikiException( "Cannot obtain Stripes configuration. This might happen if " + "called outside a JSP." );
        }
        TypeConverterFactory converters = stripesConfig.getTypeConverterFactory();
        m_errors.clear();

        // FIXME: "editor" property does not get registered, may be related with
        // http://bugs.jspwiki.org/show_bug.cgi?id=117
        // disabling it until knowing why it's happening
        // FIXME: editormanager reads jspwiki.editor -- which of both properties
        // should continue
        String defaultValue = props.getProperty( "jspwiki.defaultprefs.template.editor", "plain" );
        Object value = coerceCookieValue( request, converters, PREFS_EDITOR, defaultValue );
        m_prefs.put( PREFS_EDITOR, value );

        // Init the orientation
        defaultValue = props.getProperty( "jspwiki.defaultprefs.template.orientation", Orientation.LEFT.name() );
        value = coerceCookieValue( request, converters, PREFS_ORIENTATION, defaultValue );
        m_prefs.put( PREFS_ORIENTATION, value );

        // Init section editing
        defaultValue = props.getProperty( "jspwiki.defaultprefs.template.sectionediting", "false" );
        value = coerceCookieValue( request, converters, PREFS_SECTION_EDITING, defaultValue );
        m_prefs.put( PREFS_SECTION_EDITING, value );

        // Init the template skin
        defaultValue = props.getProperty( "jspwiki.defaultprefs.template.skinname", "PlainVanilla" );
        value = coerceCookieValue( request, converters, PREFS_SKIN, defaultValue );
        m_prefs.put( PREFS_SKIN, value );

        // Init the time format
        defaultValue = props.getProperty( "jspwiki.defaultprefs.template.dateformat", m_engine.getInternationalizationManager()
            .get( InternationalizationManager.CORE_BUNDLE, locale, "common.datetimeformat" ) );
        value = coerceCookieValue( request, converters, PREFS_TIME_FORMAT, defaultValue );
        m_prefs.put( PREFS_TIME_FORMAT, value );

        // Init the time zone
        defaultValue = props.getProperty( "jspwiki.defaultprefs.template.timezone", java.util.TimeZone.getDefault().getID() );
        value = coerceCookieValue( request, converters, PREFS_TIME_ZONE, defaultValue );
        for( TimeZone timeZone : AVAILABLE_TIME_ZONES )
        {
            if( timeZone.getRawOffset() == ((TimeZone) value).getRawOffset() )
            {
                value = timeZone;
                break;
            }
        }
        m_prefs.put( PREFS_TIME_ZONE, value );
    }

}
