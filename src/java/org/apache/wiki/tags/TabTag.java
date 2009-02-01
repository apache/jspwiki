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

package org.apache.wiki.tags;

import java.util.Locale;

import javax.servlet.jsp.JspTagException;

import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.util.TextUtil;


/**
 * <p>
 * Generates single tabbed page layout. Works together with the tabbedSection
 * javascript. Note that if you do not specify an url, the body contents of the
 * tag are loaded by the tag itself.
 * </p>
 * <p>
 * <b>Attributes</b>
 * </p>
 * <ul>
 * <li><b>id</b> - ID for this tab. <em>Mandatory.</em></li>
 * <li><b>title</b> - Title of this tab.
 * <em>Mandatory, if <code>titleKey</code> is not supplied</em></li>
 * <li><b>titleKey</b> - Message key in <code>default.properties</code> that
 * contains the title for this tab. The
 * <em>Mandatory, it <code>title</code> is not supplied</em>. If both
 * <code>title</code> and <code>titleKey</code> are supplied,
 * <code>titleKey</code> wins.</li>
 * <li><b>accesskey</b> - Single char usable as quick accesskey (alt- or
 * ctrl-) (optional</em></li>
 * <li><b>url</b> - If you <i>don't</i> want to create a Javascript-enabled
 * tag, you can use this to make the tab look just the usual tag, but instead,
 * it will actually link to that page. This can be useful in certain cases where
 * you have something that you want to look like a part of a tag, but for
 * example, due to it being very big in size, don't want to include it as a part
 * of the page content every time. <em>Optional.</em></li>
 * </ul>
 * 
 * @author Dirk Frederickx
 * @since v2.3.63
 */
public class TabTag extends WikiTagBase
{
    private static final long serialVersionUID = -8534125226484616489L;

    private String m_accesskey;

    private String m_outputTitle;

    private String m_tabTitle;

    private String m_tabTitleKey;

    private String m_url;

    /**
     * {@inheritDoc}
     */
    public int doEndTag() throws javax.servlet.jsp.JspTagException
    {
        TabbedSectionTag parent = getParentTag( TabbedSectionTag.class );

        StringBuilder sb = new StringBuilder();

        if( parent.isStateFindDefaultTab() )
        {
            // inform the parent of each tab
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
                sb.append( " href='" + m_url + "'" );
            }

            if( handleAccesskey() )
            {
                sb.append( " accesskey=\"" + m_accesskey + "\"" );
            }

            sb.append( " >" );
            sb.append( m_outputTitle );
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

    /**
     * {@inheritDoc}
     */
    public void doFinally()
    {
        super.doFinally();

        m_accesskey = null;
        m_outputTitle = null;
        m_tabTitle = null;
        m_tabTitleKey = null;
        m_url = null;
    }

    /**
     * {@inheritDoc}
     */
    public int doWikiStartTag() throws JspTagException
    {
        TabbedSectionTag parent = getParentTag( TabbedSectionTag.class );

        //
        // Sanity checks
        //
        if( getId() == null )
        {
            throw new JspTagException( "Tab Tag without \"id\" attribute" );
        }
        if( m_tabTitle == null && m_tabTitleKey == null )
        {
            throw new JspTagException( "Tab Tag without \"tabTitle\" or \"tabTitleKey\" attribute" );
        }
        if( parent == null )
        {
            throw new JspTagException( "Tab Tag without parent \"TabbedSection\" Tag" );
        }

        // Generate the actual title
        if( m_tabTitleKey != null )
        {
            Locale locale = m_wikiContext.getHttpRequest().getLocale();
            InternationalizationManager i18n = m_wikiContext.getEngine().getInternationalizationManager();
            m_outputTitle = i18n.get( InternationalizationManager.TEMPLATES_BUNDLE, locale, m_tabTitleKey );
        }
        if ( m_outputTitle == null )
        {
           m_outputTitle = m_tabTitle;
        }

        if( !parent.isStateGenerateTabBody() )
            return SKIP_BODY;

        StringBuilder sb = new StringBuilder( 32 );

        sb.append( "<div id=\"" + getId() + "\"" );

        if( !parent.validateDefaultTab( getId() ) )
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
     * Sets the tab access key.
     * 
     * @param accessKey the access key
     */
    public void setAccesskey( String accessKey )
    {
        m_accesskey = TextUtil.replaceEntities( accessKey ); // take only the
        // first char
    }

    /**
     * Sets the tab title.
     * 
     * @param title the tab title
     */
    public void setTitle( String title )
    {
        m_tabTitle = TextUtil.replaceEntities( title );
    }

    /**
     * Sets the tab title key.
     * 
     * @param key the tab title key
     */
    public void setTitleKey( String key )
    {
        m_tabTitleKey = TextUtil.replaceEntities( key );
    }

    /**
     * Sets the tab URL.
     * 
     * @param url the URL
     */
    public void setUrl( String url )
    {
        m_url = TextUtil.replaceEntities( url );
    }

    // insert <u> ..accesskey.. </u> in title
    private boolean handleAccesskey()
    {
        if( (m_outputTitle == null) || (m_accesskey == null) )
            return false;

        int pos = m_outputTitle.toLowerCase().indexOf( m_accesskey.toLowerCase() );
        if( pos > -1 )
        {
            m_outputTitle = m_outputTitle.substring( 0, pos ) + "<span class='accesskey'>" + m_outputTitle.charAt( pos )
                            + "</span>" + m_outputTitle.substring( pos + 1 );
        }
        return true;
    }
}
