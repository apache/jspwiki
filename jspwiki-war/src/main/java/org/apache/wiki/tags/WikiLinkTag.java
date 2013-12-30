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
package org.apache.wiki.tags;

import java.io.IOException;

/**
 *  Root class for different internal wiki links.  Cannot be used directly,
 *  but provides basic stuff for other classes.
 *  <P>
 *  Extend from this class if you need the following attributes.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *    <li>format - Either "url" or "anchor".  If "url", will provide
 *  just the URL for the link.  If "anchor", will output proper HTML
 *  (&lt;a&gt; href="...).
 *  </UL>
 *
 *  @since 2.0
 */
public abstract class WikiLinkTag extends WikiTagBase {

	private static final long serialVersionUID = 4130732879352134867L;
	public static final int   ANCHOR = 0;
    public static final int   URL    = 1;

    protected String m_pageName;
    protected int    m_format = ANCHOR;
    protected String m_template;

    
    public void initTag() 
    {
        super.initTag();
        m_pageName = m_template = null;
        m_format = ANCHOR;
    }
    
    public void setPage( String page )
    {
        m_pageName = page;
    }

    public String getPage()
    {
        return m_pageName;
    }


    public String getTemplate()
    {
        return m_template;
    }

    public void setTemplate( String arg )
    {
        m_template = arg;
    }

    public void setFormat( String mode )
    {
        if( "url".equalsIgnoreCase(mode) )
        {
            m_format = URL;
        }
        else
        {
            m_format = ANCHOR;
        }
    }

    public int doEndTag()
    {
        try
        {
            if( m_format == ANCHOR )
            {
                pageContext.getOut().print("</a>");
            }
        }
        catch( IOException e )
        {
            // FIXME: Should do something?
        }

        return EVAL_PAGE;
    }
}
