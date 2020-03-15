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
package org.apache.wiki.api.filters;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.FilterException;

import java.lang.reflect.Method;
import java.util.Properties;

import static org.apache.wiki.api.filters.FilterSupportOperations.executePageFilterPhase;
import static org.apache.wiki.api.filters.FilterSupportOperations.methodOfNonPublicAPI;


/**
 * Hooks all filters not using the public api into it.
 *
 * @deprecated use {@link BasePageFilter} instead
 * @see BasePageFilter
 */
@Deprecated
public class BasicPageFilter extends BasePageFilter {

    public void initialize( final WikiEngine engine, final Properties properties ) throws FilterException {
        this.m_engine = engine;
        Logger.getLogger( BasicPageFilter.class ).warn( this.getClass().getName() + " implements deprecated org.apache.wiki.api.filters.BasicPageFilter" );
        Logger.getLogger( BasicPageFilter.class ).warn( "Please contact the filter's author so there can be a new release of the filter " +
                                                        "extending the new org.apache.wiki.api.filters.BasePageFilter class" );
    }

    public String preTranslate( final WikiContext wikiContext, final String content ) throws FilterException {
        return content;
    }

    public String preTranslate( final Context wikiContext, final String content ) throws FilterException {
        final Method m = methodOfNonPublicAPI( this, "preTranslate", "org.apache.wiki.WikiContext", "java.lang.String" );
        return executePageFilterPhase( () -> content, m, this, wikiContext, content );
        // return content;
    }

    public String postTranslate( final WikiContext wikiContext, final String htmlContent ) throws FilterException {
        return htmlContent;
    }

    public String postTranslate( final Context wikiContext, final String htmlContent ) throws FilterException {
        final Method m = methodOfNonPublicAPI( this, "postTranslate", "org.apache.wiki.WikiContext", "java.lang.String" );
        return executePageFilterPhase( () -> htmlContent, m, this, wikiContext, htmlContent );
        // return htmlContent;
    }

    public String preSave( final WikiContext wikiContext, final String content ) throws FilterException {
        return content;
    }

    public String preSave( final Context wikiContext, final String content ) throws FilterException {
        final Method m = methodOfNonPublicAPI( this, "preSave", "org.apache.wiki.WikiContext", "java.lang.String" );
        return executePageFilterPhase( () -> content, m, this, wikiContext, content );
        // return content;
    }

    public void postSave( final WikiContext wikiContext, final String content ) throws FilterException {
    }

    public void postSave( final Context wikiContext, final String content ) throws FilterException {
        final Method m = methodOfNonPublicAPI( this, "postSave", "org.apache.wiki.WikiContext", "java.lang.String" );
        executePageFilterPhase( () -> null, m, this, content );
        // empty method
    }

    public void destroy( final WikiEngine engine ) {
    }

    public void destroy( final Engine engine ) {
        final Method m = methodOfNonPublicAPI( this, "destroy", "org.apache.wiki.WikiEngine" );
        executePageFilterPhase( () -> null, m, this, engine );
        // empty method
    }

}
