/*
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

package org.apache.wiki.diff;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.util.ClassUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;


/**
 * Load, initialize and delegate to the DiffProvider that will actually do the work.
 */
public class DifferenceManager {
    private static final Logger log = Logger.getLogger( DifferenceManager.class );

    /**
     * Property value for storing a diff provider.  Value is {@value}.
     */
    public static final String PROP_DIFF_PROVIDER = "jspwiki.diffProvider";

    private DiffProvider m_provider;

    /**
     * Creates a new DifferenceManager for the given engine.
     *
     * @param engine The WikiEngine.
     * @param props  A set of properties.
     */
    public DifferenceManager( final WikiEngine engine, final Properties props ) {
        loadProvider( props );
        initializeProvider( engine, props );

        log.info( "Using difference provider: " + m_provider.getProviderInfo() );
    }

    private void loadProvider( final Properties props ) {
        final String providerClassName = props.getProperty( PROP_DIFF_PROVIDER, TraditionalDiffProvider.class.getName() );
        try {
            final Class< ? > providerClass = ClassUtil.findClass("org.apache.wiki.diff", providerClassName );
            m_provider = (DiffProvider) providerClass.getDeclaredConstructor().newInstance();
        } catch( final ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e ) {
            log.warn("Failed loading DiffProvider, will use NullDiffProvider.", e);
        }

        if( m_provider == null ) {
            m_provider = new DiffProvider.NullDiffProvider();
        }
    }


    private void initializeProvider( final WikiEngine engine, final Properties props ) {
        try {
            m_provider.initialize(engine, props);
        } catch( final NoRequiredPropertyException | IOException e1 ) {
            log.warn("Failed initializing DiffProvider, will use NullDiffProvider.", e1);
            m_provider = new DiffProvider.NullDiffProvider(); //doesn't need init'd
        }
    }

    /**
     * Returns valid XHTML string to be used in any way you please.
     *
     * @param context        The Wiki Context
     * @param firstWikiText  The old text
     * @param secondWikiText the new text
     * @return XHTML, or empty string, if no difference detected.
     */
    public String makeDiff( final WikiContext context, final String firstWikiText, final String secondWikiText ) {
        String diff;
        try {
            diff = m_provider.makeDiffHtml( context, firstWikiText, secondWikiText );

            if( diff == null ) {
                diff = "";
            }
        } catch( final Exception e ) {
            diff = "Failed to create a diff, check the logs.";
            log.warn( diff, e );
        }
        return diff;
    }
}

