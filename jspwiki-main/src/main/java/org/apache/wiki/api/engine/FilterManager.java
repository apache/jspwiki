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
package org.apache.wiki.api.engine;

import java.util.Collection;
import java.util.List;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.filters.PageFilter;
import org.apache.wiki.modules.WikiModuleInfo;

public interface FilterManager
{
    /** Property name for setting the filter XML property file.  Value is <tt>{@value}</tt>. */
    String PROP_FILTERXML = "jspwiki.filterConfig";
    
    /** Default location for the filter XML property file.  Value is <tt>{@value}</tt>. */
    String DEFAULT_XMLFILE = "/WEB-INF/filters.xml";

    /** JSPWiki system filters are all below this value. */
    int SYSTEM_FILTER_PRIORITY = -1000;
    
    /** The standard user level filtering. */
    int USER_FILTER_PRIORITY   = 0;
    
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
     * entry is an instance of the {@link WikiModuleInfo} class.  This method should return something
     * which is safe to iterate over, even if the underlying collection changes.
     * 
     * @return A Collection of {@link WikiModuleInfo} instances.
     */
    Collection< WikiModuleInfo > modules();
    
    /**
     * Returns the {@link WikiModuleInfo} information about the provided filterName.
     * @param filterName
     * @return The WikiModuleInfo
     */
    WikiModuleInfo getModuleInfo(String filterName);
}
