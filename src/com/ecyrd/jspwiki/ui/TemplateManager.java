/*
 JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2003-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.modules.ModuleManager;

/**
 *  This class takes care of managing JSPWiki templates.  This class also provides
 *  the ResourceRequest mechanism.
 *
 *  @since 2.1.62
 *  @author Janne Jalkanen
 */
public class TemplateManager
    extends ModuleManager
{
    private static final String SKIN_DIRECTORY = "skins";

    /**
     * Requests a JavaScript function to be called during window.onload. Value is {@value}.
     */
    public static final String RESOURCE_JSFUNCTION = "jsfunction";

    /**
     * Requests a stylesheet to be inserted. Value is {@value}.
     */
    public static final String RESOURCE_STYLESHEET = "stylesheet";

    /**
     * Requests a script to be loaded.  Value is {@value}.
     */
    public static final String RESOURCE_SCRIPT     = "script";

    /**
     *  Requests inlined CSS. Value is {@value}.
     */
    public static final String RESOURCE_INLINECSS  = "inlinecss";

    /** The default directory for the properties. Value is {@value}. */
    public static final String DIRECTORY           = "templates";

    /** The name of the default template.  Value is {@value}. */
    public static final String DEFAULT_TEMPLATE    = "default";

    /** Name of the file that contains the properties.*/

    public static final String PROPERTYFILE        = "template.properties";

    /** The name under which the resource includes map is stored in the WikiContext. */
    public static final String RESOURCE_INCLUDES   = "jspwiki.resourceincludes";

    // private Cache              m_propertyCache;

    protected static final Logger log = Logger.getLogger( TemplateManager.class );

    /** Requests a HTTP header.  Value is {@value}. */
    public static final String RESOURCE_HTTPHEADER = "httpheader";

    /**
     *  Creates a new TemplateManager.  There is typically one manager per engine.
     *  
     *  @param engine The owning engine.
     *  @param properties The property list used to initialize this.
     */
    public TemplateManager( WikiEngine engine, Properties properties )
    {
        super(engine);

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
            catch( IOException e ) {}

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
        int idx = name.indexOf('/');
        if( idx != -1 )
        {
            idx = name.indexOf('/', idx); // Find second "/"

            if( idx != -1 )
            {
                return name.substring( idx+1 );
            }
        }

        return name;
    }

    /**
     *  Returns the full name (/templates/foo/bar) for name=bar, template=foo.
     *  
     * @param template
     * @param name
     * @return
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
            log.fatal("findJSP() was asked to find a null template or name ("+template+","+name+")."+
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
    public Set listSkins( PageContext pageContext, String template )
    {
        String place = makeFullJSPName( template, SKIN_DIRECTORY );

        ServletContext sContext = pageContext.getServletContext();

        Set skinSet = sContext.getResourcePaths( place );
        TreeSet resultSet = new TreeSet();

        if( log.isDebugEnabled() ) log.debug( "Listings skins from "+place );

        if( skinSet != null )
        {
            String[] skins = {};

            skins = (String[]) skinSet.toArray(skins);

            for( int i = 0; i < skins.length; i++ )
            {
                String[] s = StringUtils.split(skins[i],"/");

                if( s.length > 2 && skins[i].endsWith("/") )
                {
                    String skinName = s[s.length-1];
                    resultSet.add( skinName );
                    if( log.isDebugEnabled() ) log.debug("...adding skin '"+skinName+"'");
                }
            }
        }

        return resultSet;
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
     *  HTML comment format.
     *
     *  @param type the marker
     *  @return the generated marker comment
     */
    public static String getMarker( String type )
    {
        if( type.equals(RESOURCE_JSFUNCTION) )
        {
            return "/* INCLUDERESOURCES ("+type+") */";
        }
        return "<!-- INCLUDERESOURCES ("+type+") -->";
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
    public static void addResourceRequest( WikiContext ctx, String type, String resource )
    {
        HashMap resourcemap = (HashMap) ctx.getVariable( RESOURCE_INCLUDES );

        if( resourcemap == null )
        {
            resourcemap = new HashMap();
        }

        Vector resources = (Vector) resourcemap.get( type );

        if( resources == null )
        {
            resources = new Vector();
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

    public static String[] getResourceRequests( WikiContext ctx, String type )
    {
        HashMap hm = (HashMap) ctx.getVariable( RESOURCE_INCLUDES );

        if( hm == null ) return new String[0];

        Vector resources = (Vector) hm.get( type );

        if( resources == null ) return new String[0];

        String[] res = new String[resources.size()];

        return (String[]) resources.toArray( res );
    }

    /**
     *  Returns all those types that have been requested so far.
     *
     * @param ctx the wiki context
     * @return the array of types requested
     */
    public static String[] getResourceTypes( WikiContext ctx )
    {
        String[] res = new String[0];

        if( ctx != null )
        {
            HashMap hm = (HashMap) ctx.getVariable( RESOURCE_INCLUDES );

            if( hm != null )
            {
                Set keys = hm.keySet();

                res = (String[]) keys.toArray( res );
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
