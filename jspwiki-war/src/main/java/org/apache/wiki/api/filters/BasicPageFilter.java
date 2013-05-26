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

import java.util.Properties;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.FilterException;

/**
 *  Provides a base implementation of a PageFilter.  None of the callbacks
 *  do anything, so it is a good idea for you to extend from this class
 *  and implement only methods that you need.
 *
 */
public class BasicPageFilter
    implements PageFilter
{
    protected WikiEngine m_engine;
  
    /**
     *  If you override this, you should call super.initialize() first.
     *  
     *  {@inheritDoc}
     */
    public void initialize( WikiEngine engine, Properties properties )
        throws FilterException
    {
        m_engine = engine;
    }

    /**
     *  {@inheritDoc}
     */
    public String preTranslate( WikiContext wikiContext, String content )
        throws FilterException
    {
        return content;
    }

    /**
     *  {@inheritDoc}
     */
    public String postTranslate( WikiContext wikiContext, String htmlContent )
        throws FilterException
    {
        return htmlContent;
    }

    /**
     *  {@inheritDoc}
     */
    public String preSave( WikiContext wikiContext, String content )
        throws FilterException
    {
        return content;
    }

    /**
     *  {@inheritDoc}
     */
    public void postSave( WikiContext wikiContext, String content )
        throws FilterException
    {
    }
    
    /**
     *  {@inheritDoc}
     */
    public void destroy( WikiEngine engine ) 
    {
    }
}
