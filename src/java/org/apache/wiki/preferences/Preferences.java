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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.TemplateManager;
import org.apache.wiki.ui.stripes.LocaleConverter;
import org.apache.wiki.util.PropertyReader;


/**
 * <p>
 * Represents user preferences for language, timezone, preferred template skin
 * and other customizable user interface items. Preferences are initialized by
 * JSPWiki by reading a series of cookies from the user's HTTP request. This
 * happens once per request, during the execution of
 * {@link org.apache.wiki.ui.stripes.WikiInterceptor}.
 * The method {@link #setupPreferences(HttpServletRequest)} performs the actual
 * initialization.
 * <p>
 * After initialization, Preferences are stashed in the user's session as an
 * attribute named {@link #SESSIONPREFS}. Preferences may be subsequently
 * changed by calling
 * {@link org.apache.wiki.action.UserPreferencesActionBean#save()} with
 * appropriate parameters, generally from a JSP.
 * </p>
 */
public class Preferences extends HashMap<String, String>
{
    private static final Map<Locale,String> LOCALES;

    /**
     * Private constructor to prevent direct instantiation.
     */
    private Preferences() {}

    /**
     * List of time zones, used by {@link #listTimeZones(HttpServletRequest)}.
     */
    private static final List<TimeZone> TIME_ZONES;

    static
    {
        // Init time zones
        List<TimeZone> zones = new ArrayList<TimeZone>();
        String[] ids = { "Pacific/Midway", "Pacific/Honolulu", "America/Anchorage", "PST", "MST", "CST", "EST", "America/Caracas",
                        "Brazil/East", "Atlantic/South_Georgia", "Atlantic/Cape_Verde", "Etc/Greenwich", "CET", "ART", "EAT",
                        "Asia/Dubai", "IST", "BST", "VST", "CTT", "JST", "Australia/Sydney", "SST", "NZ", "Pacific/Tongatapu",
                        "Pacific/Kiritimati" };
        for( String id : ids )
        {
            java.util.TimeZone zone = java.util.TimeZone.getTimeZone( id );
            zones.add( zone );
        }
        TIME_ZONES = Collections.unmodifiableList( zones );
        
        // Init locales
        Locale[] locales = Locale.getAvailableLocales();
        Map<Locale,String> foundLocales = new HashMap<Locale,String>();
        for ( Locale locale : locales )
        {
            URL url = TemplateManager.class.getClassLoader().getResource( "CoreResources_" + locale.toString() + ".properties" );
            if ( url != null )
            {
                foundLocales.put( locale, locale.getDisplayName( locale ) );
            }
        }
        LOCALES = Collections.unmodifiableMap( foundLocales );
    }

    /**
     * <p>
     * Enumeration of three different date formats that JSPWiki supports.
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
     * The name under which a Preferences object is stored in the HttpSession.
     * Its value is {@value}.
     */
    public static final String SESSIONPREFS = "prefs";

    /**
     * Cookie name for the user's preferred {@link java.util.Locale}.
     */
    public static final String PREFS_LOCALE = "Locale";

    /** I18N string to mark the server timezone */
    public static final String I18NSERVER_TIMEZONE = "prefs.user.timezone.server";

    /**
     * Cookie name for the user's preferred template skin.
     */
    public static final String PREFS_SKIN = "Skin";

    private static final long serialVersionUID = 2L;

    private static Logger log = LoggerFactory.getLogger( Preferences.class );

    private static final LocaleConverter LOCALE_CONVERTER = new LocaleConverter();

    /** Prefix of the default timeformat properties. */
    public static final String TIMEFORMATPROPERTIES = "jspwiki.defaultprefs.timeformat.";

    /**
     * Get SimpleTimeFormat according to user browser locale and preferred time
     * formats. If not found, it will revert to whichever format is set for the
     * default
     * 
     * @param context WikiContext to use for rendering.
     * @param tf Which version of the dateformat you are looking for?
     * @return A SimpleTimeFormat object which you can use to render
     * @since 2.8
     */
    public static SimpleDateFormat getDateFormat( WikiContext context, TimeFormat tf )
    {
        InternationalizationManager imgr = context.getEngine().getInternationalizationManager();
        Locale clientLocale = getLocale( context.getHttpRequest() );
        String prefTimeZone = Preferences.getPreference( context, PREFS_TIME_ZONE );
        String prefDateFormat;

        log.debug( "Checking for preferences..." );

        switch( tf )
        {
            case DATETIME:
                prefDateFormat = Preferences.getPreference( context, PREFS_TIME_FORMAT );
                log.debug( "Preferences fmt = " + prefDateFormat );
                if( prefDateFormat == null )
                {
                    prefDateFormat = imgr.get( InternationalizationManager.CORE_BUNDLE, clientLocale, "common.datetimeformat" );
                    log.debug( "Using locale-format = " + prefDateFormat );
                }
                break;

            case TIME:
                prefDateFormat = imgr.get( "common.timeformat" );
                break;

            case DATE:
                prefDateFormat = imgr.get( "common.dateformat" );
                break;

            default:
                throw new InternalWikiException( "Got a TimeFormat for which we have no value!" );
        }

        try
        {
            SimpleDateFormat fmt = new SimpleDateFormat( prefDateFormat, clientLocale );

            if( prefTimeZone != null )
            {
                TimeZone tz = TimeZone.getTimeZone( prefTimeZone );
                // TimeZone tz = TimeZone.getDefault();
                // tz.setRawOffset(Integer.parseInt(prefTimeZone));

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
     * Returns the user's preferred {@link java.util.Locale} according the
     * contents of the cookie value {@link #PREFS_LOCALE}, or alternatively the
     * Locale supplied by the HTTP request object.
     * 
     * @param request the HTTP request
     * @return a Locale object.
     * @since 2.8
     */
    public static Locale getLocale( HttpServletRequest request )
    {
        String localePref = getCookieValue( request, PREFS_LOCALE, request.getLocale().toString() );
        Locale locale = LOCALE_CONVERTER.convert( localePref, Locale.class, null );

        // Otherwise use the JVM's default
        if( locale == null )
        {
            locale = Locale.getDefault();
        }
        return locale;
    }

    /**
     * Returns a preference value from the Preferences object stored in the
     * user's session.
     * 
     * @param session the HTTP session
     * @param name the name of the preference to retrieve
     * @return the preference value, or <code>null</code> if not found
     */
    public static String getPreference( HttpSession session, String name )
    {
        Preferences prefs = (Preferences) session.getAttribute( SESSIONPREFS );
        if( prefs != null )
        {
            return prefs.get( name );
        }
        return null;
    }

    /**
     * Returns a preference value from the Preferences object stored in the
     * user's session.
     * 
     * @param pageContext the page context
     * @param name the name of the preference to retrieve
     * @return the preference value, or <code>null</code> if not found
     */
    public static String getPreference( PageContext pageContext, String name )
    {
        return getPreference( pageContext.getSession(), name );
    }

    /**
     * Returns a preference value from the Preferences object stored in the
     * user's session.
     * 
     * @param wikiContext the wiki context
     * @param name the name of the preference to retrieve
     * @return the preference value, or <code>null</code> if not found
     */
    public static String getPreference( WikiContext wikiContext, String name )
    {
        return getPreference( wikiContext.getHttpRequest().getSession(), name );
    }

    /**
     * Saves a preference value as a cookie. If <code>key</code> or
     * <code>value</code> is <code>null</code>, the cookie is not set.
     * 
     * @param session the HTTP session
     * @param response the HTTP response
     * @param key the cookie name
     * @param value the cookie value
     */
    public static void setPreference( HttpSession session, HttpServletResponse response, String key, String value )
    {
        if( key != null && value != null )
        {
            Cookie cookie = new Cookie( key, value );
            response.addCookie( cookie );
            Preferences prefs = (Preferences) session.getAttribute( SESSIONPREFS );
            if( prefs != null )
            {
                prefs.put( key, value );
            }
        }
    }

    /**
     * A simple helper function to render a date based on the user preferences.
     * This is useful for example for all plugins.
     * 
     * @param context The context which is used to get the preferences
     * @param date The date to render.
     * @param tf In which format the date should be rendered.
     * @return A ready-rendered date.
     * @since 2.8
     */
    public static String renderDate( WikiContext context, Date date, TimeFormat tf )
    {
        DateFormat df = getDateFormat( context, tf );

        return df.format( date );
    }

    /**
     * <p>
     * This method checks to see if user preferences have been loaded from
     * request cookies, and if not, loads them. This method is called from three
     * places: from
     * {@link org.apache.wiki.ui.stripes.WikiInterceptor#intercept(net.sourceforge.stripes.controller.ExecutionContext)},
     * {@link org.apache.wiki.action.WikiContextFactory#newContext(HttpServletRequest, javax.servlet.http.HttpServletResponse, String)}
     * and
     * {@link org.apache.wiki.action.WikiContextFactory#newViewContext(HttpServletRequest, javax.servlet.http.HttpServletResponse, org.apache.wiki.api.WikiPage)}.
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
     */
    public static void setupPreferences( HttpServletRequest request )
    {
        HttpSession session = request.getSession();

        if( session.getAttribute( SESSIONPREFS ) != null )
        {
            return;
        }

        // Ok, set up the preferences now
        WikiEngine engine = WikiEngine.getInstance( session.getServletContext(), null );
        Preferences prefs = new Preferences();
        Properties props = PropertyReader.loadWebAppProps( session.getServletContext() );
        Locale locale = request.getLocale();

        // FIXME: "editor" property does not get registered, may be related with
        // http://bugs.jspwiki.org/show_bug.cgi?id=117
        // disabling it until knowing why it's happening
        // FIXME: editormanager reads jspwiki.editor -- which of both properties
        // should continue
        String defaultValue = props.getProperty( "jspwiki.defaultprefs.template.editor", "plain" );
        prefs.put( PREFS_EDITOR, getCookieValue( request, PREFS_EDITOR, defaultValue ) );

        // Init the Locale
        defaultValue = props.getProperty( "jspwiki.defaultprefs.template.language", locale.toString() );
        prefs.put( PREFS_LOCALE, getCookieValue( request, PREFS_LOCALE, defaultValue ) );

        // Init the orientation
        defaultValue = props.getProperty( "jspwiki.defaultprefs.template.orientation", Orientation.LEFT.name() );
        prefs.put( PREFS_ORIENTATION, getCookieValue( request, PREFS_ORIENTATION, defaultValue ) );

        // Init section editing
        defaultValue = props.getProperty( "jspwiki.defaultprefs.template.sectionediting", "false" );
        prefs.put( PREFS_SECTION_EDITING, getCookieValue( request, PREFS_SECTION_EDITING, defaultValue ) );

        // Init the template skin
        defaultValue = props.getProperty( "jspwiki.defaultprefs.template.skinname", "PlainVanilla" );
        prefs.put( PREFS_SKIN, getCookieValue( request, PREFS_SKIN, defaultValue ) );

        // Init the date format
        defaultValue = props.getProperty( "jspwiki.defaultprefs.template.dateformat", engine.getInternationalizationManager()
            .get( InternationalizationManager.CORE_BUNDLE, locale, "common.datetimeformat" ) );
        prefs.put( PREFS_TIME_FORMAT, getCookieValue( request, PREFS_TIME_FORMAT, defaultValue ) );

        // Init the time zone
        defaultValue = props.getProperty( "jspwiki.defaultprefs.template.timezone", java.util.TimeZone.getDefault().getID() );
        prefs.put( PREFS_TIME_ZONE, getCookieValue( request, PREFS_TIME_ZONE, defaultValue ) );

        // We're done here...
        session.setAttribute( SESSIONPREFS, prefs );
    }

    private static String getCookieValue( HttpServletRequest request, String key, String defaultValue )
    {
        String cookieValue = null;
        Cookie[] cookies = request.getCookies();
        if( cookies != null )
        {
            for( Cookie cookie : request.getCookies() )
            {
                if( key.equals( cookie.getName() ) )
                {
                    cookieValue = cookie.getValue();
                    break;
                }
            }
        }
        return cookieValue == null ? defaultValue : cookieValue;
    }

    /**
     * Returns a Map of localized time zones, with the TimeZone IDs as keys and
     * localized formatted names as the values.
     * 
     * @param request the HTTP request
     * @return map of TimeZones
     * @since 2.7.x
     */
    public static Map<String, String> listTimeZones( HttpServletRequest request )
    {
        WikiEngine engine = WikiEngine.getInstance( request.getSession().getServletContext(), null );
        LinkedHashMap<String, String> resultMap = new LinkedHashMap<String, String>();
        java.util.TimeZone serverZone = java.util.TimeZone.getDefault();
        Date now = new Date();

        // Build a map of TimeZones and their localized display names
        for( TimeZone zone : TIME_ZONES )
        {
            int offset = zone.getRawOffset() / 3600000;
            String zoneLabel = "[GMT" + (offset > 0 ? "+" : "") + offset + "] "
                               + zone.getDisplayName( zone.inDaylightTime( now ), TimeZone.LONG, request.getLocale() );
            if( serverZone.getRawOffset() == zone.getRawOffset() )
            {
                InternationalizationManager i18n = engine.getInternationalizationManager();
                String serverLabel = i18n.get( InternationalizationManager.TEMPLATES_BUNDLE, request.getLocale(),
                                               I18NSERVER_TIMEZONE );
                zoneLabel = zoneLabel + " " + serverLabel;
            }
            resultMap.put( zone.getID(), zoneLabel );
        }
        return resultMap;
    }

    /**
     * List all installed i18n language properties
     * 
     * @param request the HTTP request
     * @return map of installed Languages (with help of Juan Pablo Santos
     *         Rodriguez)
     * @since 2.7.x
     */
    public static Map<Locale, String> listLocales( HttpServletRequest request )
    {
        return LOCALES;
    }

    /**
     * List all available timeformats, read from the jspwiki.properties
     * 
     * @param context the wiki context
     * @return map of TimeFormats
     * @since 2.7.x
     */
    public static Map<String,String> listTimeFormats( WikiContext context )
    {
        Properties props = context.getEngine().getWikiProperties();
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

        String prefTimeZone = Preferences.getPreference( context.getHttpRequest().getSession(), PREFS_TIME_ZONE );
        TimeZone timeZone = TimeZone.getTimeZone( prefTimeZone );

        Date d = new Date(); // current date
        try
        {
            // dummy format pattern
            SimpleDateFormat format = Preferences.getDateFormat( context, TimeFormat.DATETIME );
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

}
