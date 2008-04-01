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
package com.ecyrd.jspwiki.parser;

import java.lang.ref.WeakReference;

import org.jdom.Document;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;

/**
 *  Stores the DOM tree of a rendered WikiPage.  This class
 *  extends the org.jdom.Document to provide some extra metadata
 *  specific to JSPWiki.
 *  <p>
 *  The document is not stored as metadata in the WikiPage because
 *  otherwise it could not be cached separately.
 *  
 *  @since  2.4
 */
public class WikiDocument extends Document
{
    private static final long serialVersionUID = 0L;
    
    private WikiPage m_page;
    private String   m_wikiText;

    private WeakReference m_context;
    
    /**
     *  Creates a new WikiDocument for a specific page.
     * 
     *  @param page The page to which this document refers to.
     */
    public WikiDocument( WikiPage page )
    {
        m_page     = page;
    }
    
    public void setPageData( String data )
    {
        m_wikiText = data;
    }
    
    public String getPageData()
    {
        return m_wikiText;
    }
    
    public WikiPage getPage()
    {
        return m_page;
    }

    public void setContext( WikiContext ctx )
    {
        m_context = new WeakReference( ctx );
    }
    
    /**
     * Returns the wiki context for this document. This method
     * may return <code>null</code> if the associated wiki session
     * had previously been garbage-collected.
     * @return the wiki context
     */
    public WikiContext getContext()
    {
        return (WikiContext) m_context.get();
    }
}
