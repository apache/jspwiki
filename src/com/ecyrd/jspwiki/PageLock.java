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

import java.io.Serializable;
import java.util.Date;

/**
 *  Describes a lock acquired by an user on a page.  For the most part,
 *  the regular developer does not have to instantiate this class.
 *  <p>
 *  The PageLock keeps no reference to a WikiPage because otherwise it could
 *  keep a reference to a page for a long time.
 *  
 *  @author Janne Jalkanen
 *
 */
public class PageLock
    implements Serializable
{
    private static final long serialVersionUID = 0L;
    
    private String   m_page;
    private String   m_locker;
    private Date     m_lockAcquired;
    private Date     m_lockExpiry;

    /**
     *  Creates a new PageLock.  The lock is not attached to any objects at this point.
     *  
     *  @param page     WikiPage which is locked.
     *  @param locker   The username who locked this page (for display purposes).
     *  @param acquired The timestamp when the lock is acquired
     *  @param expiry   The timestamp when the lock expires.
     */
    public PageLock( WikiPage page, 
                     String locker,
                     Date acquired,
                     Date expiry )
    {
        m_page         = page.getName();
        m_locker       = locker;
        m_lockAcquired = (Date)acquired.clone();
        m_lockExpiry   = (Date)expiry.clone();
    }

    /**
     *  Returns the name of the page which is locked.
     *  
     *  @return The name of the page.
     */
    public String getPage()
    {
        return m_page;
    }

    /**
     *  Returns the locker name.
     *  
     *  @return The name of the locker.
     */
    public String getLocker()
    {
        return m_locker;
    }

    /**
     *  Returns the timestamp on which this lock was acquired.
     *  
     *  @return The acquisition time.
     */
    public Date getAcquisitionTime()
    {
        return m_lockAcquired;
    }

    /**
     *  Returns the timestamp on which this lock will expire.
     *  
     *  @return The expiry date.
     */
    public Date getExpiryTime()
    {
        return m_lockExpiry;
    }

    /**
     *  Returns the amount of time left in minutes, rounded up to the nearest
     *  minute (so you get a zero only at the last minute).
     *  
     *  @return Time left in minutes.
     */
    public long getTimeLeft()
    {
        long time = m_lockExpiry.getTime() - new Date().getTime();

        return (time / (1000L * 60)) + 1;
    }
    
    // FIXME: Should really have a isExpired() method as well.
}
