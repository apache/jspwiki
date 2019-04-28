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

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.attachment.Attachment;

/**
 *  Writes a link to a Wiki page.  Body of the link becomes the actual text.
 *  The link is written regardless to whether the page exists or not.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *    <LI>format - either "anchor" or "url" to output either an <A>... or just the HREF part of one.
 *    <LI>template - Which template should we link to.
 *    <LI>title - Is used in page actions to display hover text (tooltip)
 *    <LI>accesskey - Set an accesskey (ALT+[Char])
 *  </UL>
 *
 *  @since 2.0
 */
public class LinkToTag
    extends WikiLinkTag
{
    private static final long serialVersionUID = 0L;

    private String m_version = null;
    public String m_title = "";
    public String m_accesskey = "";

    public void initTag()
    {
        super.initTag();
        m_version = null;
    }

    public String getVersion()
    {
        return m_version;
    }

    public void setVersion( String arg )
    {
        m_version = arg;
    }

    public void setTitle( String title )
    {
        m_title = title;
    }

    public void setAccesskey( String access )
    {
        m_accesskey = access;
    }


    public int doWikiStartTag()
        throws IOException
    {
        String     pageName = m_pageName;
        boolean    isattachment = false;

        if( m_pageName == null )
        {
            WikiPage p = m_wikiContext.getPage();

            if( p != null )
            {
                pageName = p.getName();

                isattachment = p instanceof Attachment;
            }
            else
            {
                return SKIP_BODY;
            }
        }

        JspWriter out = pageContext.getOut();
        String url;
        String linkclass;
        String forceDownload = "";

        if( isattachment )
        {
            url = m_wikiContext.getURL(WikiContext.ATTACH, pageName,
                                       (getVersion() != null) ? "version="+getVersion() : null );
            linkclass = "attachment";

            if( m_wikiContext.getEngine().getAttachmentManager().forceDownload( pageName ) )
            {
                forceDownload = "download ";
            }

        }
        else
        {
        	StringBuilder params = new StringBuilder();
            if( getVersion() != null ) params.append( "version="+getVersion() );
            if( getTemplate() != null ) params.append( (params.length()>0?"&amp;":"") + "skin="+getTemplate() );

            url = m_wikiContext.getURL( WikiContext.VIEW, pageName,
                                        params.toString() );
            linkclass = "wikipage";
        }

        switch( m_format )
        {
          case ANCHOR:
            out.print("<a class=\""+linkclass+"\" href=\""+url+"\" accesskey=\""
                          + m_accesskey + "\" title=\"" + m_title
                          + "\" " + forceDownload + ">");
            break;
          case URL:
            out.print( url );
            break;
        }

        return EVAL_BODY_INCLUDE;
    }

}
