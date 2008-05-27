/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.util.Properties;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Provides a base implementation of a PageFilter.  None of the callbacks
 *  do anything, so it is a good idea for you to extend from this class
 *  and implement only methods that you need.
 *
 *  @author Janne Jalkanen
 */
public class BasicPageFilter
    implements PageFilter
{
    protected WikiEngine m_engine;
  
    /**
     *  If you override this, you should call super.initialize() first.
     */
    public void initialize( WikiEngine engine, Properties properties )
        throws FilterException
    {
        m_engine = engine;
    }

    public String preTranslate( WikiContext wikiContext, String content )
        throws FilterException
    {
        return content;
    }

    public String postTranslate( WikiContext wikiContext, String htmlContent )
        throws FilterException
    {
        return htmlContent;
    }

    public String preSave( WikiContext wikiContext, String content )
        throws FilterException
    {
        return content;
    }

    public void postSave( WikiContext wikiContext, String content )
        throws FilterException
    {
    }
    
    public void destroy( WikiEngine engine ) 
    {
    }
}
