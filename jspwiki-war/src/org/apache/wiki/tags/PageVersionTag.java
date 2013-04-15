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

import java.io.IOException;

import org.apache.wiki.WikiPage;

/**
 *  Writes the version of the current page.  If this is
 *  marked as the current version, then includes body as text instead of
 *  version number.
 *
 *  @since 2.0
 */
public class PageVersionTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    public final int doWikiStartTag()
        throws IOException
    {
        WikiPage   page   = m_wikiContext.getPage();

        if( page != null )
        {
            int version = page.getVersion();

            if( version > 0 )
            {
                pageContext.getOut().print( Integer.toString(version) );
                return SKIP_BODY;
            }
        }

        return EVAL_BODY_INCLUDE;
    }
}
