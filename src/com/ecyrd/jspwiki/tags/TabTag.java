/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.ecyrd.jspwiki.tags;

import javax.servlet.jsp.*;
import com.ecyrd.jspwiki.TextUtil;

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
 *  @author Dirk Frederickx
 *  @since v2.3.63
 */

public class TabTag extends WikiTagBase
{
    private static final long serialVersionUID = -8534125226484616489L;
    private String m_accesskey;
    private String m_tabID;
    private String m_tabTitle;
    private String m_url;

    /**
     * {@inheritDoc}
     */
    public void doFinally()
    {
        super.doFinally();

        m_accesskey = null;
        m_tabID     = null;
        m_tabTitle  = null;
        m_url       = null;
    }

    /**
     * Sets the tab ID.
     * @param aTabID the ID
     */
    public void setId(String aTabID)
    {
        m_tabID = TextUtil.replaceEntities( aTabID );
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
        if( m_tabID == null )
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

        StringBuffer sb = new StringBuffer(32);

        sb.append( "<div id=\""+ m_tabID + "\"" );

        if( !parent.validateDefaultTab( m_tabID) )
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
    public int doEndTag() throws javax.servlet.jsp.JspTagException
    {
        TabbedSectionTag parent=(TabbedSectionTag)findAncestorWithClass( this, TabbedSectionTag.class );

        StringBuffer sb = new StringBuffer();

        if( parent.isStateFindDefaultTab() )
        {
            //inform the parent of each tab
            parent.validateDefaultTab( m_tabID );
        }
        else if( parent.isStateGenerateTabBody() )
        {
            sb.append( "</div>\n" );
        }
        else if( parent.isStateGenerateTabMenu() )
        {
            sb.append( "<a" );

            if( parent.validateDefaultTab( m_tabID ) )
            {
                sb.append( " class=\"activetab\"" );
            }

            sb.append( " id=\"menu-" + m_tabID + "\"" );

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
        catch( java.io.IOException e )
        {
            throw new JspTagException( "IO Error: " + e.getMessage() );
        }

        return EVAL_PAGE;
    }
}
