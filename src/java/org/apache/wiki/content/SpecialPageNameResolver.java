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
package org.apache.wiki.content;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;

/**
 *  This PageNameResolver initializes itself from the "jspwiki.specialPage" -properties
 *  and resolves the PageName to itself if the page exists.
 */
public class SpecialPageNameResolver extends PageNameResolver
{
    /** Prefix in jspwiki.properties signifying special page keys. */
    private static final String PROP_SPECIALPAGE = "jspwiki.specialPage.";

    private static Logger log = LoggerFactory.getLogger( SpecialPageNameResolver.class );
    
    /** Private map with JSPs as keys, URIs (for absolute or relative URLs)  as values */
    private final Map<String, URI> m_specialRedirects;

    /**
     *  Constructs a SpecialPageNameResolver using the engine properties.
     *  
     *  @param engine {@inheritDoc}
     */
    public SpecialPageNameResolver( WikiEngine engine )
    {
        super( engine );
        
        m_specialRedirects = new HashMap<String, URI>();

        initSpecialPageRedirects( engine.getWikiProperties() );
    }

    @Override
    public WikiPath resolve( WikiPath name ) throws ProviderException
    {
        if( simplePageExists(name) ) return name;
        
        return null;
    }


    /**
     * Determines whether a "page" exists by examining the list of special pages
     * and querying the page manager.
     * 
     * @param page the page to seek
     * @return <code>true</code> if the page exists, <code>false</code>
     *         otherwise
     */
    protected final boolean simplePageExists( WikiPath page ) throws ProviderException
    {
        if( m_specialRedirects.containsKey( page ) )
        {
            return true;
        }
        
        return m_engine.getContentManager().pageExists( page );
    }
    
    /**
     * Skims through a supplied set of Properties and looks for anything with
     * the "special page" prefix, and creates Stripes
     * {@link net.sourceforge.stripes.action.RedirectResolution} objects for any
     * that are found.
     */
    private void initSpecialPageRedirects( Properties properties )
    {
        for( Map.Entry<Object,Object> entry : properties.entrySet() )
        {
            String key = (String) entry.getKey();
            if( key.startsWith( PROP_SPECIALPAGE ) )
            {
                String specialPage = key.substring( PROP_SPECIALPAGE.length() );
                String redirectUrl = (String) entry.getValue();
                if( specialPage != null && redirectUrl != null )
                {
                    specialPage = specialPage.trim();
                    
                    // Parse the special page
                    redirectUrl = redirectUrl.trim();
                    try
                    {
                        URI uri = new URI( redirectUrl );
                        if ( uri.getAuthority() == null )
                        {
                            // No http:// ftp:// or other authority, so it must be relative to webapp /
                            if ( !redirectUrl.startsWith( "/" ) )
                            {
                                uri = new URI( "/" + redirectUrl );
                            }
                        }
                        
                        // Add the URI for the special page
                        m_specialRedirects.put( specialPage, uri );
                    }
                    catch( URISyntaxException e )
                    {
                        // The user supplied a STRANGE reference
                        log.error( "Strange special page reference: " + redirectUrl );
                    }
                }
            }
        }
    }



    /**
     * <p>
     * If the page is a special page, this method returns an
     * a String representing the relative or absolute URL to that page;
     * otherwise, it returns <code>null</code>.
     * </p>
     * <p>
     * Special pages are non-existent references to other pages. For example,
     * you could define a special page reference "RecentChanges" which would
     * always be redirected to "RecentChanges.jsp" instead of trying to find a
     * Wiki page called "RecentChanges".
     * </p>
     */
    public final URI getSpecialPageURI( String page )
    {
        return m_specialRedirects.get( page );
    }
}
