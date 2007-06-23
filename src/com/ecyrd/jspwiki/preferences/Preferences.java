package com.ecyrd.jspwiki.preferences;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.json.JSONObject;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.util.HttpUtil;

/**
 *  Represents an object which is used to store user preferences.
 *  
 *  @author jalkanen
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
     
    public static final String DELIM  = "\u00a0";

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
        WikiContext context = WikiContext.findContext( pageContext );
        
        Preferences prefs = new Preferences();
        /* FIXME: load default prefs, better read from jspwiki.properties */
        prefs.put("SkinName", "PlainVanilla" );
        prefs.put("DateFormat", "dd-MMM-yyyy HH:mm" );
        prefs.put("TimeZone", java.util.TimeZone.getDefault().getID() );
                
       // Preferences prefs = parseCookiePreferences( (HttpServletRequest) pageContext.getRequest() );
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
                    String key = (String)i.next();
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
     *  
     *  @param pageContext
     *  @param name
     *  @return
     */
    public static String getPreference( PageContext pageContext, String name )
    {
        Preferences prefs = (Preferences)pageContext.getSession().getAttribute( SESSIONPREFS );
        
        if( prefs != null )
            return (String)prefs.get( name );
        
        return null;
    }
}
