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

import java.util.List;
import java.util.Properties;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiException;


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
 *  @deprecated will be removed in 2.10 scope. Consider using {@link DefaultFilterManager} instead
 */
@Deprecated
public final class FilterManager extends DefaultFilterManager
{
    /**
     *  Constructs a new FilterManager object.
     *  
     *  @param engine The WikiEngine which owns the FilterManager
     *  @param props Properties to initialize the FilterManager with
     *  @throws WikiException If something goes wrong.
     */
    public FilterManager( WikiEngine engine, Properties props )
        throws WikiException
    {
        super( engine, props );
    }
    
    public List getFilterList()
    {
        return super.getFilterList();
    }
   
}
