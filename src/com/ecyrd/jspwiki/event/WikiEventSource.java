package com.ecyrd.jspwiki.event;

/**
 * Interface for types that generate {@link WikiEvent}s for others
 * to consume.
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2006-07-23 21:44:38 $
 * @since 2.4.22
 */
public interface WikiEventSource
{

    /**
     * Registers a WikiEventListener with this instance.
     * @param listener the event listener
     */
    public void addWikiEventListener( WikiEventListener listener );

    /**
     * Un-registers a WikiEventListener with this instance.
     * @param listener the event listener
     */
    public void removeWikiEventListener( WikiEventListener listener );

}