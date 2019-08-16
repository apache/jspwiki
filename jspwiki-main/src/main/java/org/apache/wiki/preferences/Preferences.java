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
package org.apache.wiki.preferences;

import com.google.gson.Gson;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiContext;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.util.HttpUtil;
import org.apache.wiki.util.PropertyReader;
import org.apache.wiki.util.TextUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TimeZone;

/**
 *  Represents an object which is used to store user preferences.
 *
 */
public class Preferences
    extends HashMap<String,String>
{
    private static final long serialVersionUID = 1L;

    /**
     *  The name under which a Preferences object is stored in the HttpSession.
     *  Its value is {@value}.
     */
    public static final String SESSIONPREFS = "prefs";

    private static Logger log = Logger.getLogger( Preferences.class );

    /**
     *  This is an utility method which is called to make sure that the
     *  JSP pages do have proper access to any user preferences.  It should be
     *  called from the commonheader.jsp.
     *  <p>
     *  This method reads user cookie preferences and mixes them up with any
     *  default preferences (and in the future, any user-specific preferences)
     *  and puts them all in the session, so that they do not have to be rewritten
     *  again.
     *  <p>
     *  This method will remember if the user has already changed his prefs.
     *
     *  @param pageContext The JSP PageContext.
     */
    public static void setupPreferences( PageContext pageContext )
    {
        //HttpSession session = pageContext.getSession();

        //if( session.getAttribute( SESSIONPREFS ) == null )
        //{
            reloadPreferences( pageContext );
        //}
    }

    /**
     *  Reloads the preferences from the PageContext into the WikiContext.
     *
     *  @param pageContext The page context.
     */
    // FIXME: The way that date preferences are chosen is currently a bit wacky: it all
    //        gets saved to the cookie based on the browser state with which the user
    //        happened to first arrive to the site with.  This, unfortunately, means that
    //        even if the user changes e.g. language preferences (like in a web cafe),
    //        the old preferences still remain in a site cookie.
    public static void reloadPreferences( PageContext pageContext )
    {
        Preferences prefs = new Preferences();
        Properties props = PropertyReader.loadWebAppProps( pageContext.getServletContext() );
        WikiContext ctx = WikiContext.findContext( pageContext );
        String dateFormat = ctx.getEngine().getInternationalizationManager().get( InternationalizationManager.CORE_BUNDLE,
                                                                                  getLocale( ctx ),
                                                                                  "common.datetimeformat" );

        prefs.put("SkinName", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.skinname", "PlainVanilla" ) );
        prefs.put("DateFormat", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.dateformat", dateFormat ) );
        prefs.put("TimeZone", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.timezone", java.util.TimeZone.getDefault().getID() ) );
        prefs.put("Orientation", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.orientation", "fav-left" ) );
        prefs.put("Sidebar", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.sidebar", "active" ) );
        prefs.put("Layout", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.layout", "fluid" ) );
        prefs.put("Language", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.language", getLocale( ctx ).toString() ) );
        prefs.put("SectionEditing", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.sectionediting", "true" ) );
        prefs.put("Appearance", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.appearance", "true" ) );

        //editor cookies
        prefs.put("autosuggest", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.autosuggest", "true" ) );
        prefs.put("tabcompletion", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.tabcompletion", "true" ) );
        prefs.put("smartpairs", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.smartpairs", "false" ) );
        prefs.put("livepreview", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.livepreview", "true" ) );
        prefs.put("previewcolumn", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.previewcolumn", "true" ) );


        // FIXME: editormanager reads jspwiki.editor -- which of both properties should continue
        prefs.put("editor", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.editor", "plain" ) );

        parseJSONPreferences( (HttpServletRequest) pageContext.getRequest(), prefs );

        pageContext.getSession().setAttribute( SESSIONPREFS, prefs );
    }


    /**
     *  Parses new-style preferences stored as JSON objects and stores them
     *  in the session.  Everything in the cookie is stored.
     *
     *  @param request
     *  @param prefs The default hashmap of preferences
     *
     */
	private static void parseJSONPreferences( HttpServletRequest request, Preferences prefs ) {
        String prefVal = TextUtil.urlDecodeUTF8( HttpUtil.retrieveCookieValue( request, "JSPWikiUserPrefs" ) );

        if( prefVal != null ) {
            // Convert prefVal JSON to a generic hashmap
            @SuppressWarnings("unchecked")
            Map<String,String> map = new Gson().fromJson(prefVal, Map.class );

            for (String key : map.keySet()) {
                key = TextUtil.replaceEntities( key );
                // Sometimes this is not a String as it comes from the Cookie set by Javascript
                Object value = map.get(key);
                if (value != null) {
                    prefs.put( key, value.toString() );
                }
            }
        }
    }

    /**
     *  Returns a preference value programmatically.
     *  FIXME
     *
     *  @param wikiContext
     *  @param name
     *  @return the preference value
     */
    public static String getPreference( WikiContext wikiContext, String name ) {
        HttpServletRequest request = wikiContext.getHttpRequest();
        if ( request == null ) return null;

        Preferences prefs = (Preferences)request.getSession().getAttribute( SESSIONPREFS );

        if( prefs != null ) {
            return prefs.get( name );
        }

        return null;
    }

    /**
     *  Returns a preference value programmatically.
     *  FIXME
     *
     *  @param pageContext
     *  @param name
     *  @return the preference value
     */
    public static String getPreference( PageContext pageContext, String name )
    {
        Preferences prefs = (Preferences)pageContext.getSession().getAttribute( SESSIONPREFS );

        if( prefs != null )
            return prefs.get( name );

        return null;
    }


    /**
     * Get Locale according to user-preference settings or the user browser locale
     *
     * @param context The context to examine.
     * @return a Locale object.
     * @since 2.8
     */
    public static Locale getLocale( WikiContext context ) {
        Locale loc = null;

        String langSetting = getPreference( context, "Language" );

        // parse language and construct valid Locale object
        if( langSetting != null ) {
            String language = "";
            String country  = "";
            String variant  = "";

            String[] res = StringUtils.split( langSetting, "-_" );

            if( res.length > 2 ) variant = res[2];
            if( res.length > 1 ) country = res[1];

            if( res.length > 0 ) {
                language = res[0];

                loc = new Locale( language, country, variant );
            }
        }

        // see if default locale is set server side
        if( loc == null ) {
            String locale = context.getEngine().getWikiProperties().getProperty( "jspwiki.preferences.default-locale" );
            try {
                loc = LocaleUtils.toLocale( locale );
            } catch( IllegalArgumentException iae ) {
                log.error( iae.getMessage() );
            }
        }

        // otherwise try to find out the browser's preferred language setting, or use the JVM's default
        if( loc == null ) {
            HttpServletRequest request = context.getHttpRequest();
            loc = ( request != null ) ? request.getLocale() : Locale.getDefault();
        }

        log.debug( "using locale "+loc.toString() );
        return loc;
    }

    /**
     *  Locates the i18n ResourceBundle given.  This method interprets
     *  the request locale, and uses that to figure out which language the
     *  user wants.
     *  @see org.apache.wiki.i18n.InternationalizationManager
     *  @param context {@link WikiContext} holding the user's locale
     *  @param bundle  The name of the bundle you are looking for.
     *  @return A localized string (or from the default language, if not found)
     *  @throws MissingResourceException If the bundle cannot be found
     */
    public static ResourceBundle getBundle( WikiContext context, String bundle )
        throws MissingResourceException
    {
        Locale loc = getLocale( context );
        InternationalizationManager i18n = context.getEngine().getInternationalizationManager();
        return i18n.getBundle( bundle, loc );
    }

    /**
     *  Get SimpleTimeFormat according to user browser locale and preferred time
     *  formats. If not found, it will revert to whichever format is set for the
     *  default
     *
     *  @param context WikiContext to use for rendering.
     *  @param tf Which version of the dateformat you are looking for?
     *  @return A SimpleTimeFormat object which you can use to render
     *  @since 2.8
     */
    public static SimpleDateFormat getDateFormat( WikiContext context, TimeFormat tf )
    {
        InternationalizationManager imgr = context.getEngine().getInternationalizationManager();
        Locale clientLocale = getLocale( context );
        String prefTimeZone = getPreference( context, "TimeZone" );
        String prefDateFormat;

        log.debug("Checking for preferences...");

        switch( tf )
        {
            case DATETIME:
                prefDateFormat = getPreference( context, "DateFormat" );
                log.debug("Preferences fmt = "+prefDateFormat);
                if( prefDateFormat == null )
                {
                    prefDateFormat = imgr.get( InternationalizationManager.CORE_BUNDLE,
                                               clientLocale,
                                               "common.datetimeformat" );
                    log.debug("Using locale-format = "+prefDateFormat);
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
     *  A simple helper function to render a date based on the user preferences.
     *  This is useful for example for all plugins.
     *
     *  @param context  The context which is used to get the preferences
     *  @param date     The date to render.
     *  @param tf       In which format the date should be rendered.
     *  @return A ready-rendered date.
     *  @since 2.8
     */
    public static String renderDate( WikiContext context, Date date, TimeFormat tf )
    {
        DateFormat df = getDateFormat( context, tf );

        return df.format( date );
    }

    /**
     *  Is used to choose between the different date formats that JSPWiki supports.
     *  <ul>
     *   <li>TIME: A time format, without  date</li>
     *   <li>DATE: A date format, without a time</li>
     *   <li>DATETIME: A date format, with a time</li>
     *  </ul>
     *
     *  @since 2.8
     */
    public enum TimeFormat
    {
        /** A time format, no date. */
        TIME,

        /** A date format, no time. */
        DATE,

        /** A date+time format. */
        DATETIME
    }
}
