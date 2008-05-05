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
package com.ecyrd.jspwiki.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;

/**
 *  This class is an example of how to have a simple filter.  It removes
 *  all nasty words located at <code>profanity.properties</code> file, inside 
 *  <code>com/ecyrd/jspwiki/filters</code> package. The search of profanities
 *  is case unsensitive.
 *
 */
public class ProfanityFilter extends BasicPageFilter
{
    private static Logger     log = Logger.getLogger(ProfanityFilter.class);
    
    private static final String PROPERTYFILE = "com/ecyrd/jspwiki/filters/profanity.properties";
    private static String[] c_profanities;
    
    static 
    {
        try 
        {
            ClassLoader loader = ProfanityFilter.class.getClassLoader();
            InputStream in = loader.getResourceAsStream( PROPERTYFILE );
            
            if( in == null )
            {
                throw new IOException("No property file found! (Check the installation, it should be there.)");
            }
            
            BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
            List l_profs = new ArrayList();
            
            String str;
            while ( ( str = br.readLine() ) != null ) 
            {
                if( str.length() > 0 && !str.startsWith( "#" ) )
                { // allow comments on profanities file
                    l_profs.add( str );
                }
            }
            c_profanities = ( String[] )l_profs.toArray( new String[ l_profs.size() ] );
        }
        catch( IOException e )
        {
            log.error( "Unable to load profanities from "+PROPERTYFILE, e );
        }
        catch( Exception e )
        {
            log.error( "Unable to initialize Profanity Filter", e );
        }
    }

    public String preTranslate( WikiContext context, String content )
    {
        for( int i = 0; i < c_profanities.length; i++ )
        {
            String word = c_profanities[i];
            String replacement = word.charAt(0)+"*"+word.charAt(word.length()-1);

            content = TextUtil.replaceStringCaseUnsensitive( content, word, replacement );
        }

        return content;
    }
}
