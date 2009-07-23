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
package org.apache.wiki.content.resolver;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.TextUtil;

/**
 *  A resolver for English language plurals (matches "PageName" to "PageNames" and
 *  vice versa).  If the page does not exist, returns <code>null</code>.
 */
public class EnglishPluralsPageNameResolver extends PageNameResolver
{
    /** If true, we'll also consider english plurals (+s) a match. */
    private boolean m_matchEnglishPlurals;

 
    public EnglishPluralsPageNameResolver( WikiEngine engine )
    {
        super( engine );
        

        // Do we match plurals?
        m_matchEnglishPlurals = TextUtil.getBooleanProperty( engine.getWikiProperties(), 
                                                             WikiEngine.PROP_MATCHPLURALS, 
                                                             true );
    }

    @Override
    public WikiPath resolve( WikiPath name ) throws ProviderException
    {
        ContentManager mgr = m_engine.getContentManager();
        
        if( mgr.pageExists( name ) ) return name;
        
        if( m_matchEnglishPlurals )
        {
            WikiPath alternativeName;
            
            if( name.getPath().endsWith( "s" ) )
            {
                alternativeName = new WikiPath( name.getSpace(), 
                                                name.getPath().substring( 0, name.getPath().length()-1) );
            }
            else
            {
                alternativeName = new WikiPath( name.getSpace(),
                                                name.getPath()+"s" );
            }
            
            if( mgr.pageExists( alternativeName ) )
                return alternativeName;
        }
        
        return null;
    }

}
