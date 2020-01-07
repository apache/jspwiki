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
package org.apache.wiki.tags;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;

import java.io.IOException;

/**
 *  Returns the currently requested page or attachment size.
 *
 *  @since 2.0
 */
public class PageSizeTag extends WikiTagBase {

    private static final long serialVersionUID = 0L;
    private static final Logger log = Logger.getLogger( PageSizeTag.class );
    
    public final int doWikiStartTag() throws IOException {
        final WikiEngine engine = m_wikiContext.getEngine();
        final WikiPage page = m_wikiContext.getPage();

        try {
            if( page != null ) {
                long size = page.getSize();

                if( size == -1 && engine.getPageManager().wikiPageExists( page ) ) { // should never happen with attachments
                    size = engine.getPageManager().getPureText( page.getName(), page.getVersion() ).length();
                    page.setSize( size );
                }

                pageContext.getOut().write( Long.toString(size) );
            }
        } catch( final ProviderException e ) {
            log.warn("Providers did not work: ",e);
            pageContext.getOut().write("Error determining page size: "+e.getMessage());
        }

        return SKIP_BODY;
    }

}
