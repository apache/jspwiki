/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 JSPWiki development group

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
package com.ecyrd.jspwiki.ui.admin;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.ui.GenericHTTPHandler;

/**
 *  Describes an administrative bean.
 *  
 *  @author Janne Jalkanen
 *  @since  2.5.52
 */
public interface AdminBean
    extends GenericHTTPHandler
{
    public static final int UNKNOWN = 0;
    public static final int CORE    = 1;
    public static final int EDITOR  = 2;
    
    public void initialize( WikiEngine engine );
    
    /**
     *  Return a human-readable title for this AdminBean.
     *  
     *  @return the bean's title
     */
    public String getTitle();
    
    /**
     *  Returns a type (UNKNOWN, EDITOR, etc).
     *  
     *  @return the bean's type
     */
    public int getType();
}
