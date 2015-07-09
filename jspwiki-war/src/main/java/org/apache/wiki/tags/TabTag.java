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

import javax.servlet.jsp.JspTagException;

import org.apache.wiki.util.TextUtil;

/**
 *  Generates single tabbed page layout.
 *  Works together with the tabbedSection javascript.  Note that if you do not
 *  specify an url, the body contents of the tag are loaded by the tag itself.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>id - ID for this tab. (mandatory)
 *    <LI>title - Title of this tab. (mandatory)
 *    <LI>accesskey - Single char usable as quick accesskey (alt- or ctrl-) (optional)
 *    <li>url - If you <i>don't</i> want to create a Javascript-enabled tag, you can use this
 *              to make the tab look just the usual tag, but instead, it will actually link
 *              to that page.  This can be useful in certain cases where you have something
 *              that you want to look like a part of a tag, but for example, due to it being
 *              very big in size, don't want to include it as a part of the page content
 *              every time.
 *  </UL>
 *
 *  @since v2.3.63
 */

public class TabTag extends WikiTagBase
{
    private static final long serialVersionUID = -8534125226484616489L;
    private String m_accesskey;
    private String m_tabTitle;
    private String m_url;

    /**
     * {@inheritDoc}
     */
    public void doFinally()
    {
        super.doFinally();

        m_accesskey = null;
        m_tabTitle  = null;
        m_url       = null;
    }

    /**
     * Sets the tab title.
     * @param aTabTitle the tab title
     */
    public void setTitle(String aTabTitle)
    {
        m_tabTitle = TextUtil.replaceEntities( aTabTitle );
    }

    /**
     * Sets the tab access key.
     * @param anAccesskey the access key
     */
    public void setAccesskey(String anAccesskey)
    {
        m_accesskey = TextUtil.replaceEntities( anAccesskey ); //take only the first char
    }

    /**
     * Sets the tab URL.
     * @param url the URL
     */
    public void setUrl( String url )
    {
        m_url = TextUtil.replaceEntities( url );
    }

    // insert <u> ..accesskey.. </u> in title
    private boolean handleAccesskey()
    {
        if( (m_tabTitle == null) || (m_accesskey == null) ) return false;

        int pos = m_tabTitle.toLowerCase().indexOf( m_accesskey.toLowerCase() );
        if( pos > -1 )
        {
            m_tabTitle = m_tabTitle.substring( 0, pos ) + "<span class='accesskey'>"
                       + m_tabTitle.charAt( pos ) + "</span>" + m_tabTitle.substring( pos+1 );
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int doWikiStartTag() throws JspTagException
    {
        TabbedSectionTag parent=(TabbedSectionTag)findAncestorWithClass( this, TabbedSectionTag.class );

        //
        //  Sanity checks
        //
        if( getId() == null )
        {
            throw new JspTagException("Tab Tag without \"id\" attribute");
        }
        if( m_tabTitle == null )
        {
            throw new JspTagException("Tab Tag without \"tabTitle\" attribute");
        }
        if( parent == null )
        {
            throw new JspTagException("Tab Tag without parent \"TabbedSection\" Tag");
        }

        if( !parent.isStateGenerateTabBody() ) return SKIP_BODY;

        StringBuilder sb = new StringBuilder(32);

        sb.append( "<div id=\""+ getId() + "\"" );

        if( !parent.validateDefaultTab( getId()) )
        {
            sb.append( " class=\"hidetab\"" );
        }
        sb.append( " >\n" );

        try
        {
            pageContext.getOut().write( sb.toString() );
        }
        catch( java.io.IOException e )
        {
            throw new JspTagException( "IO Error: " + e.getMessage() );
        }

        return EVAL_BODY_INCLUDE;
    }

    /**
     * {@inheritDoc}
     */
    public int doEndTag() throws JspTagException
    {
        TabbedSectionTag parent=(TabbedSectionTag)findAncestorWithClass( this, TabbedSectionTag.class );

        StringBuilder sb = new StringBuilder();

        if( parent.isStateFindDefaultTab() )
        {
            //inform the parent of each tab
            parent.validateDefaultTab( getId() );
        }
        else if( parent.isStateGenerateTabBody() )
        {
            sb.append( "</div>\n" );
        }
        else if( parent.isStateGenerateTabMenu() )
        {
            sb.append( "<a" );

            if( parent.validateDefaultTab( getId() ) )
            {
                sb.append( " class=\"activetab\"" );
            }

            sb.append( " id=\"menu-" + getId() + "\"" );

            if( m_url != null )
            {
                sb.append( " href='"+m_url+"'" );
            }

            if( handleAccesskey() )
            {
                sb.append( " accesskey=\"" + m_accesskey + "\"" );
            }

            sb.append( " >" );
            sb.append( m_tabTitle );
            sb.append( "</a>" );
        }

        try
        {
            pageContext.getOut().write( sb.toString() );
        }
        catch( IOException e )
        {
            throw new JspTagException( "IO Error: " + e.getMessage() );
        }

        return EVAL_PAGE;
    }
}
