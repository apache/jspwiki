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
package com.ecyrd.jspwiki.providers;

/**
 *  This is a provider interface which providers can implement, if they
 *  support fast checks of versions.
 *  <p>
 *  Note that this interface is pretty much a hack to support certain functionality
 *  before a complete refactoring of the complete provider interface.  Please
 *  don't bug me too much about it...
 *  
 *  @author jalkanen
 *
 *  @since 2.3.29
 */
public interface VersioningProvider
{
    /**
     *  Return true, if page with a particular version exists.
     */

    public boolean pageExists( String page, int version );
}
