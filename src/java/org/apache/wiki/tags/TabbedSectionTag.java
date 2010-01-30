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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import net.sourceforge.stripes.util.UrlBuilder;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.tags.TabTag.TabInfo;

/**
 * <p>
 * Generates a container for page tabs, as defined by collaborating
 * {@link TabTag} tags. Works together with the tabbedSection JavaScript. The
 * output of the two collaborating tags is two sets of <code>&lt;div&gt;</code>
 * elements. The first one will have a class of <code>tabmenu</code>, and
 * includes the tab names, accessibility keys and and URL that contains the
 * tab's contents (if specified). The second <code>&lt;div&gt;</code>, of
 * class <code>tabs</code>, contains additional nested
 * <code>&lt;div&gt;</code> elements that contain the actual tab content, if
 * any was enclosed by the <code>wiki:Tab</code> tags.
 * </p>
 * <p>
 * For example, consider the following tags as defined on a JSP:
 * </p>
 * <blockquote><code>
 * &lt;wiki:TabbedSection defaultTab="pagecontent"&gt;<br/>
 * &nbsp;&nbsp;&lt;wiki:Tab id="pagecontent" title="View" accesskey="V"&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;p&gt;This is the main tab.&lt;/p&gt;<br/>
 * &nbsp;&nbsp;&lt;/wiki:Tab&gt;<br/>
 * &nbsp;&nbsp;&lt;wiki:Tab id="info" title="Info" accesskey="I" url="/PageInfo.jsp?page=Main" /&gt;<br/>
 * &lt;/wiki:TabbedSection&gt;
 * </code></blockquote>
 * <p>
 * This will cause the following HTML to be generated when the page contents are
 * returned to the browser:
 * </p>
 * <blockquote><code>
 * &lt;div class="tabmenu"&gt;<br/>
 * &nbsp;&nbsp;&lt;a class="activetab" id="menu-pagecontent" accesskey="v" &gt;&lt;span class='accesskey'&gt;V&lt;/span&gt;iew&lt;/a&gt;<br/>
 * &nbsp;&nbsp;&lt;a id="menu-info" href='<var>web-context</var>/PageInfo.jsp?page=Main' accesskey="i" &gt;&lt;span class='accesskey'&gt;I&lt;/span&gt;nfo&lt;/a&gt;<br/>
 * &lt;/div&gt;<br/>
 * &lt;div class="tabs"&gt;<br/>
 * &nbsp;&nbsp;&lt;div id="pagecontent"&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;p&gt;This is the main tab.&lt;/p&gt;<br/>
 * &nbsp;&nbsp;&lt;/div&gt;<br/>
 * &nbsp;&nbsp;&lt;div id="info" class="hidetab" /&gt;<br/>
 * &nbsp;&nbsp;&lt;div style="clear:both;" &gt;&lt;/div&gt;<br/>
 * &lt;/div&gt;
 * </code></blockquote>
 * <h3>Attributes</h3>
 * <ul>
 * <li>defaultTab - Page name to refer to. Default is the first tab.</li>
 * </ul>
 * 
 * @since v2.3.63
 */
public class TabbedSectionTag extends BodyTagSupport
{
    private static final long serialVersionUID = 2702437933960026481L;

    private WikiEngine m_engine;

    /**
     * Returns the TabCollection for the current HttpServletRequest. This
     * method is always guaranteed to return a valid TabCollection.
     * 
     * @param request the servlet request
     * @return the TabCollection
     */
    public static TabCollection getTabContext( ServletRequest request )
    {
        TabCollection tc = (TabCollection) request.getAttribute( ATTR_TABS );
        if( tc == null )
        {
            tc = new TabCollection();
            request.setAttribute( ATTR_TABS, tc );
        }
        return tc;
    }
    
    private static final String ATTR_TABS = "JSPWiki.TabbedSection.Tags";

    /**
     * Holds the current set of related {@link TabbedSectionTag} and {@link TabTag}
     * tags. One TabCollection is created for each HTTPServletRequest, rather than
     * per PageContext, because the tags could span multiple pages.
     */
    public static class TabCollection
    {
        /**
         * Private constructor to prevent direct instantiation.
         */
        private TabCollection()
        {
            super();
        }

        private final List<TabTag.TabInfo> m_tabs = new ArrayList<TabTag.TabInfo>();

        /**
         * Adds a child TabTag to the TabCollection. When the TabbedSection tag
         * generates its menu and tab &lt;div&gt; elements, they will be
         * generated in the order added. The tab added will be stored as a
         * defensive copy, so that calls to
         * {@link javax.servlet.jsp.tagext.Tag#release()} won't null out the
         * cached copies.
         * 
         * @param tab the tab to add
         * @throws ClassNotFoundException 
         */
        public void addTab( TabTag tab ) throws JspTagException
        {
            if( tab == null )
            {
                throw new JspTagException( "Cannot add null TabTag." );
            }

            TabInfo tabInfo = new TabInfo();
            tabInfo.setAccesskey( tab.getTabInfo().getAccesskey() );
            if ( tab.getTabInfo().getBeanclass() != null )
            {
                try
                {
                    tabInfo.setBeanclass( tab.getTabInfo().getBeanclass().getName() );
                }
                catch( ClassNotFoundException e )
                {
                    throw new JspTagException( "Could not set beanclass: " + e.getMessage() );
                }
            }
            for ( Map.Entry<String,String> entry : tab.getTabInfo().getContainedParameters().entrySet() )
            {
                tabInfo.setContainedParameter( entry.getKey(), entry.getValue() );
            }
            tabInfo.setEvent( tab.getTabInfo().getEvent() );
            tabInfo.setId( tab.getTabInfo().getId() );
            tabInfo.setTitle( tab.getTabInfo().getTitle() );
            tabInfo.setTitleKey( tab.getTabInfo().getTitleKey() );
            tabInfo.setUrl( tab.getTabInfo().getUrl() );
            m_tabs.add( tabInfo );
        }

        /**
         * Returns the list TabTag objects known to this TabCollection.
         * 
         * @return the list of tab
         */
        public List<TabTag.TabInfo> getTabs()
        {
            return m_tabs;
        }

        /**
         * Releases the TabCollection by clearing the internally cached list of
         * TabTag objects. This method is called by
         * {@link TabbedSectionTag#doEndTag()}.
         */
        public void release()
        {
            m_tabs.clear();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
    {
        super.release();
        m_defaultTabID = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int doStartTag() throws JspTagException
    {
        m_engine = WikiEngine.getInstance( ((HttpServletRequest) pageContext.getRequest()).getSession().getServletContext(), null );
        return EVAL_BODY_BUFFERED; /* always look inside */
    }

    /**
     * The tabbed section iterates 3 time through the underlying Tab tags -
     * first it identifies the default tab (displayed by default) - second it
     * generates the tabmenu markup (displays all tab-titles) - finally it
     * generates the content of each tab.
     * 
     * @return {@inheritDoc}
     * @throws {@inheritDoc}
     */
    @Override
    public int doAfterBody() throws JspTagException
    {
        // Stash the tag body (previously evaluated)
        BodyContent body = getBodyContent();
        String bodyString = body.getString();

        // Figure out the active (default) tab
        TabCollection tc = getTabContext( pageContext.getRequest() );
        List<TabTag.TabInfo> tabs = tc.getTabs();

        try
        {
            // Generate menu divs; output to enclosing writer
            body.clear();
            JspWriter writer = this.getPreviousOut();

            writer.append( "<div class=\"tabmenu\">\n" );
            for( TabTag.TabInfo tab : tabs )
            {
                // Is this the default tab?
                if( tab.getId().equals( m_defaultTabID ) )
                {
                    m_defaultTabID = tab.getId();
                }

                // If default tag still not 't set, use the first one
                if( m_defaultTabID == null || m_defaultTabID.length() == 0 )
                {
                    m_defaultTabID = tab.getId();
                }

                // Generate each menu item div
                writeTabMenuItem( writer, tab );
            }
            writer.append( "</div>\n" );

            // Output the opening "tabs" div
            writer.append( "<div class=\"tabs\">" );

            // Remove the "hidden" class from the active tab
            String activeTabDiv = "<div id=\"" + m_defaultTabID + "\" class=\"hidetab\">";
            bodyString = bodyString.replace( activeTabDiv, "<div id=\"" + m_defaultTabID + "\">" );

            // Write back the stashed tag body
            writer.append( bodyString );

            // Append our closing div tags
            writer.append( "  <div style=\"clear:both;\" ></div>\n</div>\n" );
        }
        catch( IOException e )
        {
            throw new JspTagException( e );
        }

        return SKIP_BODY;
    }

    public int doEndTag() throws JspException
    {
        // Clear the TabCollection for the next caller
        TabCollection tc = getTabContext( pageContext.getRequest() );
        tc.release();

        return super.doEndTag();
    }

    /**
     * Outputs a single menu item <code>div</code> element for a supplied tag.
     * 
     * @param writer the JspWriter to write the output to
     * @param tab the TabInfo object containing information about the tab
     * @throws IOException
     */
    private void writeTabMenuItem( JspWriter writer, TabTag.TabInfo tab ) throws IOException
    {
        writer.append( "  <a" );

        // Generate the ID
        writer.append( " id=\"menu-" + tab.getId() + "\"" );

        // Active tab?
        if( tab.getId().equals( m_defaultTabID ) )
        {
            writer.append( " class=\"activetab\"" );
        }
        
        // Generate the ActionBean event URL, if supplied
        if ( tab.getBeanclass() != null )
        {
            HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
            UrlBuilder builder = new UrlBuilder( request.getLocale(), tab.getBeanclass(), true );
            if ( tab.getEvent() != null )
            {
                builder.setEvent( tab.getEvent() );
            }
            for ( Map.Entry<String, String> entry : tab.getContainedParameters().entrySet() )
            {
                builder.addParameter( entry.getKey(), entry.getValue() );
            }
            String url = builder.toString();
            if ( request.getContextPath() != null && !url.startsWith( request.getContextPath() ) )
            {
                url = request.getContextPath() + url;
            }
            writer.append( " href='" + url + "'" );
        }

        // Generate the URL, if supplied
        else if( tab.getUrl() != null )
        {
            writer.append( " href='" + tab.getUrl() + "'" );
        }

        // Generate the tab title
        String tabTitle = null;
        if( tab.getTitleKey() != null )
        {
            Locale locale = pageContext.getRequest().getLocale();
            InternationalizationManager i18n = m_engine.getInternationalizationManager();
            tabTitle = i18n.get( InternationalizationManager.TEMPLATES_BUNDLE, locale, tab.getTitleKey() );
        }
        if( tabTitle == null )
        {
            tabTitle = tab.getTitle();
        }
        writer.append( ">" );

        // Output the tab title
        String accesskey = tab.getAccesskey();
        if( tabTitle != null )
        {
            // Generate the access key, if supplied
            if( accesskey != null )
            {
                int pos = tabTitle.toLowerCase().indexOf( accesskey.toLowerCase() );
                if( pos > -1 )
                {
                    tabTitle = tabTitle.substring( 0, pos ) + "<span class='accesskey'>" + tabTitle.charAt( pos ) + "</span>"
                               + tabTitle.substring( pos + 1 );
                }
            }
            writer.append( tabTitle );
        }

        // Output the closing tag
        writer.append( "</a>\n" );
    }

    private String m_defaultTabID = null;

    /**
     * Returns the default tab ID.
     * 
     * @return the tab ID
     */
    protected String getDefaultTab()
    {
        return m_defaultTabID;
    }

    /**
     * Sets the id of the default tab. If not set, the first {@link TabTag}
     * element encountered will be used as the default.
     * 
     * @param defaultTab the id of tab to use as the default
     */
    public void setDefaultTab( String defaultTab )
    {
        m_defaultTabID = defaultTab;
    }

}
