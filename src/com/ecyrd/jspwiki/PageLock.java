package com.ecyrd.jspwiki;

import java.io.Serializable;
import java.util.Date;

public class PageLock
    implements Serializable
{
    private String   m_page;
    private String   m_locker;
    private Date     m_lockAcquired;
    private Date     m_lockExpiry;

    public PageLock( WikiPage page, 
                     String locker,
                     Date acquired,
                     Date expiry )
    {
        m_page         = page.getName();
        m_locker       = locker;
        m_lockAcquired = acquired;
        m_lockExpiry   = expiry;
    }

    public String getPage()
    {
        return m_page;
    }

    public String getLocker()
    {
        return m_locker;
    }

    public Date getAcquisitionTime()
    {
        return m_lockAcquired;
    }

    public Date getExpiryTime()
    {
        return m_lockExpiry;
    }

    /**
     *  Returns the amount of time left in minutes, rounded up to the nearest
     *  minute (so you get a zero only at the last minute).
     */
    public long getTimeLeft()
    {
        long time = m_lockExpiry.getTime() - new Date().getTime();

        return (time / (1000L * 60)) + 1;
    }
}
