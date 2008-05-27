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
package com.ecyrd.jspwiki.util;

/**
 *  A watchdog needs something to watch.  If you wish to be watched,
 *  implement this interface.
 *
 *  @author jalkanen
 */
public interface Watchable
{
    /**
     *  This is a callback which is called whenever your expected
     *  completion time is exceeded.  The current state of the
     *  stack is available.
     *
     *  @param state The state in which your Watchable is currently.
     */
    public void timeoutExceeded( String state );

    /**
     *  Returns a human-readable name of this Watchable.  Used in
     *  logging.
     *
     *  @return The name of the Watchable.
     */
    public String getName();

    /**
     *  Returns <code>true</code>, if this Watchable is still alive and can be
     *  watched; otherwise <code>false</code>. For example, a stopped Thread
     *  is not very interesting to watch.
     *
     *  @return the result
     */
    public boolean isAlive();
}
