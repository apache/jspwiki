package com.ecyrd.jspwiki.event;

/**
 * Listener interface for notification of WikiEvents.
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-02-23 20:51:31 $
 * @since 2.3.79
 */
public interface WikiEventListener
{

    /**
     * Fired when a WikiEvent is triggered by an event source.
     * @param event the event
     */
    public void actionPerformed( WikiEvent event );

}
