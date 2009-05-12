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
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import java.util.*;

/**
 * Plugin for displaying pages that are not linked to in other pages.
 * Uses the ReferenceManager.
 * <p>
 *  Parameters  (from AbstractReferralPlugin):
 *  <ul>
 *  <li><b>separator</b> - how to separate generated links; default is a wikitext line break,  producing a vertical list</li>
 * <li><b> maxwidth</b> - maximum width, in chars, of generated links.</li>
 * </ul>
 *
 */
public class UnusedPagesPlugin
    extends AbstractReferralPlugin
{
    /**
     *  If set to "true", attachments are excluded from display.  Value is {@value}.
     */
    public static final String PARAM_EXCLUDEATTS = "excludeattachments";

    /**
     *  {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        ReferenceManager refmgr = context.getEngine().getReferenceManager();
        Collection<String> links = refmgr.findUnreferenced();
        //
        // filter out attachments if "excludeattachments" was requested:
        //
        String prop = (String) params.get( PARAM_EXCLUDEATTS );
        if( TextUtil.isPositive(prop) ) 
        {
            //  remove links to attachments (recognizable by a slash in it)
            //  FIXME: In 3.0, this assumption is going to fail. FIXME3.0
            Iterator iterator = links.iterator();
            while( iterator.hasNext() ) 
            {
                String link = (String) iterator.next();
                if (link.indexOf("/")!=-1) 
                {
                    iterator.remove();
                }
            }
        }

        super.initialize( context, params );

        links = filterAndSortCollection( links );

        String wikitext = null;
        
        if (m_show.equals(PARAM_SHOW_VALUE_COUNT))
        {
            wikitext = "" + links.size();
            if (m_lastModified && links.size()!=0)
            {
                wikitext = links.size() + " (" + m_dateFormat.format(m_dateLastModified) + ")";
            }
        }
        else
        {
            wikitext = wikitizeCollection( links, m_separator, ALL_ITEMS );
        }        
        return makeHTML( context, wikitext );
    }

}

