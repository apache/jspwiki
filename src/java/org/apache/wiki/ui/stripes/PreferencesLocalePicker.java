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
package org.apache.wiki.ui.stripes;

import java.util.Locale;
import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.localization.LocalePicker;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.preferences.Preferences;

/**
 * This is a simple Stripes LocalePicker which uses
 * {@link Preferences#getPreferences(HttpServletRequest)} to determine the request
 * Locale.
 */
public class PreferencesLocalePicker implements LocalePicker
{
    private static final LocaleConverter LOCALE_CONVERTER = new LocaleConverter();

    /**
     * Cookie name for the user's preferred {@link java.util.Locale}.
     */
    private static final String COOKIE_LOCALE = Preferences.PREFS_LOCALE;

    /**
     * Initializes the locale picker.
     */
    public void init( Configuration configuration ) throws Exception
    {
    }

    /**
     * JSPWiki only uses UTF-8.
     */
    public String pickCharacterEncoding( HttpServletRequest request, Locale locale )
    {
        return "UTF-8";
    }

    /**
     * Picks the Locale by extracting the user's desired value from cookies,
     * then selecting the closest supported locale/country/variant combination
     * that is supported by JSPWiki. If the user's locale cookie is not set, the
     * request's locale will be checked.
     */
    public Locale pickLocale( HttpServletRequest request )
    {
        // Extract the user's cookie value (browser's request value is the fallback)
        Locale locale = null;
        Cookie[] cookies = request.getCookies();
        if( cookies != null )
        {
            for( Cookie cookie : request.getCookies() )
            {
                if( COOKIE_LOCALE.equals( cookie.getName() ) )
                {
                    locale = LOCALE_CONVERTER.convert( cookie.getValue(), Locale.class, null );
                    break;
                }
            }
        }
        if ( locale == null )
        {
            locale = request.getLocale();
        }

        // See if we can match the user's locale against one we support
        Locale match = isSupported( locale.getLanguage(), locale.getCountry(), locale.getVariant() );
        if( match == null )
        {
            match = isSupported( locale.getLanguage(), locale.getCountry(), null );
            if( match == null )
            {
                match = isSupported( locale.getLanguage(), null, null );
            }
        }
        if( match == null )
        {
            // If we can't, use the WikiEngine's default locale
            WikiEngine engine = WikiEngine.getInstance( request.getSession().getServletContext(), null );
            Properties props = engine.getWikiProperties();
            String defaultPref = props.getProperty( "jspwiki.defaultprefs.template.language", Locale.getDefault().toString() );
            match = LOCALE_CONVERTER.convert( defaultPref, Locale.class, null );
        }
        
        // Set the preferred locale in Prefs
        Preferences prefs = Preferences.getPreferences( request );
        prefs.put( Preferences.PREFS_LOCALE, match );
        return match;
    }

    /**
     * Returns true if two strings are equal. Unlike the normal equals function,
     * two strings that are both {@code null} are considered equal.
     * 
     * @param a the first string
     * @param b the second string
     * @return the result
     */
    private boolean isEqual( String a, String b )
    {
        if ( a == null ) a = "";
        if ( b == null ) b = "";
        return a.equals( b );
    }

    private Locale isSupported( String language, String country, String variant )
    {
        for( Locale locale : Preferences.AVAILABLE_LOCALES.keySet() )
        {
            boolean languageMatch = isEqual( language, locale.getLanguage() );
            boolean countryMatch = isEqual( country, locale.getCountry() );
            boolean variantMatch = isEqual( variant, locale.getVariant() );
            if( languageMatch && countryMatch && variantMatch )
            {
                return locale;
            }
        }
        return null;
    }

}
