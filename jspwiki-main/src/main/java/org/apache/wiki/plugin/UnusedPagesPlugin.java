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
package org.apache.wiki.plugin;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.references.ReferenceManager;
import org.apache.wiki.util.TextUtil;

import java.util.Collection;
import java.util.Map;

/**
 * Plugin for displaying pages that are not linked to in other pages.
 * Uses the ReferenceManager.
 * <p>
 *  Parameters  (from AbstractReferralPlugin):
 *  <ul>
 *  <li><b>separator</b> - how to separate generated links; default is a wikitext line break,  producing a vertical list</li>
 * <li><b> maxwidth</b> - maximum width, in chars, of generated links.</li>
 * </ul>
 */
public class UnusedPagesPlugin extends AbstractReferralPlugin {

    /** If set to "true", attachments are excluded from display.  Value is {@value}. */
    public static final String PARAM_EXCLUDEATTS = "excludeattachments";

    /**
     *  {@inheritDoc}
     */
    @Override public String execute( final WikiContext context, final Map< String, String > params ) throws PluginException {
        final ReferenceManager refmgr = context.getEngine().getManager( ReferenceManager.class );
        Collection< String > links = refmgr.findUnreferenced();

        // filter out attachments if "excludeattachments" was requested:
        final String prop = params.get( PARAM_EXCLUDEATTS );
        if( TextUtil.isPositive( prop ) ) {
            // remove links to attachments (recognizable by a slash in it)
            links.removeIf( link -> link.contains( "/" ) );
        }

        super.initialize( context, params );
        links = filterAndSortCollection( links );

        String wikitext;
        if( m_show.equals( PARAM_SHOW_VALUE_COUNT ) ) {
            wikitext = "" + links.size();
            if( m_lastModified && links.size() != 0 ) {
                wikitext = links.size() + " (" + m_dateFormat.format( m_dateLastModified ) + ")";
            }
        } else {
            wikitext = wikitizeCollection( links, m_separator, ALL_ITEMS );
        }
        return makeHTML( context, wikitext );
    }

}

