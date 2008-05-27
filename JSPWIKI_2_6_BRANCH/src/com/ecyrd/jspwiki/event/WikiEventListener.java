/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

package com.ecyrd.jspwiki.event;

import  java.util.EventListener;

/**
  * Defines an interface for an object that listens for WikiEvents.
  *
  * @author  Murray Altheim
  * @since   2.3.92
  */
public interface WikiEventListener extends EventListener
{

   /**
     * Fired when a WikiEvent is triggered by an event source.
     *
     * @param event    a WikiEvent object
     */
    public void actionPerformed( WikiEvent event );


} // end com.ecryd.jspwiki.event.WikiEventListener
