package com.ecyrd.jspwiki.event;

import java.util.EventObject;

/**
 * Abstract parent class for wiki events.
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2006-06-17 23:12:56 $
 * @see com.ecyrd.jspwiki.auth.WikiSecurityEvent
 * @since 2.3.79
 */
public abstract class WikiEvent extends EventObject
{
    private final int m_type;

    /**
     * Constructs an instance of this event.
     * @param source the Object that is the source of the event.
     */
    public WikiEvent( Object source, int type )
    {
        super( source );
        m_type = type;
    }

    /**
     * Returns the type of event.
     * @return the event type
     */
    public final int getType()
    {
        return m_type;
    }

}
