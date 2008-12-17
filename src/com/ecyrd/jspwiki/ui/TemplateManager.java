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
package com.ecyrd.jspwiki.ui;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;

import net.sourceforge.stripes.action.ForwardResolution;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.action.WikiActionBean;
import com.ecyrd.jspwiki.action.WikiContextFactory;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;
import com.ecyrd.jspwiki.modules.ModuleManager;
import com.ecyrd.jspwiki.preferences.Preferences;
import com.ecyrd.jspwiki.preferences.Preferences.TimeFormat;
import com.ecyrd.jspwiki.ui.stripes.WikiActionBeanContext;

/**
 *  This class takes care of managing JSPWiki templates.  This class also provides
 *  the ResourceRequest mechanism.
 *
 *  @since 2.1.62
 */
public class TemplateManager
    extends ModuleManager
{
    /**
     * Enum that specifies the two types of templates: {@link #VIEW} and {@link #EDIT}.
     */
    public enum Template { 
        /** Template used for viewing things. */
        VIEW("ViewTemplate.jsp"), 
        /** Template used for editing things. */
        EDIT("EditTemplate.jsp");
        
        private final String m_template;
        
        /**
         * Package-private constructor.
         * @param template the name of the template
         */
        Template( String template )
        {
            m_template = template;
        }
        
        /**
         * Returns the JSP for the template.
         * @return
         */
        public String template()
        {
            return m_template;
        }
    }
    
    private static final String SKIN_DIRECTORY = "skins";

    /**
     * Requests a JavaScript function to be called during window.onload. Value is {@value}.
     */
    public static final String RESOURCE_JSFUNCTION = "jsfunction";

    /**
     * Requests a JavaScript associative array with all localized strings.
     */
    public static final String RESOURCE_JSLOCALIZEDSTRINGS = "jslocalizedstrings";

    /**
     * Requests a stylesheet to be inserted. Value is {@value}.
     */
    public static final String RESOURCE_STYLESHEET = "stylesheet";

    /**
     * Requests a script to be loaded. Value is {@value}.
     */
    public static final String RESOURCE_SCRIPT = "script";

    /**
     * Requests inlined CSS. Value is {@value}.
     */
    public static final String RESOURCE_INLINECSS = "inlinecss";

    /** The default directory for the properties. Value is {@value}. */
    public static final String DIRECTORY = "templates";

    /** The name of the default template. Value is {@value}. */
    public static final String DEFAULT_TEMPLATE = "default";

    /** Name of the file that contains the properties. */

    public static final String PROPERTYFILE = "template.properties";

    /** Location of I18N Resource bundles, and path prefix and suffixes */

    public static final String I18NRESOURCE_PATH = "/WEB-INF/lib/JSPWiki.jar";

    public static final String I18NRESOURCE_PREFIX = "templates/default_";

    public static final String I18NRESOURCE_SUFFIX = ".properties";

    /** I18N string to mark the default locale */

    public static final String I18NDEFAULT_LOCALE = "prefs.user.language.default";

    /** I18N string to mark the server timezone */

    public static final String I18NSERVER_TIMEZONE = "prefs.user.timezone.server";

    /** Prefix of the default timeformat properties. */

    public static final String TIMEFORMATPROPERTIES = "jspwiki.defaultprefs.timeformat.";

    /**
     * The name under which the resource includes map is stored in the
     * WikiContext.
     */
    public static final String RESOURCE_INCLUDES = "jspwiki.resourceincludes";

    // private Cache m_propertyCache;

    protected static final Logger log = LoggerFactory.getLogger(TemplateManager.class);

    /** Requests a HTTP header. Value is {@value}. */
    public static final String RESOURCE_HTTPHEADER = "httpheader";

    private WikiEngine m_engine;

    /**
     *  Creates a new TemplateManager.  There is typically one manager per engine.
     *
     *  @param engine The owning engine.
     *  @param properties The property list used to initialize this.
     */
    public TemplateManager( WikiEngine engine, Properties properties )
    {
        super(engine);
        m_engine = engine;
        //
        //  Uses the unlimited cache.
        //
        // m_propertyCache = new Cache( true, false );
    }

    /**
     *  Check the existence of a template.
     */
    // FIXME: Does not work yet
    public boolean templateExists( String templateName )
    {
        ServletContext context = m_engine.getServletContext();

        InputStream in = context.getResourceAsStream( getPath(templateName)+"ViewTemplate.jsp");

        if( in != null )
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
            }

            return true;
        }

        return false;
    }

    /**
     *  Tries to locate a given resource from the template directory. If the
     *  given resource is not found under the current name, returns the
     *  path to the corresponding one in the default template.
     *
     *  @param sContext The servlet context
     *  @param name The name of the resource
     *  @return The name of the resource which was found.
     */
    private static String findResource( ServletContext sContext, String name )
    {
        InputStream is = sContext.getResourceAsStream( name );

        if( is == null )
        {
            String defname = makeFullJSPName( DEFAULT_TEMPLATE,
                                              removeTemplatePart(name) );
            is = sContext.getResourceAsStream( defname );

            if( is != null )
                name = defname;
            else
                name = null;
        }

        if( is != null )
        {
            try
            {
                is.close();
            }
            catch( IOException e )
            {}
        }

        return name;
    }

    /**
     *  Attempts to find a resource from the given template, and if it's not found
     *  attempts to locate it from the default template.
     * @param sContext
     * @param template
     * @param name
     * @return
     */
    private static String findResource( ServletContext sContext, String template, String name )
    {
        if( name.charAt(0) == '/' )
        {
            // This is already a full path
            return findResource( sContext, name );
        }

        String fullname = makeFullJSPName( template, name );

        return findResource( sContext, fullname );
    }

    /**
     * Returns a Stripes ForwardResolution to the content page
     * @return
     */
    public ForwardResolution getContentPage( WikiActionBean actionBean, String templateJsp, String contentJsp )
    {
        WikiActionBeanContext wikiContext = actionBean.getContext();
        wikiContext.setVariable( "contentTemplate", contentJsp );
        String contentPage = findResource( wikiContext.getServletContext(), wikiContext.getTemplate(), templateJsp );
        
        ForwardResolution r = new ForwardResolution( contentPage );
        return r;
    }

    /**
     *  An utility method for finding a JSP page.  It searches only under
     *  either current context or by the absolute name.
     *
     *  @param pageContext the JSP PageContext
     *  @param name The name of the JSP page to look for (e.g "Wiki.jsp")
     *  @return The context path to the resource
     */
    public String findJSP( PageContext pageContext, String name )
    {
        ServletContext sContext = pageContext.getServletContext();

        return findResource( sContext, name );
    }

    /**
     *  Removes the template part of a name.
     */
    private static final String removeTemplatePart( String name )
    {
        int idx = 0;
        if( name.startsWith( "/" ) ) idx = 1;
        
        idx = name.indexOf('/', idx);
        if( idx != -1 )
        {
            idx = name.indexOf('/', idx+1); // Find second "/"

            if( idx != -1 )
            {
                name = name.substring( idx+1 );
            }
        }

        log.info( "Final name = "+name );
        return name;
    }

    /**
     *  Returns the full name (/templates/foo/bar) for name=bar, template=foo.
     *
     * @param template The name of the template.
     * @param name The name of the resource.
     * @return The full name for a template.
     */
    private static final String makeFullJSPName( String template, String name )
    {
        return "/"+DIRECTORY+"/"+template+"/"+name;
    }

    /**
     *  Attempts to locate a resource under the given template.  If that template
     *  does not exist, or the page does not exist under that template, will
     *  attempt to locate a similarly named file under the default template.
     *  <p>
     *  Even though the name suggests only JSP files can be located, but in fact
     *  this method can find also other resources than JSP files.
     *
     *  @param pageContext The JSP PageContext
     *  @param template From which template we should seek initially?
     *  @param name Which resource are we looking for (e.g. "ViewTemplate.jsp")
     *  @return path to the JSP page; null, if it was not found.
     */
    public String findJSP( PageContext pageContext, String template, String name )
    {
        if( name == null || template == null )
        {
            log.error("findJSP() was asked to find a null template or name ("+template+","+name+")."+
                      " JSP page '"+
                      ((HttpServletRequest)pageContext.getRequest()).getRequestURI()+"'");
            throw new InternalWikiException("Illegal arguments to findJSP(); please check logs.");
        }

        return findResource( pageContext.getServletContext(), template, name );
    }

    /**
     *  Attempts to locate a resource under the given template.  This matches the
     *  functionality findJSP(), but uses the WikiContext as the argument.  If there
     *  is no servlet context (i.e. this is embedded), will just simply return
     *  a best-guess.
     *  <p>
     *  This method is typically used to locate any resource, including JSP pages, images,
     *  scripts, etc.
     *
     *  @since 2.6
     *  @param ctx the wiki context
     *  @param template the name of the template to use
     *  @param name the name of the resource to fine
     *  @return the path to the resource
     */
    public String findResource( WikiContext ctx, String template, String name )
    {
        if( m_engine.getServletContext() != null )
        {
            return findResource( m_engine.getServletContext(), template, name );
        }

        return getPath(template)+"/"+name;
    }

    /**
     *  Returns a property, as defined in the template.  The evaluation
     *  is lazy, i.e. the properties are not loaded until the template is
     *  actually used for the first time.
     */
    /*
    public String getTemplateProperty( WikiContext context, String key )
    {
        String template = context.getTemplate();

        try
        {
            Properties props = (Properties)m_propertyCache.getFromCache( template, -1 );

            if( props == null )
            {
                try
                {
                    props = getTemplateProperties( template );

                    m_propertyCache.putInCache( template, props );
                }
                catch( IOException e )
                {
                    log.warn("IO Exception while reading template properties",e);

                    return null;
                }
            }

            return props.getProperty( key );
        }
        catch( NeedsRefreshException ex )
        {
            // FIXME
            return null;
        }
    }
*/
    /**
     *  Returns an absolute path to a given template.
     */
    private static final String getPath( String template )
    {
        return "/"+DIRECTORY+"/"+template+"/";
    }

    /**
     *   Lists the skins available under this template.  Returns an
     *   empty Set, if there are no extra skins available.  Note that
     *   this method does not check whether there is anything actually
     *   in the directories, it just lists them.  This may change
     *   in the future.
     *
     *   @param pageContext the JSP PageContext
     *   @param template The template to search
     *   @return Set of Strings with the skin names.
     *   @since 2.3.26
     */
    @SuppressWarnings("unchecked")
    public Set listSkins( PageContext pageContext, String template )
    {
        String place = makeFullJSPName( template, SKIN_DIRECTORY );

        ServletContext sContext = pageContext.getServletContext();

        Set<String> skinSet = sContext.getResourcePaths( place );
        TreeSet<String> resultSet = new TreeSet<String>();

        if( log.isDebugEnabled() ) log.debug( "Listings skins from "+place );

        if( skinSet != null )
        {
            String[] skins = {};

            skins = skinSet.toArray(skins);

            for (int i = 0; i < skins.length; i++)
            {
                String[] s = StringUtils.split(skins[i], "/");

                if (s.length > 2 && skins[i].endsWith("/"))
                {
                    String skinName = s[s.length - 1];
                    resultSet.add(skinName);
                    if (log.isDebugEnabled())
                        log.debug("...adding skin '" + skinName + "'");
                }
            }
        }

        return resultSet;
    }

    /**
     * List all installed i18n language properties
     * 
     * @param pageContext
     * @return map of installed Languages (with help of Juan Pablo Santos Rodriguez)
     * @since 2.7.x
     */
    public Map listLanguages(PageContext pageContext)
    {
        LinkedHashMap<String,String> resultMap = new LinkedHashMap<String,String>();

        String clientLanguage = ((HttpServletRequest) pageContext.getRequest()).getLocale().toString();
        JarInputStream jarStream = null;
        
        try
        {
            JarEntry entry;
            InputStream inputStream = pageContext.getServletContext().getResourceAsStream(I18NRESOURCE_PATH);
            jarStream = new JarInputStream(inputStream);

            while ((entry = jarStream.getNextJarEntry()) != null)
            {
                String name = entry.getName();

                if (!entry.isDirectory() && name.startsWith(I18NRESOURCE_PREFIX) && name.endsWith(I18NRESOURCE_SUFFIX))
                {
                    name = name.substring(I18NRESOURCE_PREFIX.length(), name.lastIndexOf(I18NRESOURCE_SUFFIX));

                    Locale locale = new Locale(name.substring(0, 2), ((name.indexOf("_") == -1) ? "" : name.substring(3, 5)));

                    String defaultLanguage = "";

                    if (clientLanguage.startsWith(name))
                    {
                        defaultLanguage = LocaleSupport.getLocalizedMessage(pageContext, I18NDEFAULT_LOCALE);
                    }

                    resultMap.put(name, locale.getDisplayName(locale) + " " + defaultLanguage);
                }
            }
        }
        catch (IOException ioe)
        {
            if (log.isDebugEnabled())
                log.debug("Could not search jar file '" + I18NRESOURCE_PATH + 
                          "'for properties files due to an IOException: \n" + ioe.getMessage());
        }
        finally
        {
            if( jarStream != null ) 
            {
                try 
                { 
                    jarStream.close(); 
                } 
                catch(IOException e) {}
            }
        }
        
        return resultMap;
    }


    /**
     * List all available timeformats, read from the jspwiki.properties
     * 
     * @param pageContext
     * @return map of TimeFormats
     * @since 2.7.x
     */
    public Map listTimeFormats(PageContext pageContext)
    {
        WikiContext context = WikiContextFactory.findContext( pageContext ); 
        Properties props = m_engine.getWikiProperties();
        ArrayList<String> tfArr = new ArrayList<String>(40);
        LinkedHashMap<String,String> resultMap = new LinkedHashMap<String,String>();

        /* filter timeformat properties */
        for (Enumeration e = props.propertyNames(); e.hasMoreElements();)
        {
            String name = (String) e.nextElement();

            if (name.startsWith(TIMEFORMATPROPERTIES))
            {
                tfArr.add(name);
            }
        }

        /* fetch actual formats */
        if (tfArr.size() == 0) /*
                                 * no props found - make sure some default
                                 * formats are avail
                                 */
        {
            tfArr.add("dd-MMM-yy");
            tfArr.add("d-MMM-yyyy");
            tfArr.add("EEE, dd-MMM-yyyy, zzzz");
        }
        else
        {
            Collections.sort(tfArr);

            for (int i = 0; i < tfArr.size(); i++)
            {
                tfArr.set(i, props.getProperty(tfArr.get(i)));
            }
        }

        String prefTimeZone = Preferences.getPreference( context, "TimeZone" );
        //TimeZone tz = TimeZone.getDefault();
        TimeZone tz = TimeZone.getTimeZone(prefTimeZone);
        /*try
        {
            tz.setRawOffset(Integer.parseInt(prefTimeZone));
        }
        catch (Exception e)
        {
        }*/

        Date d = new Date(); // current date
        try
        {
            // dummy format pattern
            SimpleDateFormat fmt = Preferences.getDateFormat( context, TimeFormat.DATETIME );
            fmt.setTimeZone(tz);

            for (int i = 0; i < tfArr.size(); i++)
            {
                try
                {
                    String f = tfArr.get(i);
                    fmt.applyPattern(f);

                    resultMap.put(f, fmt.format(d));
                }
                catch (IllegalArgumentException e)
                {
                } // skip parameter
            }
        }
        catch (IllegalArgumentException e)
        {
        } // skip parameter

        return resultMap;
    }

    /**
     * List all timezones, with special marker for server timezone
     * 
     * @param pageContext
     * @return map of TimeZones
     * @since 2.7.x
     */
    public Map listTimeZones(PageContext pageContext)
    {
        LinkedHashMap<String,String> resultMap = new LinkedHashMap<String,String>();

        String[][] tzs = { { "GMT-12", "Enitwetok, Kwajalien" },
                          { "GMT-11", "Nome, Midway Island, Samoa" }, 
                          { "GMT-10", "Hawaii" },
                          { "GMT-9", "Alaska" }, 
                          { "GMT-8", "Pacific Time" },
                          { "GMT-7", "Mountain Time" }, 
                          { "GMT-6", "Central Time, Mexico City" },
                          { "GMT-5", "Eastern Time, Bogota, Lima, Quito" },
                          { "GMT-4", "Atlantic Time, Caracas, La Paz" }, 
                          { "GMT-3:30", "Newfoundland" },
                          { "GMT-3", "Brazil, Buenos Aires, Georgetown, Falkland Is." },
                          { "GMT-2", "Mid-Atlantic, Ascention Is., St Helena" },
                          { "GMT-1", "Azores, Cape Verde Islands" },
                          { "GMT", "Casablanca, Dublin, Edinburgh, London, Lisbon, Monrovia" },
                          { "GMT+1", "Berlin, Brussels, Copenhagen, Madrid, Paris, Rome" },
                          { "GMT+2", "Helsinki, Athens, Kaliningrad, South Africa, Warsaw" },
                          { "GMT+3", "Baghdad, Riyadh, Moscow, Nairobi" }, 
                          { "GMT+3:30", "Tehran" },
                          { "GMT+4", "Adu Dhabi, Baku, Muscat, Tbilisi" }, 
                          { "GMT+4:30", "Kabul" },
                          { "GMT+5", "Islamabad, Karachi, Tashkent" },
                          { "GMT+5:30", "Bombay, Calcutta, Madras, New Delhi" },
                          { "GMT+6", "Almaty, Colomba, Dhakra" }, 
                          { "GMT+7", "Bangkok, Hanoi, Jakarta" },
                          { "GMT+8", "Beijing, Hong Kong, Perth, Singapore, Taipei" },
                          { "GMT+9", "Osaka, Sapporo, Seoul, Tokyo, Yakutsk" },
                          { "GMT+9:30", "Adelaide, Darwin" },
                          { "GMT+10", "Melbourne, Papua New Guinea, Sydney, Vladivostok" },
                          { "GMT+11", "Magadan, New Caledonia, Solomon Islands" },
                          { "GMT+12", "Auckland, Wellington, Fiji, Marshall Island" } };

        java.util.TimeZone servertz = java.util.TimeZone.getDefault();

        for( int i = 0; i < tzs.length; i++ )
        {
            String tzID = tzs[i][0];
            java.util.TimeZone tz = java.util.TimeZone.getTimeZone(tzID);
            
            String serverTimeZone = "";

            if( servertz.getRawOffset() == tz.getRawOffset() )
            {
                serverTimeZone = LocaleSupport.getLocalizedMessage(pageContext, I18NSERVER_TIMEZONE);
                tzID = servertz.getID(); 
            }

            resultMap.put(tzID, "(" + tzs[i][0] + ") "+tzs[i][1] + " " + serverTimeZone);            
        }

        return resultMap;
    }

    /**
     *  Always returns a valid property map.
     */
    /*
    private Properties getTemplateProperties( String templateName )
        throws IOException
    {
        Properties p = new Properties();

        ServletContext context = m_engine.getServletContext();

        InputStream propertyStream = context.getResourceAsStream(getPath(templateName)+PROPERTYFILE);

        if( propertyStream != null )
        {
            p.load( propertyStream );

            propertyStream.close();
        }
        else
        {
            log.debug("Template '"+templateName+"' does not have a propertyfile '"+PROPERTYFILE+"'.");
        }

        return p;
    }
*/
    /**
     *  Returns the include resources marker for a given type.  This is in a
     *  HTML or Javascript comment format.
     *
     *  @param context the wiki context
     *  @param type the marker
     *  @return the generated marker comment
     */
    public static String getMarker(WikiContext context, String type )
    {
        if( type.equals(RESOURCE_JSLOCALIZEDSTRINGS) )
        {
            return getJSLocalizedStrings( context );
        }
        else if( type.equals(RESOURCE_JSFUNCTION) )
        {
            return "/* INCLUDERESOURCES ("+type+") */";
        }
        return "<!-- INCLUDERESOURCES ("+type+") -->";
    }

    /**
     *  Extract all i18n strings in the javascript domain. (javascript.*)
     *  Returns a javascript snippet which defines the LoacalizedStings array.
     *
     *  @param wiki context
     *  @return Javascript snippet which defines the LocaliedStrings array
     *  @author Dirk Frederickx
     *  @since 2.5.108
     */
    private static String getJSLocalizedStrings( WikiContext context )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "var LocalizedStrings = {\n");

        ResourceBundle rb = context.getBundle("templates.default");

        boolean first = true;

        for( Enumeration en = rb.getKeys(); en.hasMoreElements(); )
        {
            String key = (String)en.nextElement();

            if( key.startsWith("javascript") )
            {
                if( first )
                {
                    first = false;
                }
                else
                {
                    sb.append( ",\n" );
                }
                sb.append( "\""+key+"\":\""+rb.getString(key)+"\"" );
            }
        }
        sb.append("\n};\n");

        return( sb.toString() );
    }

    /**
     *  Adds a resource request to the current request context.
     *  The content will be added at the resource-type marker
     *  (see IncludeResourcesTag) in WikiJSPFilter.
     *  <p>
     *  The resources can be of different types.  For RESOURCE_SCRIPT and RESOURCE_STYLESHEET
     *  this is an URI path to the resource (a script file or an external stylesheet)
     *  that needs to be included.  For RESOURCE_INLINECSS
     *  the resource should be something that can be added between &lt;style>&lt;/style> in the
     *  header file (commonheader.jsp).  For RESOURCE_JSFUNCTION it is the name of the Javascript
     *  function that should be run at page load.
     *  <p>
     *  The IncludeResourceTag inserts code in the template files, which is then filled
     *  by the WikiFilter after the request has been rendered but not yet sent to the recipient.
     *  <p>
     *  Note that ALL resource requests get rendered, so this method does not check if
     *  the request already exists in the resources.  Therefore, if you have a plugin which
     *  makes a new resource request every time, you'll end up with multiple resource requests
     *  rendered.  It's thus a good idea to make this request only once during the page
     *  life cycle.
     *
     *  @param ctx The current wiki context
     *  @param type What kind of a request should be added?
     *  @param resource The resource to add.
     */
    @SuppressWarnings("unchecked")
    public static void addResourceRequest( WikiContext ctx, String type, String resource )
    {
        HashMap<String,Vector<String>> resourcemap = (HashMap<String,Vector<String>>) ctx.getVariable( RESOURCE_INCLUDES );

        if( resourcemap == null )
        {
            resourcemap = new HashMap<String,Vector<String>>();
        }

        Vector<String> resources = resourcemap.get( type );

        if( resources == null )
        {
            resources = new Vector<String>();
        }

        String resourceString = null;

        if( type.equals(RESOURCE_SCRIPT) )
        {
            resourceString = "<script type='text/javascript' src='"+resource+"'></script>";
        }
        else if( type.equals(RESOURCE_STYLESHEET) )
        {
            resourceString = "<link rel='stylesheet' type='text/css' href='"+resource+"' />";
        }
        else if( type.equals(RESOURCE_INLINECSS) )
        {
            resourceString = "<style type='text/css'>\n"+resource+"\n</style>\n";
        }
        else if( type.equals(RESOURCE_JSFUNCTION) )
        {
            resourceString = resource;
        }
        else if( type.equals(RESOURCE_HTTPHEADER) )
        {
            resourceString = resource;
        }

        if( resourceString != null )
        {
            resources.add( resourceString );
        }

        log.debug("Request to add a resource: "+resourceString);

        resourcemap.put( type, resources );
        ctx.setVariable( RESOURCE_INCLUDES, resourcemap );
    }

    /**
     *  Returns resource requests for a particular type.  If there are no resources,
     *  returns an empty array.
     *
     *  @param ctx WikiContext
     *  @param type The resource request type
     *  @return a String array for the resource requests
     */

    @SuppressWarnings("unchecked")
    public static String[] getResourceRequests( WikiContext ctx, String type )
    {
        HashMap<String,Vector<String>> hm = (HashMap<String,Vector<String>>) ctx.getVariable( RESOURCE_INCLUDES );

        if( hm == null ) return new String[0];

        Vector<String> resources = hm.get( type );

        if( resources == null ) return new String[0];

        String[] res = new String[resources.size()];

        return resources.toArray( res );
    }

    /**
     *  Returns all those types that have been requested so far.
     *
     * @param ctx the wiki context
     * @return the array of types requested
     */
    @SuppressWarnings("unchecked")
    public static String[] getResourceTypes( WikiContext ctx )
    {
        String[] res = new String[0];

        if( ctx != null )
        {
            HashMap<String,String> hm = (HashMap<String,String>) ctx.getVariable( RESOURCE_INCLUDES );

            if( hm != null )
            {
                Set<String> keys = hm.keySet();

                res = keys.toArray( res );
            }
        }

        return res;
    }

    /**
     *  Returns an empty collection, since at the moment the TemplateManager
     *  does not manage any modules.
     *
     *  @return {@inheritDoc}
     */
    public Collection modules()
    {
        return new ArrayList();
    }
}
