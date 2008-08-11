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
package com.ecyrd.jspwiki.preferences;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.PropertyReader;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.i18n.InternationalizationManager;
import com.ecyrd.jspwiki.util.HttpUtil;

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
        HttpSession session = pageContext.getSession();

        if( session.getAttribute( SESSIONPREFS ) == null )
        {
            reloadPreferences( pageContext );
        }
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
        
        prefs.put("SkinName", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.skinname", "PlainVanilla" ) );
        prefs.put("DateFormat", 
                  TextUtil.getStringProperty( props, 
                                              "jspwiki.defaultprefs.template.dateformat", 
                                              ctx.getEngine().getInternationalizationManager().get( InternationalizationManager.CORE_BUNDLE, 
                                                                                                    getLocale( ctx ), 
                                                                                                    "common.datetimeformat" ) ) );

        prefs.put("TimeZone", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.timezone", 
                                                          java.util.TimeZone.getDefault().getID() ) );

        prefs.put("Orientation", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.orientation", "fav-left" ) );
        
        prefs.put("Language", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.language",
                                                          getLocale( ctx ).toString() ) );

        // FIXME: "editor" property does not get registered, may be related with http://bugs.jspwiki.org/show_bug.cgi?id=117
        // disabling it until knowing why it's happening
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
    private static void parseJSONPreferences( HttpServletRequest request, Preferences prefs )
    {
        //FIXME: urlDecodeUTF8 should better go in HttpUtil ??
        String prefVal = TextUtil.urlDecodeUTF8( HttpUtil.retrieveCookieValue( request, "JSPWikiUserPrefs" ) );
        
        if( prefVal != null )
        {
            try
            {
                JSONObject jo = new JSONObject( prefVal );
    
                for( Iterator i = jo.keys(); i.hasNext(); )
                {
                    String key = TextUtil.replaceEntities( (String)i.next() );
                    prefs.put(key, jo.getString(key) );
                }
            }
            catch( ParseException e )
            {
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
    public static String getPreference( WikiContext wikiContext, String name )
    {
        HttpServletRequest request = wikiContext.getHttpRequest();
        if ( request == null ) return null;
        
        Preferences prefs = (Preferences)request.getSession().getAttribute( SESSIONPREFS );
        
        if( prefs != null )
            return prefs.get( name );
        
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
    public static Locale getLocale(WikiContext context)
    {
        Locale loc = null;
        
        String language = Preferences.getPreference( context, "Language" );

        if( language != null)
            loc = new Locale(language);

        if( loc == null) 
        {    
            HttpServletRequest request = context.getHttpRequest();
            loc = ( request != null ) ? request.getLocale() : Locale.getDefault();
        }
                
        return loc;
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
        Locale clientLocale = Preferences.getLocale( context );
        String prefTimeZone = Preferences.getPreference( context, "TimeZone" );
        String prefDateFormat;
        
        log.debug("Checking for preferences...");
        
        switch( tf )
        {
            case DATETIME:
                prefDateFormat = Preferences.getPreference( context, "DateFormat" );
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
