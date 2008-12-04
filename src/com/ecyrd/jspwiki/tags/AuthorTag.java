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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.i18n.InternationalizationManager;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;
import com.ecyrd.jspwiki.render.RenderingManager;
import com.ecyrd.jspwiki.util.TextUtil;

/**
 *  Writes the author name of the current page, including a link to that page,
 *  if that page exists.
 *
 *  @since 2.0
 */
public class AuthorTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        String author = page.getAuthor();

        if( author != null && author.length() > 0 )
        {
            author = TextUtil.replaceEntities(author);
            if( engine.pageExists(author) )
            {
                // FIXME: It's very boring to have to do this.
                //        Slow, too.

                RenderingManager mgr = engine.getRenderingManager();
                
                MarkupParser p = mgr.getParser( m_wikiContext, "["+author+"|"+author+"]" );

                WikiDocument d = p.parse();
                
                author = mgr.getHTML( m_wikiContext, d );
            }

            pageContext.getOut().print( author );
        }
        else
        {
            pageContext.getOut().print( m_wikiContext.getBundle( InternationalizationManager.CORE_BUNDLE ).getString( "common.unknownauthor" ));
        }

        return SKIP_BODY;
    }
}
