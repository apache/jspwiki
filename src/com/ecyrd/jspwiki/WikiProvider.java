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
package com.ecyrd.jspwiki;

import java.util.Properties;
import java.io.IOException;

/**
 *  A generic Wiki provider for all sorts of things that the Wiki can
 *  store.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public interface WikiProvider
{
    /**
     *  Passing this to any method should get the latest version
     */
    public static final int LATEST_VERSION = -1;

    /**
     *  Initializes the page provider.
     */
    public void initialize( Properties properties ) 
        throws NoRequiredPropertyException,
               IOException;

    /**
     *  Return a valid HTML string for information.  May
     *  be anything.
     *  @since 1.6.4
     */

    public String getProviderInfo();
}


