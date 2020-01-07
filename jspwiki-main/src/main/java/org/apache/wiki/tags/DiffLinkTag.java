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
import org.apache.wiki.WikiProvider;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 *  Writes a diff link.  Body of the link becomes the link text.
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.</LI>
 *    <LI>version - The older of these versions.  May be an integer to
 *        signify a version number, or the text "latest" to signify the latest version.
 *        If not specified, will default to "latest".  May also be "previous" to signify
 *        a version prior to this particular version.</LI>
 *    <LI>newVersion - The newer of these versions.  Can also be "latest", or "previous".  Defaults to "latest".</LI>
 *  </UL>
 *
 *  If the page does not exist, this tag will fail silently, and not evaluate
 *  its body contents.
 *
 *  @since 2.0
 */
public class DiffLinkTag extends WikiLinkTag {

    private static final long serialVersionUID = 0L;
    public static final String VER_LATEST   = "latest";
    public static final String VER_PREVIOUS = "previous";
    public static final String VER_CURRENT  = "current";

    private String m_version    = VER_LATEST;
    private String m_newVersion = VER_LATEST;

    public void initTag()
    {
        super.initTag();
        m_version = m_newVersion = VER_LATEST;
    }

    public final String getVersion()
    {
        return m_version;
    }

    public void setVersion( final String arg )
    {
        m_version = arg;
    }

    public final String getNewVersion()
    {
        return m_newVersion;
    }

    public void setNewVersion( final String arg )
    {
        m_newVersion = arg;
    }

    public final int doWikiStartTag() throws IOException {
        final WikiEngine engine = m_wikiContext.getEngine();
        String pageName = m_pageName;

        if( m_pageName == null ) {
            if( m_wikiContext.getPage() != null ) {
                pageName = m_wikiContext.getPage().getName();
            } else {
                return SKIP_BODY;
            }
        }

        final JspWriter out = pageContext.getOut();

        int r1;
        int r2;

        //  In case the page does not exist, we fail silently.
        if( !engine.getPageManager().wikiPageExists( pageName ) ) {
            return SKIP_BODY;
        }

        if( VER_LATEST.equals(getVersion()) ) {
            final WikiPage latest = engine.getPageManager().getPage( pageName, WikiProvider.LATEST_VERSION );
            if( latest == null ) {
                // This may occur if matchEnglishPlurals is on, and we access the wrong page name
                return SKIP_BODY;
            }
            r1 = latest.getVersion();
        } else if( VER_PREVIOUS.equals( getVersion() ) ) {
            r1 = m_wikiContext.getPage().getVersion() - 1;
            r1 = Math.max( r1, 1 );
        } else if( VER_CURRENT.equals( getVersion() ) ) {
            r1 = m_wikiContext.getPage().getVersion();
        } else {
            r1 = Integer.parseInt( getVersion() );
        }

        if( VER_LATEST.equals( getNewVersion() ) ) {
            final WikiPage latest = engine.getPageManager().getPage( pageName, WikiProvider.LATEST_VERSION );
            r2 = latest.getVersion();
        } else if( VER_PREVIOUS.equals( getNewVersion() ) ) {
            r2 = m_wikiContext.getPage().getVersion() - 1;
            r2 = Math.max( r2, 1 );
        } else if( VER_CURRENT.equals( getNewVersion() ) ) {
            r2 = m_wikiContext.getPage().getVersion();
        } else {
            r2 = Integer.parseInt( getNewVersion() );
        }

        final String url = m_wikiContext.getURL( WikiContext.DIFF, pageName, "r1="+r1+"&amp;r2="+r2 );
        switch( m_format ) {
          case ANCHOR:
            out.print("<a href=\""+url+"\">");
            break;
          case URL:
            out.print( url );
            break;
        }

        return EVAL_BODY_INCLUDE;
    }

}
