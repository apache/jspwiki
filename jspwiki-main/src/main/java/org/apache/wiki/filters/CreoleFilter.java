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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.filters.BasePageFilter;
import org.apache.wiki.parser.CreoleToJSPWikiTranslator;

import java.util.Properties;

/**
 * <p>Provides the Implementation for mixed mode creole: If you activate this filter, it will translate all markup that was saved as creole
 * markup to JSPWiki markup. Therefore the files will be saved with mixed markup.
 * <p>
 * <b>WARNING</b>: There's no turning back after installing this filter. Since your wiki pages are saved in Creole markup you can
 * not deactivate it afterwards.
 * <p>
 * <b>WARNING</b>: This feature is completely experimental, and is known to be broken.  Use at your own risk.
 * <p>
 * <b>WARNING</b>: The CreoleFilter feature is deprecated.  JSPWiki is likely to implement a non-mixed mode Creole at some point, since
 * turning on Creole will make new pages obsolete.
 *
 * @see <a href="http://www.wikicreole.org/wiki/MixedMode">[[WikiCreole:MixedMode]]</a> 
 */
public class CreoleFilter extends BasePageFilter {
    
    private static final Logger log = LogManager.getLogger(CreoleFilter.class);
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public void initialize( final Engine engine, final Properties props ) throws FilterException {
        super.initialize( engine, props );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String preSave( final Context wikiContext, final String content ) {
        try {
            final String username = wikiContext.getCurrentUser().getName();
            final Properties prop = m_engine.getWikiProperties();
            return new CreoleToJSPWikiTranslator().translateSignature(prop, content,username);
        } catch( final Exception e ) {
            log.error( e.getMessage(), e );
            return e.getMessage();
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String preTranslate( final Context wikiContext, final String content ) {
        try {
            final Properties prop = m_engine.getWikiProperties();
            return new CreoleToJSPWikiTranslator().translate(prop ,content);
        } catch( final Exception e ) {
            log.error( e.getMessage(), e );
            return content
                   + "\n \n %%error \n"
                   + "[CreoleFilterError]: This page was not translated by the CreoleFilter due to "
                   + "the following error: " + e.getMessage() + "\n \n"
                   + "%%\n \n";
        }
    }

}
