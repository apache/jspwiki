package com.ecyrd.jspwiki.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ecyrd.jspwiki.WikiEngine;

public class InternationalizationManager
{
    public static final String JSPWIKI_BUNDLE = "jspwiki";
    public static final String PLUGINS_BUNDLE = "plugins";
    
    public InternationalizationManager( WikiEngine engine )
    {   
    }
    
    public String get( String key )
    {
        return get( JSPWIKI_BUNDLE, Locale.ENGLISH, key );
    }
    
    public ResourceBundle getBundle( String bundle, Locale locale )
    {
        ResourceBundle b = ResourceBundle.getBundle(bundle,locale);
        
        return b;
    }
    
    public String get( String bundle, Locale locale, String key )
    {
        return getBundle(bundle,locale).getString(key);
    }
}
