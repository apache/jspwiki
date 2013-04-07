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

import javax.servlet.jsp.JspWriter;

import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;

/**
 *  Writes a comment link.  Body of the link becomes the link text.
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *    <LI>format - Format, either "anchor" or "url".
 *  </UL>
 *
 *  @since 2.0
 */
public class CommentLinkTag
    extends WikiLinkTag
{
    private static final long serialVersionUID = 0L;
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public final int doWikiStartTag()
        throws IOException
    {
        WikiPage   page     = null;
        String     pageName = null;
        
        //
        //  Determine the page and the link.
        //
        if( m_pageName == null )
        {
            page = m_wikiContext.getPage();
            if( page == null )
            {
                // You can't call this on the page itself anyways.
                return SKIP_BODY;
            }

            pageName = page.getName();
        }
        else
        {
            pageName = m_pageName;
        }

        //
        //  Finally, print out the correct link, according to what
        //  user commanded.
        //
        JspWriter out = pageContext.getOut();

        switch( m_format )
        {
            case ANCHOR:
                out.print("<a href=\""+getCommentURL(pageName)+"\">");
                break;

            case URL:
                out.print( getCommentURL(pageName) );
                break;
                
            default:
                throw new InternalWikiException("Impossible format "+m_format);
            
        }

        return EVAL_BODY_INCLUDE;
    }

    private String getCommentURL( String pageName )
    {
        return m_wikiContext.getURL(WikiContext.COMMENT, pageName);
    }

}
