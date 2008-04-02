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

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;
import com.ecyrd.jspwiki.render.RenderingManager;

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
    
    public final int doWikiStartTag()
        throws IOException
    {
        if ( !( m_actionBean instanceof WikiContext ) )
        {
            return SKIP_BODY;
        }
        
        WikiContext context = (WikiContext)m_actionBean;
        WikiEngine engine = context.getEngine();
        WikiPage   page   = context.getPage();

        String author = page.getAuthor();

        if( author != null && author.length() > 0 )
        {
            author = TextUtil.replaceEntities(author);
            if( engine.pageExists(author) )
            {
                // FIXME: It's very boring to have to do this.
                //        Slow, too.

                RenderingManager mgr = engine.getRenderingManager();
                
                MarkupParser p = mgr.getParser( context, "["+author+"|"+author+"]" );

                WikiDocument d = p.parse();
                
                author = mgr.getHTML( context, d );
            }

            pageContext.getOut().print( author );
        }
        else
        {
            pageContext.getOut().print( "unknown" );
        }

        return SKIP_BODY;
    }
}
