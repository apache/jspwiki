/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ecyrd.jspwiki.WikiEngine;

public class InternationalizationManager
{
    public static final String CORE_BUNDLE = "CoreResources";
    public static final String JSPWIKI_BUNDLE = "jspwiki";
    public static final String PLUGINS_BUNDLE = "plugins";
    
    public InternationalizationManager( WikiEngine engine )
    {   
    }
    
    public String get( String key ) throws MissingResourceException
    {
        return get( JSPWIKI_BUNDLE, Locale.ENGLISH, key );
    }
    
    /**
     *  Finds a resource bundle.
     *  
     *  @param bundle The ResourceBundle to find.  Must exist.
     *  @param locale The Locale to use.  Set to null to get the default locale.
     *  @return A localized string
     */
    public ResourceBundle getBundle( String bundle, Locale locale ) throws MissingResourceException
    
    {
        if( locale == null )
            locale = Locale.getDefault();
        
        ResourceBundle b = ResourceBundle.getBundle(bundle,locale);
        
        return b;
    }
    
    /**
     *  If you are too lazy to open your own bundle, use this method
     *  to get a string simply from a bundle.
     *  @param bundle Which bundle the string is in
     *  @param locale Locale to use - null for default
     *  @param key    Which key to use.
     *  @return A localized string (or from the default language, if not found)
     */
    public String get( String bundle, Locale locale, String key ) throws MissingResourceException
    {
        return getBundle(bundle,locale).getString(key);
    }
}
