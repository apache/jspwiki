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

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.action.WikiContextFactory;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;

/**
 * <p>
 * Iterator tag for the looping through the attachments of a WikiPage. In
 * addition to the <code>start</code> and <code>maxItems</code> attributes,
 * other attributes that can be set are:
 * </p>
 * <ul>
 * <li><code>page</code> - the page name to use. The default is the one
 * associated with the current WikiContext.</li>
 * </ul>
 * 
 * @see IteratorTag
 * @since 2.0
 */
public class AttachmentsIteratorTag extends IteratorTag<WikiPage>
{
    private static final long serialVersionUID = 1L;

    private static final Collection<WikiPage> EMPTY_COLLECTION = Collections.unmodifiableCollection( new ArrayList<WikiPage>() );

    static Logger log = LoggerFactory.getLogger( AttachmentsIteratorTag.class );

    /**
     * <p>
     * Returns the attachments for the current WikiContext, or an empty
     * collection. This method will return an empty collection if:
     * </p>
     * <ul>
     * <li>the attachment manager does not allow attachments</li>
     * <li>the underlying provider throws an exception</li>
     * <li>the WikiPage the current WikiContext refers to does not exist
     * <li>
     * <li>the WikiPage the current WikiContext has no attachments</li>
     * </ul>
     * 
     * @return the collection attachments
     */
    private Collection<WikiPage> getAttachments()
    {
        // Return empty collection if attachments are not enabled
        WikiContext wikiContext = WikiContextFactory.findContext( pageContext );
        WikiEngine engine = wikiContext.getEngine();
        AttachmentManager mgr = engine.getAttachmentManager();
        if( !mgr.attachmentsEnabled() )
        {
            return EMPTY_COLLECTION;
        }

        WikiPage page = wikiContext.getPage();
        try
        {
            if( page != null && engine.pageExists( page ) )
            {
                return mgr.listAttachments( page );
            }
        }
        catch( ProviderException e )
        {
            log.error( "Provider failed while trying to fetch attachments for page " + page.getName(), e );
        }
        return EMPTY_COLLECTION;
    }

    /**
     * Returns the list of attachments that will be iterated over.
     */
    protected Collection<WikiPage> initItems()
    {
        return getAttachments();
    }
}
