/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.filters;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.Properties;
import java.util.Iterator;
import java.util.List;

import org.xml.sax.*;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;

import com.ecyrd.jspwiki.util.PriorityList;
import com.ecyrd.jspwiki.util.ClassUtil;


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
 *      <class>com.ecyrd.jspwiki.filters.ProfanityFilter</class>
 *    </filter>
 *  
 *    <filter>
 *      <class>com.ecyrd.jspwiki.filters.TestFilter</class>
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
 *  @author Janne Jalkanen
 */
public class FilterManager
    extends HandlerBase
{
    private PriorityList     m_pageFilters = new PriorityList();

    private static final Logger log = Logger.getLogger(WikiEngine.class);

    public static final String PROP_FILTERXML = "jspwiki.filterConfig";

    public static final String DEFAULT_XMLFILE = "/filters.xml";

    public FilterManager( WikiEngine engine, Properties props )
        throws WikiException
    {
        initialize( engine, props );
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
    public void addPageFilter( PageFilter f, int priority )
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
            int priority = 0; // FIXME: Currently fixed.

            Class cl = ClassUtil.findClass( "com.ecyrd.jspwiki.filters",
                                            className );

            PageFilter filter = (PageFilter)cl.newInstance();

            filter.initialize( props );

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
     */

    public void initialize( WikiEngine engine, Properties props )
        throws WikiException
    {
        InputStream xmlStream = null;
        String      xmlFile   = props.getProperty( PROP_FILTERXML );

        try
        {
            if( xmlFile == null )
            {
                log.debug("Attempting to locate "+DEFAULT_XMLFILE+" from class path.");
                xmlStream = getClass().getResourceAsStream( DEFAULT_XMLFILE );
            }
            else
            {
                log.debug("Attempting to load property file "+xmlFile);
                xmlStream = new FileInputStream( new File(xmlFile) );
            }

            if( xmlStream == null )
            {
                log.info("Cannot find property file for filters (this is okay, expected to find it as: '"+ (xmlFile == null ? DEFAULT_XMLFILE : xmlFile ) +"')");
                return;
            }
            Parser parser = new uk.co.wilson.xml.MinML(); // FIXME: Should be settable

            parser.setDocumentHandler( this );
            parser.setErrorHandler( this );
            
            parser.parse( new InputSource(xmlStream) );

        }
        catch( IOException e )
        {
            log.error("Unable to read property file", e);
        }
        catch( SAXException e )
        {
            log.error("Problem in the XML file",e);
        }
    }

    /*
     *  The XML parsing part.  We use a simple SAX1 parser; we do not at this
     *  point need anything more complicated.
     */
    private String filterName = null;
    private Properties filterProperties = new Properties();
    private boolean parsingFilters = false;
    private String lastReadCharacters = null;
    private String lastReadParamName = null;
    private String lastReadParamValue = null;

    public void startElement( String name, AttributeList atts )
    {
        if( "pagefilters".equals(name) )
        {
            parsingFilters = true;
        }
        else if( parsingFilters )
        {
            if( "filter".equals(name) )
            {
                filterName = null;
            }
        }
    }



    public void endElement( String name )
    {
        if( "pagefilters".equals(name) )
        {
            parsingFilters = false;
        }
        else if( parsingFilters )
        {
            if( "filter".equals(name) )
            {
                initPageFilter( filterName, filterProperties );
            }
            else if( "class".equals(name) )
            {
                filterName = lastReadCharacters;
            }
            else if( "param".equals(name) )
            {               
                filterProperties.setProperty( lastReadParamName, lastReadParamValue );
            }
            else if( "name".equals(name) )
            {
                lastReadParamName = lastReadCharacters;
            }
            else if( "value".equals(name) )
            {
                lastReadParamValue = lastReadCharacters;
            }
        }
    }

    public void characters( char ch[], int start, int length )
    {
        lastReadCharacters = new String( ch, start, length );
    }
    

    /**
     *  Does the filtering before a translation.
     */
    public String doPreTranslateFiltering( WikiContext context, String pageData )
        throws FilterException
    {
        for( Iterator i = m_pageFilters.iterator(); i.hasNext(); )
        {
            PageFilter f = (PageFilter) i.next();

            pageData = f.preTranslate( context, pageData );
        }

        return pageData;
    }

    /**
     *  Does the filtering after HTML translation.
     */
    public String doPostTranslateFiltering( WikiContext context, String pageData )
        throws FilterException
    {
        for( Iterator i = m_pageFilters.iterator(); i.hasNext(); )
        {
            PageFilter f = (PageFilter) i.next();

            pageData = f.postTranslate( context, pageData );
        }

        return pageData;
    }

    /**
     *  Does the filtering before a save to the page repository.
     */
    public String doPreSaveFiltering( WikiContext context, String pageData )
        throws FilterException
    {
        for( Iterator i = m_pageFilters.iterator(); i.hasNext(); )
        {
            PageFilter f = (PageFilter) i.next();

            pageData = f.preSave( context, pageData );
        }

        return pageData;
    }

    /**
     *  Does the page filtering after the page has been saved.
     */
    public void doPostSaveFiltering( WikiContext context, String pageData )
        throws FilterException
    {
        for( Iterator i = m_pageFilters.iterator(); i.hasNext(); )
        {
            PageFilter f = (PageFilter) i.next();

            f.postSave( context, pageData );
        }
    }

    public List getFilterList()
    {
        return m_pageFilters;
    }
}
