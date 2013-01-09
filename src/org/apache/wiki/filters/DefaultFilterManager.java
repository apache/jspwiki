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
package org.apache.wiki.filters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.filters.PageFilter;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.modules.ModuleManager;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.PriorityList;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;


/**
 *  Manages the page filters.  Page filters are components that can be executed
 *  at certain places:
 *  <ul>
 *    <li>Before the page is translated into HTML.
 *    <li>After the page has been translated into HTML.
 *    <li>Before the page is saved.
 *    <li>After the page has been saved.
 *  </ul>
 * 
 *  Using page filters allows you to modify the page data on-the-fly, and do things like
 *  adding your own custom WikiMarkup.
 * 
 *  <p>
 *  The initial page filter configuration is kept in a file called "filters.xml".  The
 *  format is really very simple:
 *  <pre>
 *  <?xml version="1.0"?>
 * 
 *  <pagefilters>
 *
 *    <filter>
 *      <class>org.apache.wiki.filters.ProfanityFilter</class>
 *    </filter>
 *  
 *    <filter>
 *      <class>org.apache.wiki.filters.TestFilter</class>
 *
 *      <param>
 *        <name>foobar</name>
 *        <value>Zippadippadai</value>
 *      </param>
 *
 *      <param>
 *        <name>blatblaa</name>
 *        <value>5</value>
 *      </param>
 *
 *    </filter>
 *  </pagefilters>
 *  </pre>
 *
 *  The &lt;filter> -sections define the filters.  For more information, please see
 *  the PageFilterConfiguration page in the JSPWiki distribution.
 *  
 * FIXME: this class should be final in 2.10. It isn't right now because of compatibility reasons
 */
public class DefaultFilterManager extends ModuleManager implements org.apache.wiki.api.engine.FilterManager
{
    private PriorityList<PageFilter> m_pageFilters = new PriorityList<PageFilter>();

    private Map<String, PageFilterInfo> m_filterClassMap = new HashMap<String,PageFilterInfo>();

    private static final Logger log = Logger.getLogger(DefaultFilterManager.class);

    /**
     *  Constructs a new FilterManager object.
     *  
     *  @param engine The WikiEngine which owns the FilterManager
     *  @param props Properties to initialize the FilterManager with
     *  @throws WikiException If something goes wrong.
     */
    public DefaultFilterManager( WikiEngine engine, Properties props )
        throws WikiException
    {
        super( engine );
        initialize( props );
    }

    /**
     *  Adds a page filter to the queue.  The priority defines in which
     *  order the page filters are run, the highest priority filters go
     *  in the queue first.
     *  <p>
     *  In case two filters have the same priority, their execution order
     *  is the insertion order.
     *
     *  @since 2.1.44.
     *  @param f PageFilter to add
     *  @param priority The priority in which position to add it in.
     *  @throws IllegalArgumentException If the PageFilter is null or invalid.
     */
    public void addPageFilter( PageFilter f, int priority ) throws IllegalArgumentException
    {
        if( f == null )
        {
            throw new IllegalArgumentException("Attempt to provide a null filter - this should never happen.  Please check your configuration (or if you're a developer, check your own code.)");
        }

        m_pageFilters.add( f, priority );
    }

    private void initPageFilter( String className, Properties props )
    {
        try
        {
            PageFilterInfo info = m_filterClassMap.get( className );
            
            if( info != null && !checkCompatibility(info) )
            {
                String msg = "Filter '"+info.getName()+"' not compatible with this version of JSPWiki";
                log.warn(msg);
                return;
            }
            
            int priority = 0; // FIXME: Currently fixed.

            Class cl = ClassUtil.findClass( "org.apache.wiki.filters",
                                            className );

            PageFilter filter = (PageFilter)cl.newInstance();

            filter.initialize( m_engine, props );

            addPageFilter( filter, priority );
            log.info("Added page filter "+cl.getName()+" with priority "+priority);
        }
        catch( ClassNotFoundException e )
        {
            log.error("Unable to find the filter class: "+className);
        }
        catch( InstantiationException e )
        {
            log.error("Cannot create filter class: "+className);
        }
        catch( IllegalAccessException e )
        {
            log.error("You are not allowed to access class: "+className);
        }
        catch( ClassCastException e )
        {
            log.error("Suggested class is not a PageFilter: "+className);
        }
        catch( FilterException e )
        {
            log.error("Filter "+className+" failed to initialize itself.", e);
        }
    }


    /**
     *  Initializes the filters from an XML file.
     *  
     *  @param props The list of properties.  Typically jspwiki.properties
     *  @throws WikiException If something goes wrong.
     */
    protected void initialize( Properties props )
        throws WikiException
    {
        InputStream xmlStream = null;
        String      xmlFile   = props.getProperty( PROP_FILTERXML );

        try
        {
            registerFilters();
            
            if( m_engine.getServletContext() != null )
            {
                log.debug( "Attempting to locate " + DEFAULT_XMLFILE + " from servlet context." );
                if( xmlFile == null )
                {
                    xmlStream = m_engine.getServletContext().getResourceAsStream( DEFAULT_XMLFILE );
                }
                else
                {
                    xmlStream = m_engine.getServletContext().getResourceAsStream( xmlFile );
                }
            }

            if( xmlStream == null )
            {
                // just a fallback element to the old behaviour prior to 2.5.8
                log.debug( "Attempting to locate filters.xml from class path." );

                if( xmlFile == null )
                {
                    xmlStream = getClass().getResourceAsStream( "/filters.xml" );
                }
                else
                {
                    xmlStream = getClass().getResourceAsStream( xmlFile );
                }
            }

            if( (xmlStream == null) && (xmlFile != null) )
            {
                log.debug("Attempting to load property file "+xmlFile);
                xmlStream = new FileInputStream( new File(xmlFile) );
            }

            if( xmlStream == null )
            {
                log.info("Cannot find property file for filters (this is okay, expected to find it as: '"+ (xmlFile == null ? DEFAULT_XMLFILE : xmlFile ) +"')");
                return;
            }
            
            parseConfigFile( xmlStream );
        }
        catch( IOException e )
        {
            log.error("Unable to read property file", e);
        }
        catch( JDOMException e )
        {
            log.error("Problem in the XML file",e);
        }
    }

    /**
     *  Parses the XML filters configuration file.
     *  
     * @param xmlStream
     * @throws JDOMException
     * @throws IOException
     */
    private void parseConfigFile( InputStream xmlStream )
        throws JDOMException,
               IOException
    {
        Document doc = new SAXBuilder().build( xmlStream );
        
        XPath xpath = XPath.newInstance("/pagefilters/filter");
    
        List nodes = xpath.selectNodes( doc );
        
        for( Iterator i = nodes.iterator(); i.hasNext(); )
        {
            Element f = (Element) i.next();
            
            String filterClass = f.getChildText("class");

            Properties props = new Properties();
            
            List params = f.getChildren("param");
            
            for( Iterator par = params.iterator(); par.hasNext(); )
            {
                Element p = (Element) par.next();
                
                props.setProperty( p.getChildText("name"), p.getChildText("value") );
            }

            initPageFilter( filterClass, props );
        }
        
    }
    
 
    /**
     *  Does the filtering before a translation.
     *  
     *  @param context The WikiContext
     *  @param pageData WikiMarkup data to be passed through the preTranslate chain.
     *  @throws FilterException If any of the filters throws a FilterException
     *  @return The modified WikiMarkup
     *  
     *  @see PageFilter#preTranslate(WikiContext, String)
     */
    public String doPreTranslateFiltering( WikiContext context, String pageData )
        throws FilterException
    {
        fireEvent( WikiPageEvent.PRE_TRANSLATE_BEGIN, context );

        for( PageFilter f : m_pageFilters )
        {
            pageData = f.preTranslate( context, pageData );
        }

        fireEvent( WikiPageEvent.PRE_TRANSLATE_END, context );

        return pageData;
    }

    /**
     *  Does the filtering after HTML translation.
     *  
     *  @param context The WikiContext
     *  @param htmlData HTML data to be passed through the postTranslate
     *  @throws FilterException If any of the filters throws a FilterException
     *  @return The modified HTML
     *  @see PageFilter#postTranslate(WikiContext, String)
     */
    public String doPostTranslateFiltering( WikiContext context, String htmlData )
        throws FilterException
    {
        fireEvent( WikiPageEvent.POST_TRANSLATE_BEGIN, context );

        for( PageFilter f : m_pageFilters )
        {
            htmlData = f.postTranslate( context, htmlData );
        }

        fireEvent( WikiPageEvent.POST_TRANSLATE_END, context );

        return htmlData;
    }

    /**
     *  Does the filtering before a save to the page repository.
     *  
     *  @param context The WikiContext
     *  @param pageData WikiMarkup data to be passed through the preSave chain.
     *  @throws FilterException If any of the filters throws a FilterException
     *  @return The modified WikiMarkup
     *  @see PageFilter#preSave(WikiContext, String)
     */
    public String doPreSaveFiltering( WikiContext context, String pageData )
        throws FilterException
    {
        fireEvent( WikiPageEvent.PRE_SAVE_BEGIN, context );

        for( PageFilter f : m_pageFilters )
        {
            pageData = f.preSave( context, pageData );
        }

        fireEvent( WikiPageEvent.PRE_SAVE_END, context );

        return pageData;
    }

    /**
     *  Does the page filtering after the page has been saved.
     * 
     *  @param context The WikiContext
     *  @param pageData WikiMarkup data to be passed through the postSave chain.
     *  @throws FilterException If any of the filters throws a FilterException
     * 
     *  @see PageFilter#postSave(WikiContext, String)
     */
    public void doPostSaveFiltering( WikiContext context, String pageData )
        throws FilterException
    {
        fireEvent( WikiPageEvent.POST_SAVE_BEGIN, context );

        for( PageFilter f : m_pageFilters )
        {
            // log.info("POSTSAVE: "+f.toString() );
            f.postSave( context, pageData );
        }

        fireEvent( WikiPageEvent.POST_SAVE_END, context );
    }

    /**
     *  Returns the list of filters currently installed.  Note that this is not
     *  a copy, but the actual list.  So be careful with it.
     *  
     *  @return A List of PageFilter objects
     */
    public List< PageFilter > getFilterList()
    {
        return m_pageFilters;
    }

    /**
     * 
     * Notifies PageFilters to clean up their ressources.
     *
     */
    public void destroy()
    {
        for( PageFilter f : m_pageFilters )
        {
            f.destroy( m_engine );
        }        
    }
    
    // events processing .......................................................

    /**
     *  Fires a WikiPageEvent of the provided type and WikiContext.
     *  Invalid WikiPageEvent types are ignored.
     *
     * @see org.apache.wiki.event.WikiPageEvent 
     * @param type      the WikiPageEvent type to be fired.
     * @param context   the WikiContext of the event.
     */
    public final void fireEvent( int type, WikiContext context )
    {
        if ( WikiEventManager.isListening(this) && WikiPageEvent.isValidType(type) )
        {
            WikiEventManager.fireEvent(this,
                    new WikiPageEvent(m_engine,type,context.getPage().getName()) );
        }
    }

    /**
     *  {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Collection modules()
    {
        ArrayList modules = new ArrayList();
        
        modules.addAll( m_pageFilters );
        
        return modules;
    }

    private void registerFilters()
    {
        log.info( "Registering filters" );

        SAXBuilder builder = new SAXBuilder();

        try
        {
            //
            // Register all filters which have created a resource containing its properties.
            //
            // Get all resources of all plugins.
            //

            Enumeration resources = getClass().getClassLoader().getResources( PLUGIN_RESOURCE_LOCATION );

            while( resources.hasMoreElements() )
            {
                URL resource = (URL) resources.nextElement();

                try
                {
                    log.debug( "Processing XML: " + resource );

                    Document doc = builder.build( resource );

                    List plugins = XPath.selectNodes( doc, "/modules/filter");

                    for( Iterator i = plugins.iterator(); i.hasNext(); )
                    {
                        Element pluginEl = (Element) i.next();

                        String className = pluginEl.getAttributeValue("class");

                        PageFilterInfo pluginInfo = PageFilterInfo.newInstance( className, pluginEl );

                        if( pluginInfo != null )
                        {
                            registerPlugin( pluginInfo );
                        }
                    }
                }
                catch( java.io.IOException e )
                {
                    log.error( "Couldn't load " + PLUGIN_RESOURCE_LOCATION + " resources: " + resource, e );
                }
                catch( JDOMException e )
                {
                    log.error( "Error parsing XML for filter: "+PLUGIN_RESOURCE_LOCATION );
                }
            }
        }
        catch( java.io.IOException e )
        {
            log.error( "Couldn't load all " + PLUGIN_RESOURCE_LOCATION + " resources", e );
        }
    }

    private void registerPlugin(PageFilterInfo pluginInfo)
    {
        m_filterClassMap.put( pluginInfo.getName(), pluginInfo );
    }

    /**
     *  Stores information about the filters.
     * 
     *  @since 2.6.1
     */
    private static final class PageFilterInfo extends WikiModuleInfo
    {
        private PageFilterInfo( String name )
        {
            super(name);
        }
        
        protected static PageFilterInfo newInstance(String className, Element pluginEl)
        {
            if( className == null || className.length() == 0 ) return null;
            PageFilterInfo info = new PageFilterInfo( className );

            info.initializeFromXML( pluginEl );
            return info;        
        }
    }
}
