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
package org.apache.wiki.filters;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.filters.BasicPageFilter;
import org.apache.wiki.util.TextUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 *  This class is an example of how to have a simple filter.  It removes
 *  all nasty words located at <code>profanity.properties</code> file, inside 
 *  <code>org/apache/wiki/filters</code> package. The search of profanities
 *  is case unsensitive.
 *
 */
public class ProfanityFilter extends BasicPageFilter {
	
    private static final Logger log = Logger.getLogger( ProfanityFilter.class );
    
    private static final String PROPERTYFILE = "org/apache/wiki/filters/profanity.properties";
    private static String[] c_profanities = new String[0];
    
    static {
        final ClassLoader loader = ProfanityFilter.class.getClassLoader();
        try( final InputStream in = loader.getResourceAsStream( PROPERTYFILE ) ) {
            if( in == null ) {
                throw new IOException( "No property file found! (Check the installation, it should be there.)" );
            }
            try( final BufferedReader br =  new BufferedReader( new InputStreamReader( in ) ) ) {
                final List< String > profs = new ArrayList<>();

                String str;
                while ( ( str = br.readLine() ) != null ) {
                    if( str.length() > 0 && !str.startsWith( "#" ) ) { // allow comments on profanities file
                        profs.add( str );
                    }
                }
                c_profanities = profs.toArray( new String[0] );
            }
        } catch( final IOException e ) {
            log.error( "Unable to load profanities from " + PROPERTYFILE, e );
        } catch( final Exception e ) {
            log.error( "Unable to initialize Profanity Filter", e );
        }
    }

    /**
     *  {@inheritDoc}
     */
    public String preTranslate( final WikiContext context, String content ) {
        for( int i = 0; i < c_profanities.length; i++ ) {
            final String word = c_profanities[ i ];
            final String replacement = word.charAt( 0 ) + "*" + word.charAt( word.length() - 1 );

            content = TextUtil.replaceStringCaseUnsensitive( content, word, replacement );
        }

        return content;
    }

}
