/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Category;

import com.ecyrd.jspwiki.providers.WikiPageProvider;

/**
 *  Manages the WikiPages.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class PageManager
{
    public static final String PROP_PAGEPROVIDER = "jspwiki.pageProvider";
    public static final String PROP_USECACHE     = "jspwiki.usePageCache";

    static Category log = Category.getInstance( PageManager.class );

    private WikiPageProvider m_provider;

    /**
     *  Creates a new PageManager.
     *  @throws WikiException If anything goes wrong, you get this.
     */
    public PageManager( Properties props )
        throws WikiException
    {
        String classname;

        boolean useCache = "true".equals(props.getProperty( PROP_USECACHE ));

        //
        //  If user wants to use a cache, then we'll use the CachingProvider.
        //
        if( useCache )
        {
            classname = "com.ecyrd.jspwiki.providers.CachingProvider";
        }
        else
        {
            classname = props.getProperty( PROP_PAGEPROVIDER );
        }

        try
        {
            Class providerclass = WikiEngine.findWikiClass( classname, 
                                                            "com.ecyrd.jspwiki.providers" );
            m_provider = (WikiPageProvider)providerclass.newInstance();

            log.debug("Initializing page provider class "+m_provider);
            m_provider.initialize( props );
        }
        catch( ClassNotFoundException e )
        {
            log.error("Unable to locate provider class "+classname,e);
            throw new WikiException("no provider class");
        }
        catch( InstantiationException e )
        {
            log.error("Unable to create provider class "+classname,e);
            throw new WikiException("faulty provider class");
        }
        catch( IllegalAccessException e )
        {
            log.error("Illegal access to provider class "+classname,e);
            throw new WikiException("illegal provider class");
        }
        catch( NoRequiredPropertyException e )
        {
            log.error("Provider did not found a property it was looking for: "+e.getMessage(),
                      e);
            throw e;  // Same exception works.
        }
        catch( IOException e )
        {
            log.error("An I/O exception occurred while trying to create a new page provider: "+classname, e );
            throw new WikiException("Unable to start page provider: "+e.getMessage());
        }
    }

    /**
     *  Returns the page provider currently in use.
     */
    public WikiPageProvider getProvider()
    {
        return m_provider;
    }
}
