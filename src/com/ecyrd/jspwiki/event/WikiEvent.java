package com.ecyrd.jspwiki.event;

import java.util.EventObject;

/**
 * Abstract parent class for wiki events.
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-02-23 20:51:31 $
 * @see com.ecyrd.jspwiki.auth.WikiSecurityEvent
 * @since 2.3.79
 */
public abstract class WikiEvent extends EventObject
{

    /**
     * Constructs an instance of this event.
     * @param source the Object that is the source of the event.
     */
    public WikiEvent( Object source )
    {
        super( source );
    }

}
