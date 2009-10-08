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
package org.apache.wiki.tags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;

/**
 * <p>
 * Iterator tag that loops through the historical versions of a WikiPage. In
 * addition to the <code>start</code> and <code>maxItems</code> attributes,
 * other attributes that can be set are:
 * </p>
 * <ul>
 * <li><code>page</code> - the page name to use. The default is the one
 * associated with the current WikiContext.</li>
 * </ul>
 * 
 * @since 2.0
 */
public class HistoryIteratorTag extends IteratorTag<WikiPage>
{
    private static final long serialVersionUID = 1L;

    private static final Collection<WikiPage> EMPTY_COLLECTION = Collections.unmodifiableCollection( new ArrayList<WikiPage>() );

   private  static final Logger log = LoggerFactory.getLogger( HistoryIteratorTag.class );

    /**
     * Returns the historical versions of the current WikiPage.
     * @return a collection of {@link org.apache.wiki.api.WikiPage} objects
     */
    @Override
    protected Collection<WikiPage> initItems()
    {
        WikiPage page = m_wikiContext.getPage();
        WikiEngine engine = m_wikiContext.getEngine();
        try
        {
            if( page != null && engine.pageExists( page ) )
            {
                return engine.getVersionHistory( page.getName() );
            }
        }
        catch( PageNotFoundException e )
        {
            log.error( "Provider claims page " + page.getName() + " doesn't exist, right after it said it did. This is odd!", e );
        }
        catch( ProviderException e )
        {
            log.error( "Provider failed while trying to fetch history for page " + page.getName(), e );
        }
        return EMPTY_COLLECTION;
    }
}
