/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki;

import java.util.Properties;
import java.util.Hashtable;
import java.io.InputStream;
import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import com.opensymphony.module.oscache.base.Cache;
import com.opensymphony.module.oscache.base.NeedsRefreshException;

/**
 *  This class takes care of managing JSPWiki templates.
 *
 *  @since 2.1.62
 *  @author Janne Jalkanen
 */
public class TemplateManager
{
    /** The default directory for the properties. */
    public static final String DIRECTORY    = "templates";

    /** Name of the file that contains the properties.*/

    public static final String PROPERTYFILE = "template.properties";

    private WikiEngine         m_engine;
    private Cache              m_propertyCache;

    protected static Logger log = Logger.getLogger( TemplateManager.class );

    public TemplateManager( WikiEngine engine, Properties properties )
    {
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
            catch( IOException e ) {}

            return true;
        }

        return false;
    }


    /**
     *  Returns a property, as defined in the template.  The evaluation
     *  is lazy, i.e. the properties are not loaded until the template is
     *  actually used for the first time.
     */
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

    private static final String getPath( String template )
    {
        return "/"+DIRECTORY+"/"+template+"/";
    }

    /**
     *  Check if the property map requires refresh.
     */

    private boolean checkRefresh( String templateName )
    {
        // CacheEntry entry = m_propertyCache.get();
        return false;
    }

    /**
     *  Always returns a valid property map.
     */
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
}
