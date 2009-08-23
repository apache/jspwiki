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

import javax.servlet.jsp.JspTagException;

import org.apache.wiki.tags.TabbedSectionTag.TabCollection;
import org.apache.wiki.util.TextUtil;

/**
 * <p>
 * Generates single tabbed page layout, when nested under a
 * {@link TabbedSectionTag}. Works together with the tabbedSection javascript.
 * Note that if you do not specify an url, the body contents of the tag are
 * loaded by the tag itself.
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
 * @since v2.3.63
 */
public class TabTag extends WikiTagBase
{
    private static final long serialVersionUID = -8534125226484616489L;
    
    private final TabInfo m_tabInfo = new TabInfo();

    /**
     * Lightweight class that holds information about TabTags.
     */
    public static class TabInfo 
    {
        private String m_id = null;
        
        private String m_accesskey = null;

        private String m_tabTitle = null;

        private String m_tabTitleKey = null;

        private String m_url = null;
        
        /**
         * Sets the id.
         * @param id
         */
        public void setId( String id )
        {
            m_id = id;
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
        
        /**
         * Returns the ID for this tab.
         * @return id
         */
        public String getId()
        {
            return m_id;
        }
        
        /**
         * Returns the URL for this tab, if supplied.
         * 
         * @return the URL
         */
        public String getUrl()
        {
            return m_url;
        }

        /**
         * Returns the tab access key.
         * 
         * @return the access key
         */
        public String getAccesskey()
        {
            return m_accesskey;
        }
        
        /**
         * Returns the tab title.
         * @return the title
         */
        public String getTitle()
        {
            return m_tabTitle;
        }
        
        /**
         * Returns the i18n key used to generate the tab title.
         * @return the title key
         */
        public String getTitleKey()
        {
            return m_tabTitleKey;
        }
    }

    protected TabInfo getTabInfo()
    {
        return m_tabInfo;
    }
    
    /**
     * {@inheritDoc}
     */
    public int doEndTag() throws javax.servlet.jsp.JspTagException
    {
        try
        {
            pageContext.getOut().write( "</div>\n" );
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
        m_tabInfo.m_accesskey = null;
        m_tabInfo.m_tabTitle = null;
        m_tabInfo.m_tabTitleKey = null;
        m_tabInfo.m_url = null;
    }

    /**
     * {@inheritDoc}
     */
    public int doWikiStartTag() throws JspTagException
    {
        //
        // Sanity checks
        //
        if( getId() == null )
        {
            throw new JspTagException( "Tab Tag without \"id\" attribute" );
        }
        if( m_tabInfo.m_tabTitle == null && m_tabInfo.m_tabTitleKey == null )
        {
            throw new JspTagException( "Tab Tag without \"tabTitle\" or \"tabTitleKey\" attribute" );
        }

        // Add tab to TabCollection so parent TabbedSection can get it later
        TabCollection tc = TabbedSectionTag.getTabContext( getPageContext().getRequest() );
        tc.addTab( this );

        // Generate the opening <div id=foo> tag, always with "hidetab" class
        // (TabbedSection#doAfterBody will fix this later...)
        try
        {
            pageContext.getOut().write( "<div id=\"" );
            pageContext.getOut().write( getId() );
            pageContext.getOut().write( "\" class=\"hidetab\">\n" );
        }
        catch( java.io.IOException e )
        {
            throw new JspTagException( "IO Error: " + e.getMessage() );
        }

        return EVAL_BODY_INCLUDE;
    }
    
    /**
     * {@inheritDoc}. Also sets the ID for the embedded {@link TabInfo object}.
     */
    @Override
    public void setId( String id )
    {
        super.setId( id );
        m_tabInfo.setId( id );
    }
    
    /**
     * Sets the tab access key.
     * 
     * @param accessKey the access key
     */
    public void setAccesskey( String accessKey )
    {
        m_tabInfo.setAccesskey( accessKey );
    }

    /**
     * Sets the tab title.
     * 
     * @param title the tab title
     */
    public void setTitle( String title )
    {
        m_tabInfo.setTitle( title );
    }

    /**
     * Sets the tab title key.
     * 
     * @param key the tab title key
     */
    public void setTitleKey( String key )
    {
        m_tabInfo.setTitleKey( key );
    }

    /**
     * Sets the tab URL.
     * 
     * @param url the URL
     */
    public void setUrl( String url )
    {
        m_tabInfo.setUrl( url );
    }
}
