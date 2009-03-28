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

import java.io.IOException;

import org.apache.wiki.api.WikiPage;


/**
 *  Returns the currently requested page or attachment size.
 *
 *  @since 2.0
 */
public class PageSizeTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    /**
     *  {@inheritDoc}
     */
    public final int doWikiStartTag()
        throws IOException
    {
        WikiPage   page   = m_wikiContext.getPage();

        if( page != null )
        {
            long size = page.getSize();

            pageContext.getOut().write( Long.toString(size) );
        }

        return SKIP_BODY;
    }
}
