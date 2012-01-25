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

package org.apache.wiki.util;

import java.util.*;

import org.apache.log4j.Logger;

import org.apache.wiki.WikiPage;
import org.apache.wiki.util.comparators.JavaNaturalComparator;

/**
 * Wrapper class for managing and using the PageNameComparator.
 * <p>
 * <b>Note</b> - this class is deliberately not null safe. Never call any of the
 * methods with a null argument!
 */
public class PageSorter implements Comparator
{
    private static Logger log = Logger.getLogger( PageSorter.class );

    // The name of the property that specifies the desired page name comparator
    protected static final String PROP_PAGE_NAME_COMPARATOR = "jspwiki.pageNameComparator.class";

    private Comparator<String> m_comparator;

    /**
     * Default constructor uses Java "natural" ordering.
     */
    public PageSorter()
    {
        m_comparator = JavaNaturalComparator.DEFAULT_JAVA_COMPARATOR;
    }

    /**
     * Construct with a particular comparator.
     * 
     * @param comparator the Comparator to use
     */
    public PageSorter( Comparator<String> comparator )
    {
        m_comparator = comparator;
    }

    /**
     * Compare two page names (Object version). Arguments must be either String
     * or WikiPage.
     * 
     * @throws IllegalArgumentException if incorrect argument types.
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( Object o1, Object o2 )
    {
        if( o1 instanceof String )
        {
            if( o2 instanceof String )
                return m_comparator.compare( (String) o1, (String) o2 );
            if( o2 instanceof WikiPage )
                return m_comparator.compare( (String) o1, ((WikiPage) o2).getName() );
        }
        else if( o1 instanceof WikiPage )
        {
            if( o2 instanceof WikiPage )
                return m_comparator.compare( ((WikiPage) o1).getName(), ((WikiPage) o2).getName() );
            if( o2 instanceof String )
                return m_comparator.compare( ((WikiPage) o1).getName(), (String) o2 );
        }

        throw new IllegalArgumentException( "Can only compare String or WikiPage" );
    }

    /**
     * Compare two page names (String version).
     * 
     * @param pageName1 the first page name
     * @param pageName2 the second page name
     * @return see java.util.Comparator
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( String pageName1, String pageName2 )
    {
        return m_comparator.compare( pageName1, pageName2 );
    }

    /**
     * Compare two pages (WikiPage version). Compares them by name first. If the
     * same name, compares their versions.
     * 
     * @param page1 the first page
     * @param page2 the second page
     * @return see java.util.Comparator
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( WikiPage page1, WikiPage page2 )
    {
        if( page1 == page2 )
            return 0; // the same object

        int res = m_comparator.compare( page1.getName(), page2.getName() );
        if( res == 0 )
            res = page1.getVersion() - page2.getVersion();
        return res;
    }

    @Override
    public boolean equals( Object o )
    {
        if( !(o instanceof PageSorter) )
            return false; // Definitely not equal

        PageSorter that = (PageSorter) o;
        if( this == that || m_comparator == that.m_comparator )
            return true; // Essentially the same object
        return m_comparator.equals( that.m_comparator );
    }

    /**
     * Called by WikiEngine to initialise this instance. Tries to use class
     * given by the PROP_PAGE_NAME_COMPARATOR property as the page name
     * comparator. Uses a default comparator if this property is not set or
     * there is any problem loading the specified class.
     * 
     * @param props this WikiEngine's properties.
     */
    @SuppressWarnings( "unchecked" )
    public void initialize( Properties props )
    {
        // Default is Java natural order
        m_comparator = JavaNaturalComparator.DEFAULT_JAVA_COMPARATOR;
        String className = props.getProperty( PROP_PAGE_NAME_COMPARATOR );
        if( className != null && className.length() > 0 )
            try
            {
                m_comparator = (Comparator<String>) ClassUtil.findClass( "org.apache.wiki.util.comparators", className )
                    .newInstance();
            }
            catch( Exception e )
            {
                log.error( "Falling back to default \"natural\" comparator", e );
            }
    }

    /**
     * Sorts the specified list into ascending order based on the
     * PageNameComparator. The actual sort is done using
     * <code>Collections.sort()</code>.
     * 
     * @param nameList the page names to be sorted
     */
    public void sort( List<String> nameList )
    {
        Collections.sort( nameList, m_comparator );
    }

    /**
     * Sorts the specified array into ascending order based on the
     * PageNameComparator. The actual sort is done using
     * <code>Arrays.sort()</code>.
     * 
     * @param nameArray the page names to be sorted
     */
    public void sort( String[] nameArray )
    {
        Arrays.sort( nameArray, m_comparator );
    }

    /**
     * Sorts the specified list into ascending order based on the
     * PageNameComparator. The actual sort is done using
     * <code>Collections.sort()</code>.
     * 
     * @param pageList the pages to be sorted
     */
    @SuppressWarnings( "unchecked" )
    public void sortPages( List<Object> pageList )
    {
        Collections.sort( pageList, this );
    }

    /**
     * Sorts the specified array into ascending order based on the
     * PageNameComparator. The actual sort is done using
     * <code>Arrays.sort()</code>.
     * 
     * @param pageArray the pages to be sorted
     */
    @SuppressWarnings( "unchecked" )
    public void sortPages( Object[] pageArray )
    {
        Arrays.sort( pageArray, this );
    }
}
