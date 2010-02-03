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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspTagException;

import net.sourceforge.stripes.action.ActionBean;

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
 * <li><b>beanclass</b> and <b>event</b> - If you <i>don't</i> want to create
 * a Javascript-enabled tag, you can supply the name of a Stripes ActionBean
 * and event to be invoked, the URL for which will be looked up and rendered.
 * If the event is not specified, the default event will be used. If both
 * {@code url} and {@code beanclass} are specified, {@code beanclass} wins.
 * <em>Optional.</em></li>
 * <li><b>onclick</b>(Pass-through) JavaScript to be executed after the
 * tab is clicked, but before the contents are displayed. <em>Optional.</em></li>
 * </ul>
 * 
 * @since v2.3.63
 */
public class TabTag extends WikiTagBase implements ParamHandler
{
    private static final long serialVersionUID = -8534125226484616489L;
    
    private final Info m_info = new Info();
    
    /**
     * Lightweight class that holds information about TabTags.
     */
    public static class Info implements Cloneable
    {
        static final String ACCESS_KEY = "accessKey";

        static final String EVENT = "event";
        
        static final String ID = "id";

        static final String ON_CLICK = "onClick";

        static final String TITLE = "tabTitle";

        static final String TITLE_KEY = "tabTitleKey";

        static final String URL = "url";
        
        /**
         * The tab's ActionBean class name for generating an URL.
         */
        Class<? extends ActionBean> beanclass = null;
        
        /**
         * Parameters passed to the Tab tag.
         */
        final Map<String, String> containedParams = new HashMap<String,String>();
        
        /**
         * The String options passed to the TabTag object.
         */
        final Map<String,String> options = new HashMap<String,String>();

        /**
         * Clones the Info object.
         */
        public Object clone()
        {
            Info info = new Info();
            if ( beanclass != null )
            {
                info.beanclass = beanclass;
            }
            info.containedParams.putAll( containedParams );
            info.options.putAll( options );
            return info;
        }
    }

    protected Info getTabInfo()
    {
        return m_info;
    }
    
    /**
     * {@inheritDoc}
     */
    public int doEndTag() throws javax.servlet.jsp.JspTagException
    {
        // Add tab to TabCollection so parent TabbedSection can get it later
        TabCollection tc = TabbedSectionTag.getTabContext( getPageContext().getRequest() );
        tc.addTab( this );

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
        m_info.beanclass = null;
        m_info.containedParams.clear();
        m_info.options.clear();
    }

    /**
     * {@inheritDoc}
     * @throws ClassNotFoundException 
     */
    public int doWikiStartTag() throws JspTagException, ClassNotFoundException
    {
        //
        // Sanity checks
        //
        if( getId() == null )
        {
            throw new JspTagException( "Tab Tag without \"id\" attribute" );
        }
        if( m_info.options.get( Info.TITLE ) == null && 
            m_info.options.get( Info.TITLE_KEY ) == null )
        {
            throw new JspTagException( "Tab Tag without \"tabTitle\" or \"tabTitleKey\" attribute" );
        }

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
     * {@inheritDoc}. Also sets the ID for the embedded {@link Info object}.
     * This attribute is required.
     */
    @Override
    public void setId( String id )
    {
        super.setId( id );
        m_info.options.put( Info.ID, id );
    }
    
    /**
     * Sets the tab access key.
     * 
     * @param accessKey the access key
     */
    public void setAccesskey( String accessKey )
    {
        if ( accessKey != null && accessKey.length() > 0 )
        {
            m_info.options.put( Info.ACCESS_KEY, accessKey.substring( 0, 1 ) );
        }
    }
    
    /**
     * Sets the tab beanclass, which must be the name of a class of
     * type {@link net.sourceforge.stripes.action.ActionBean}.
     * @param beanclass the ActionBean class name
     * @throws ClassNotFoundException if the bean class cannot be located or loaded
     */
    @SuppressWarnings("unchecked")
    public void setBeanclass( String beanclass ) throws ClassNotFoundException
    {
        m_info.beanclass = (Class<? extends ActionBean>)Class.forName( beanclass );
    }

    /**
     * Support for ParamTag supplied parameters in body.
     */
    public void setContainedParameter( String name, String value )
    {
        m_info.containedParams.put( name, value );
    }

    /**
     * Sets the tab event, which must correspond to the handler name
     * of a Stripes ActionBean. If omitted, the event handler
     * will be the method annotated by {@link net.sourceforge.stripes.action.DefaultHandler}.
     * @param event the ActionBean event handler name
     */
    public void setEvent( String event )
    {
        m_info.options.put( Info.EVENT, TextUtil.replaceEntities( event ) );
    }

    /**
     * Sets the JavaScript to execute after the tab is clicked, but before
     * the contents are shown.
     * @param onclick the code
     */
    public void setOnclick( String onclick )
    {
        m_info.options.put( Info.ON_CLICK, onclick );
    }
    
    /**
     * Sets the tab title.
     * 
     * @param title the tab title
     */
    public void setTitle( String title )
    {
        m_info.options.put( Info.TITLE, TextUtil.replaceEntities( title ) );
    }

    /**
     * Sets the tab title key.
     * 
     * @param key the tab title key
     */
    public void setTitleKey( String key )
    {
        m_info.options.put( Info.TITLE_KEY, TextUtil.replaceEntities( key ) );
    }

    /**
     * Sets the tab URL.
     * 
     * @param url the URL
     */
    public void setUrl( String url )
    {
        m_info.options.put( Info.URL, TextUtil.replaceEntities( url ) );
    }
}
