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
        
        Preferences prefs = parseCookiePreferences( (HttpServletRequest) pageContext.getRequest() );
            
        pageContext.getSession().setAttribute( SESSIONPREFS, prefs );        
    }

    /**
     *  This is a helper method which parses the user cookie and stores
     *  the preferences into the session.
     *  <p>
     *  This should be replaced with the JSON method, below.
     *  @param request
     */
    private static Preferences parseCookiePreferences( HttpServletRequest request )
    {
        String prefSkinName = "PlainVanilla"; /* FIXME: default skin - should be settable via jspwiki.properties */
        String prefFontSize = null; 
        String prefTimeZone = java.util.TimeZone.getDefault().getID(); /* TODO */
        String prefDateFormat = "dd-MMM-yyyy HH:mm"; /* TODO should this be part of default.properties ??*/
    
        Cookie[] cookies = request.getCookies();
        if (cookies != null)
        {
          for (int i = 0; i < cookies.length; i++)
          {
             if( "JSPWikiUserPrefs".equals( cookies[i].getName() ) )
             {
                String s = TextUtil.urlDecodeUTF8 (cookies[i].getValue() ) ;
    
                java.util.StringTokenizer st = new java.util.StringTokenizer (s, DELIM);
    
                if( st.hasMoreTokens() ) prefSkinName = st.nextToken();
                if( st.hasMoreTokens() ) prefDateFormat = st.nextToken();
                if( st.hasMoreTokens() ) prefTimeZone = st.nextToken();
                if( st.hasMoreTokens() ) prefFontSize = st.nextToken();
    
                break;
             }
          }
        }
        
        Preferences prefs = new Preferences();
        
        prefs.put("SkinName",       prefSkinName );
        prefs.put("DateFormat",     prefDateFormat );
        prefs.put("TimeZone",       prefTimeZone );
        prefs.put("FontSize",       prefFontSize );
        
        return prefs;
    }

    /**
     *  Parses new-style preferences stored as JSON objects and stores them
     *  in the session.  Everything in the cookie is stored.
     *  
     *  FIXME: Not yet complete.
     *  
     *  @param request
     *  
     */
    private static void parseJSONPreferences( HttpServletRequest request )
    {
        HttpSession session = request.getSession();
        
        String prefVal = HttpUtil.retrieveCookieValue( request, "JSPWikiUserPrefs" );
        
        if( prefVal != null )
        {
            try
            {
                JSONObject jo = new JSONObject( prefVal );
    
                for( Iterator i = jo.keys(); i.hasNext(); )
                {
                    String key = (String)i.next();
                    
                    session.setAttribute( key, jo.getString(key) );
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
