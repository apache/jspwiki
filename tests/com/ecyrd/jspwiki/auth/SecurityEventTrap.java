package com.ecyrd.jspwiki.auth;

import java.util.ArrayList;
import java.util.List;

import com.ecyrd.jspwiki.WikiEvent;
import com.ecyrd.jspwiki.WikiEventListener;

/**
 * Traps the most recent WikiEvent so that it can be used in assertions.
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-02-21 08:44:09 $
 * @since 2.3.79
 */
public class SecurityEventTrap implements WikiEventListener
{
    private WikiSecurityEvent m_lastEvent = null;
    private List              m_events    = new ArrayList();

    public void actionPerformed( WikiEvent event )
    {
        if ( event instanceof WikiSecurityEvent )
        {
            m_lastEvent = (WikiSecurityEvent)event;
            m_events.add( event );
        }
        else
        {
            throw new IllegalArgumentException( "Event wasn't a WikiSecurityEvent. Check the unit test code!" );
        }
    }
    
    public WikiSecurityEvent lastEvent()
    {
        return m_lastEvent;
    }
    
    public void clearEvents()
    {
        m_events.clear();
    }
    
    public WikiSecurityEvent[] events() 
    {
        return (WikiSecurityEvent[])m_events.toArray(new WikiSecurityEvent[m_events.size()]);
    }

}
