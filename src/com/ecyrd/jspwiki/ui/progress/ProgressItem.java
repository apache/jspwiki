/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.ui.progress;

/**
 *  Provides access to an progress item.
 *
 *  @author Janne Jalkanen
 *  @since  2.6
 */
public abstract class ProgressItem
{
    public static final int CREATED  = 0;
    public static final int STARTED  = 1;
    public static final int STOPPED  = 2;
    public static final int FINISHED = 3;

    protected int m_state = CREATED;

    public int getState()
    {
        return m_state;
    }

    public void setState( int state )
    {
        m_state = state;
    }

    /**
     *  Returns the progress in percents.
     *  @return An integer 0-100.
     */
    public abstract int getProgress();
}
