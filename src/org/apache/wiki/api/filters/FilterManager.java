package org.apache.wiki.api.filters;

import java.util.Collection;
import java.util.List;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.FilterException;

public interface FilterManager
{
    /** Property name for setting the filter XML property file.  Value is <tt>{@value}</tt>. */
    static final String PROP_FILTERXML = "jspwiki.filterConfig";
    
    /** Default location for the filter XML property file.  Value is <tt>{@value}</tt>. */
    static final String DEFAULT_XMLFILE = "/WEB-INF/filters.xml";

    /** JSPWiki system filters are all below this value. */
    static final int SYSTEM_FILTER_PRIORITY = -1000;
    
    /** The standard user level filtering. */
    static final int USER_FILTER_PRIORITY   = 0;
    
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
    void addPageFilter( PageFilter f, int priority ) throws IllegalArgumentException;
    
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
    String doPreTranslateFiltering( WikiContext context, String pageData ) throws FilterException;
    
    /**
     *  Does the filtering after HTML translation.
     *  
     *  @param context The WikiContext
     *  @param htmlData HTML data to be passed through the postTranslate
     *  @throws FilterException If any of the filters throws a FilterException
     *  @return The modified HTML
     *  @see PageFilter#postTranslate(WikiContext, String)
     */
    String doPostTranslateFiltering( WikiContext context, String htmlData ) throws FilterException;
    
    /**
     *  Does the filtering before a save to the page repository.
     *  
     *  @param context The WikiContext
     *  @param pageData WikiMarkup data to be passed through the preSave chain.
     *  @throws FilterException If any of the filters throws a FilterException
     *  @return The modified WikiMarkup
     *  @see PageFilter#preSave(WikiContext, String)
     */
    String doPreSaveFiltering( WikiContext context, String pageData ) throws FilterException;
    
    /**
     *  Does the page filtering after the page has been saved.
     * 
     *  @param context The WikiContext
     *  @param pageData WikiMarkup data to be passed through the postSave chain.
     *  @throws FilterException If any of the filters throws a FilterException
     * 
     *  @see PageFilter#postSave(WikiContext, String)
     */
    void doPostSaveFiltering( WikiContext context, String pageData ) throws FilterException;
    
    /**
     *  Returns the list of filters currently installed.  Note that this is not
     *  a copy, but the actual list.  So be careful with it.
     *  
     *  @return A List of PageFilter objects
     */
    List< PageFilter > getFilterList();
    
    /**
     * Notifies PageFilters to clean up their ressources.
     */
    void destroy();
    
    /**
     * Returns a collection of modules currently managed by this ModuleManager.  Each
     * entry is an instance of the WikiModuleInfo class.  This method should return something
     * which is safe to iterate over, even if the underlying collection changes.
     * 
     * @return A Collection of WikiModuleInfo instances.
     */
    Collection modules();
}
