/* 
   JSPWiki - a JSP-based WikiWiki clone.

   Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

package com.ecyrd.jspwiki.diff;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.util.ClassUtil;



/**
 * Load, initialize and delegate to the DiffProvider that will actually do the work.
 * 
 * @author John Volkar
 */
public class DifferenceManager
{
    private static final Logger log = Logger.getLogger(DifferenceManager.class);

    public static final String PROP_DIFF_PROVIDER = "jspwiki.diffProvider";
    
    
    private DiffProvider m_provider;

    public DifferenceManager(WikiEngine engine, Properties props)
    {
        loadProvider(props); 

        initializeProvider(engine, props);
        
        log.info("Using difference provider: " + m_provider.getProviderInfo());   
    }

    private void loadProvider(Properties props)
    {
        String providerClassName = props.getProperty( PROP_DIFF_PROVIDER, 
                                                      TraditionalDiffProvider.class.getName() );
        
        try
        {
            Class providerClass = ClassUtil.findClass( "com.ecyrd.jspwiki.diff", providerClassName );
            m_provider = (DiffProvider)providerClass.newInstance();
        }
        catch( ClassNotFoundException e )
        {
            log.warn("Failed loading DiffProvider, will use NullDiffProvider.", e);
        }
        catch( InstantiationException e )
        {
            log.warn("Failed loading DiffProvider, will use NullDiffProvider.", e);
        }
        catch( IllegalAccessException e )
        {
            log.warn("Failed loading DiffProvider, will use NullDiffProvider.", e);
        }
		
        if( null == m_provider )
        {
            m_provider = new DiffProvider.NullDiffProvider();
        }
    }

    
    private void initializeProvider(WikiEngine engine, Properties props)
    {
        try
        {
            m_provider.initialize( engine, props);
        }
        catch (NoRequiredPropertyException e1)
        {
            log.warn("Failed initializing DiffProvider, will use NullDiffProvider.", e1);
            m_provider = new DiffProvider.NullDiffProvider(); //doesn't need init'd
        }
        catch (IOException e1)
        {
            log.warn("Failed initializing DiffProvider, will use NullDiffProvider.", e1);
            m_provider = new DiffProvider.NullDiffProvider(); //doesn't need init'd
        }
    }

    /**
     *   Returns valid XHTML string to be used in any way you please.
     * 
     *   @return XHTML, or empty string, if no difference detected.
     */
    public String makeDiff(String firstWikiText, String secondWikiText)
    {
        String diff = m_provider.makeDiffHtml( firstWikiText, secondWikiText);
        
        if( diff == null )
            diff = "";
        
        return diff;
    }    
}

