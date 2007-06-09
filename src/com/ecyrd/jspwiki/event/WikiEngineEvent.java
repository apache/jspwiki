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

import com.ecyrd.jspwiki.WikiEngine;

/**
  * WikiEngineEvent indicates a change in the state of the WikiEngine.
  * 
  * @author  Murray Altheim
  * @author  Andrew Jaquith
  * @see     com.ecyrd.jspwiki.event.WikiEvent
  * @since   2.4.20
  */
public class WikiEngineEvent extends WikiEvent
{
    private static final long serialVersionUID = 1829433967558773970L;

    /** Indicates a WikiEngine initialization event, fired as the 
      * wiki service is being initialized (in progress). */
    public static final int INITIALIZING   = -1;

    /** Indicates a WikiEngine initialized event, fired after the 
      * wiki service is fully available. */
    public static final int INITIALIZED    = 0;

    /** Indicates a WikiEngine closing event, fired as a signal that
      * the wiki service is shutting down. */
    public static final int SHUTDOWN       = 1;

    /** Indicates a WikiEngine stopped event, fired after halting the wiki service.
      * A WikiEngine in this state is not expected to provide further services. 
      */
    public static final int STOPPED        = 2;

    private WikiEngine m_engine;

    // ............


     /**
      *  Constructs an instance of this event.
      * @param eventSource  the Object that is the source of the event,
      * which <b>must</b> be the WikiEngine. If it is not, this
      * method thows a ClassCastException
      * @param type the event type
      */
    public WikiEngineEvent( Object eventSource, int type )
    {
        super( eventSource, type );
        m_engine = (WikiEngine)eventSource;
    }


    /**
     *  Sets the type of this event.
     *
     * @param type      the type of this WikiEngineEvent.
     */
    protected void setType( int type )
    {
        if ( type >= INITIALIZING && type <= STOPPED )
        {
            super.setType(type);
        }
        else
        {
            super.setType(ERROR);
        }
    }


    /**
     *  Returns the WikiEngine that spawned this event.
     *
     * @return  the WikiEngine that spawned this event.
     */
    public WikiEngine getEngine()
    {
        return m_engine;
    }


    /**
     *  Returns the WikiEngine that spawned this event.
     *
     * @return  the WikiEngine that spawned this event.
     * @deprecated  use {@link #getEngine()} instead.
     */
    public WikiEngine getWikiEngine()
    {
        return m_engine;
    }


   /**
     * Returns <code>true</code> if the int value is a WikiPageEvent type.
     * @param type the event type
     * @return the result
     */
    public static boolean isValidType( int type )
    {
        return type >= INITIALIZING && type <= STOPPED;
    }


    /**
     *  Returns a textual representation of the event type.
     *
     * @return a String representation of the type
     */
    public final String eventName()
    {
        switch ( getType() )
        {
            case INITIALIZING:         return "INITIALIZING";
            case INITIALIZED:          return "INITIALIZED";
            case SHUTDOWN:             return "SHUTDOWN";
            case STOPPED:              return "STOPPED";
            default:                   return super.eventName();
        }
    }


    /**
     *  Returns a human-readable description of the event type.
     *
     * @return a String description of the type
     */
    public final String getTypeDescription()
    {
        switch ( getType() )
        {
            case INITIALIZING:         return "wiki engine initializing";
            case INITIALIZED:          return "wiki engine initialized";
            case SHUTDOWN:             return "wiki engine shutting down";
            case STOPPED:              return "wiki engine stopped";
            default:                   return super.getTypeDescription();
        }
    }

} // end class com.ecyrd.jspwiki.event.WikiEngineEvent
