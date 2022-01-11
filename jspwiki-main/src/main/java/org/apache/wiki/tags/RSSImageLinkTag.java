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

import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.rss.RSSGenerator;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *  Writes an image link to a JSPWiki RSS file.  If RSS generation has
 *  been turned off in jspwiki.properties, returns an empty string.
 *
 *  @since 2.0
 */
public class RSSImageLinkTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;

    protected String m_title;
    private   String m_mode;
    private   String m_pageName;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initTag()
    {
        super.initTag();
        m_title = null;
        m_mode  = RSSGenerator.MODE_FULL;
        m_pageName = null;
    }

    /**
     *  Sets the title for the link.  If not defined, no title is shown.
     *
     *  @param title A string for the title.
     */
    public void setTitle( final String title )
    {
        m_title = title;
    }

    public void setMode( final String mode )
    {
        m_mode = mode;
    }


    /**
     *  Returns the title.
     *
     *  @return The title.
     */
    public String getTitle()
    {
        return m_title;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final int doWikiStartTag() throws IOException {
        final Engine engine = m_wikiContext.getEngine();
        final JspWriter out = pageContext.getOut();
        final ResourceBundle rb = Preferences.getBundle( m_wikiContext, InternationalizationManager.CORE_BUNDLE );
        if( engine.getManager( RSSGenerator.class ) != null && engine.getManager( RSSGenerator.class ).isEnabled() ) {
            if( RSSGenerator.MODE_FULL.equals(m_mode) ) {
                final String rssURL = engine.getGlobalRSSURL();
                if( rssURL != null ) {
                    out.print("<a class=\"feed\" href=\""+rssURL);
                    out.print( " title='"+rb.getString( "rss.title.full" )+"'>" );
                    out.print( "&nbsp;</a> ");
                }
            } else {
                final String page = m_pageName != null ? m_pageName : m_wikiContext.getPage().getName();
                final String params = "page="+page+"&mode="+m_mode;
                out.print( "<a href='"+m_wikiContext.getURL( ContextEnum.PAGE_NONE.getRequestContext(), "rss.jsp", params ));
                out.print( "' class='feed'" );
                out.print( " title='"+MessageFormat.format( rb.getString( "rss.title" ), page )+"'>" );
                out.print( "&nbsp;</a> ");
            }
        }

        return SKIP_BODY;
    }

}
