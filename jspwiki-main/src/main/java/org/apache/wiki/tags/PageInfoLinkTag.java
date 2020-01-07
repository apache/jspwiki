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

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 *  Writes a link to the Wiki PageInfo.  Body of the link becomes the actual text.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *    <LI>title - Is used in page actions to display hover text (tooltip)
 *    <LI>accesskey - Set an accesskey (ALT+[Char])
 *  </UL>
 *
 *  @since 2.0
 */
// FIXME: Refactor together with LinkToTag and EditLinkTag.
public class PageInfoLinkTag extends WikiLinkTag {

    private static final long serialVersionUID = 0L;
    public String m_title = "";
    public String m_accesskey = "";
    
    public void setTitle( final String title )
    {
        m_title = title;
    }

    public void setAccesskey( final String access )
    {
        m_accesskey = access;
    }
    
    public final int doWikiStartTag() throws IOException {
        final WikiEngine engine = m_wikiContext.getEngine();
        String     pageName = m_pageName;

        if( m_pageName == null ) {
            final WikiPage p = m_wikiContext.getPage();
            if( p != null ) {
                pageName = p.getName();
            } else {
                return SKIP_BODY;
            }
        }

        if( engine.getPageManager().wikiPageExists(pageName) ) {
            final JspWriter out = pageContext.getOut();
            final String url = m_wikiContext.getURL( WikiContext.INFO, pageName );

            switch( m_format ) {
              case ANCHOR: out.print("<a class=\"pageinfo\" href=\""+url+"\" accesskey=\"" + m_accesskey + "\" title=\"" + m_title + "\">"); break;
              case URL: out.print( url ); break;
            }
            return EVAL_BODY_INCLUDE;
        }
        return SKIP_BODY;
    }

}
