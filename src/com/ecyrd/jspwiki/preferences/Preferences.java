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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.json.JSONObject;

import com.ecyrd.jspwiki.PropertyReader;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.util.HttpUtil;

/**
 *  Represents an object which is used to store user preferences.
 *  
 */
public class Preferences
    extends HashMap
{
    private static final long serialVersionUID = 1L;
    
    /**
     *  The name under which a Preferences object is stored in the HttpSession.
     *  Its value is {@value}.
     */
    public static final String SESSIONPREFS = "prefs";
     
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
    
    public static void reloadPreferences( PageContext pageContext )
    {
        Preferences prefs = new Preferences();
        Properties props = PropertyReader.loadWebAppProps( pageContext.getServletContext() );
        
        prefs.put("SkinName", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.skinname", "PlainVanilla" ) );
        prefs.put("DateFormat", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.dateformat", "dd-MMM-yyyy HH:mm" ) );

        prefs.put("TimeZone", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.timezone", 
                                                          java.util.TimeZone.getDefault().getID() ) );
  /*
        prefs.put("TimeZone", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.timezone", 
                                                          Integer.toString( java.util.TimeZone.getDefault().getRawOffset() )
                                                        ) );
  */
        prefs.put("Orientation", TextUtil.getStringProperty( props, "jspwiki.defaultprefs.template.orientation", "fav-left" ) );
        
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
            return (String)prefs.get( name );
        
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
            return (String)prefs.get( name );
        
        return null;
    }

    
    /**
     * Get Locale according to user-preference settings or the user browser locale
     * 
     * @param wikiContext
     * @since 2.7.x
     */
    public static Locale getLocale(WikiContext context)
    {
        Locale loc = null;
        
        String language = Preferences.getPreference( context, "Language" );

        if( language != null)
            loc = new Locale(language);

        if( loc == null) {
            
            HttpServletRequest request = context.getHttpRequest();
            loc = ( request != null ) ? request.getLocale() : Locale.getDefault();
        }
                
        return loc;
    }

    /**
     * Get SimpleTimeFormat according to user browser locale and preferred time formats
     * 
     * @param pageContext
     * @return SimpleTimeFormat
     * @since 2.7.x
     */
    public static SimpleDateFormat getDateFormat(WikiContext context)
    {
        Locale clientLocale = Preferences.getLocale( context );
        String prefTimeZone = Preferences.getPreference( context, "TimeZone");
        String prefDateFormat = Preferences.getPreference( context, "DateFormat");

        try
        {
            TimeZone tz = TimeZone.getTimeZone(prefTimeZone);
            //TimeZone tz = TimeZone.getDefault();
            //tz.setRawOffset(Integer.parseInt(prefTimeZone));

            SimpleDateFormat fmt = new SimpleDateFormat(prefDateFormat, clientLocale);
            fmt.setTimeZone(tz);

            return fmt;
        }
        catch (Exception e)
        {
            return null;
        }
    }

}
