/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.io.IOException;

import javax.servlet.jsp.jstl.fmt.LocaleSupport;

import com.ecyrd.jspwiki.TextUtil;


/**
 *  Calculate pagination string. Used for page-info and search results
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI> start - start item of the page to be highlighted
 *    <LI> total - total number of items
 *    <LI> pagesize - total number of items per page
 *    <LI> maxlinks - number of page links to be generated
 *    <LI> fmtkey - pagination prefix of the i18n resource key
 *  </UL>
 *  <P>Following optional attributes can be parameterised with '%s' (item count)</P>
 *  <UL>
 *    <LI> href - href of each page link. (optional)
 *    <LI> onclick - onclick of each page link. (optional)
 *  </UL>
 *
 *  @author Dirk Frederickx
 *  @since 2.5.109
 */
public class SetPaginationTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    private static final int ALLITEMS = -1;

        private int m_start;
    private int m_total;
    private int m_pagesize;
    private int m_maxlinks;
    private String m_fmtkey;
    private String m_href;
    private String m_onclick;

    public void initTag()
    {
        super.initTag();
        m_start = 0;
        m_total = 0;
        m_pagesize = 20;
        m_maxlinks = 9;
        m_fmtkey = null;
        m_href = null;
        m_onclick = null;
    }

    public void setStart(int arg)
    {
        m_start = arg;
    }

    public void setTotal(int arg)
    {
        m_total = arg;
    }

    public void setPagesize(int arg)
    {
        m_pagesize = arg;
    }

    public void setMaxlinks(int arg)
    {
        m_maxlinks = arg;
        if( m_maxlinks % 2 == 0 ) m_maxlinks--; /* must be odd */
    }

    public void setFmtkey(String arg)
    {
        m_fmtkey = arg;
    }

    public void setHref(String arg)
    {
        m_href = arg;
    }

    public void setOnclick(String arg)
    {
        m_onclick = arg;
    }


    // 0 20 40 60
    // 0 20 40 60 80 next last
    // first previous 20 40 *60* 80 100 next last
    // fist previous 40 60 80 100 120
    public int doWikiStartTag()
        throws IOException
    {
        if( m_total < m_pagesize ) return SKIP_BODY;

        StringBuffer pagination = new StringBuffer();

        if( m_start > m_total ) m_start = m_total;
        if( m_start < ALLITEMS ) m_start = 0;

        int maxs = m_pagesize * m_maxlinks;
        int mids = m_pagesize * ( m_maxlinks / 2 );

        pagination.append( "<div class='pagination'>");

        pagination.append( LocaleSupport.getLocalizedMessage(pageContext, m_fmtkey )+ " " );

        int cursor = 0;
        int cursormax = m_total;

        if( m_total > maxs )   //need to calculate real window ends
        {
          if( m_start > mids ) cursor = m_start - mids;
          if( (cursor + maxs) > m_total )
            cursor = ( ( 1 + m_total/m_pagesize ) * m_pagesize ) - maxs ;

          cursormax = cursor + maxs;
        }


        if( ( m_start == ALLITEMS ) || (cursor > 0) )
        {
            appendLink ( pagination, 0, m_fmtkey + ".first" );
        }


        if( (m_start != ALLITEMS ) && (m_start-m_pagesize >= 0) )
        {
            appendLink( pagination, m_start-m_pagesize, m_fmtkey + ".previous" );
        }

        if( m_start != ALLITEMS )
        {
          while( cursor < cursormax )
          {
            if( cursor == m_start )
            {
              pagination.append( "<span class='cursor'>" );
              pagination.append( 1 + cursor/m_pagesize );
              pagination.append( "</span> " );
            }
            else
            {
              appendLink( pagination, cursor, 1+cursor/m_pagesize );
            }
            cursor += m_pagesize;
          }
        }


        if( (m_start != ALLITEMS ) && (m_start + m_pagesize < m_total) )
        {
            appendLink( pagination, m_start+m_pagesize, m_fmtkey + ".next" );

        if( (m_start == ALLITEMS ) || (cursormax < m_total) )
          appendLink ( pagination, ( (m_total/m_pagesize) * m_pagesize ), m_fmtkey + ".last" );
        }

        if( m_start == ALLITEMS )
        {
          pagination.append( "<span class='cursor'>" );
          pagination.append( LocaleSupport.getLocalizedMessage(pageContext, m_fmtkey + ".all" ) );
          pagination.append( "</span>&nbsp;&nbsp;" );
        }
        else
        {
          appendLink ( pagination, ALLITEMS, m_fmtkey + ".all" );
        }

        //(Total items: " + m_total + ")" );
        pagination.append( LocaleSupport.getLocalizedMessage(pageContext, m_fmtkey + ".total",
                           new Object[]{ new Integer( m_total ) } ) );

        pagination.append( "</div>" );


        /* +++ processing done +++ */

		String p = pagination.toString();

        pageContext.getOut().println( p );

        pageContext.setAttribute( "pagination", p ); /* and cache for later use in page context */

        return SKIP_BODY;
    }


    /**
     * Generate pagination links <a href='' title='' onclick=''>text</a>
     * for pagination blocks starting a page.
     * Uses m_href and m_onclick as attribute patterns
     * '%s' in the patterns are replaced with page offset
     *
     * @param sb  : stringbuffer to write output to
     * @param page : start of page block
     * @param onclick : link text
     *
     **/
    private void appendLink( StringBuffer sb, int page, String fmttextkey )
    {
		appendLink2( sb, page, LocaleSupport.getLocalizedMessage( pageContext, fmttextkey ) );
    }
    private void appendLink( StringBuffer sb, int page, int paginationblock )
    {
		appendLink2( sb, page, Integer.toString( paginationblock ) );
    }
    private void appendLink2( StringBuffer sb, int page, String text )
    {
        sb.append( "<a title=\"" );
        if( page == ALLITEMS )
        {
            sb.append( LocaleSupport.getLocalizedMessage( pageContext, m_fmtkey + ".showall.title" ) );
        }
        else
        {
            sb.append( LocaleSupport.getLocalizedMessage( pageContext, m_fmtkey + ".show.title",
                       new Object[]{ new Integer( page + 1 ), new Integer( page + m_pagesize ) } ) );
        }
        sb.append( "\" " );

        if( m_href != null )
        {
            sb.append( "href=\"" );
            sb.append( TextUtil.replaceString( m_href, "%s", Integer.toString( page ) ) );
            sb.append( "\" " );
        }

        if( m_onclick != null )
        {
            sb.append( "onclick=\"" );
            sb.append( TextUtil.replaceString( m_onclick, "%s", Integer.toString( page ) ) );
            sb.append( "\" " );
        }

        sb.append( ">" );
        sb.append( text );
        sb.append( "</a> " );
    }

}