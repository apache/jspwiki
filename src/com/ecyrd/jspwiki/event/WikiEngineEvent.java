package com.ecyrd.jspwiki.event;

/**
 * Event class for wiki engine events.
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-06-17 23:13:35 $
 * @since 2.4.14
 */
public class WikiEngineEvent extends WikiEvent
{
    private static final long serialVersionUID = 1829433967558773970L;

    public static final int   SHUTDOWN         = 1;

    /**
     * Constructs a wiki engine event of a specified type.
     * @param source the wiki engine that produced the event
     * @param type the type of the event
     */
    public WikiEngineEvent( Object source, int type )
    {
        super( source, type );
    }

    /**
     * Prints a String (human-readable) representation of this object.
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        StringBuffer msg = new StringBuffer();
        msg.append( "WikiSecurityEvent." );
        msg.append( eventName( getType() ) );
        msg.append( " [source=" );
        msg.append( getSource().toString() );
        msg.append( "]" );
        return msg.toString();
    }

    /**
     * Returns a textual representation of an event type.
     * @param type the type
     * @return the string representation
     */
    protected static final String eventName( int type )
    {
        switch( type )
        {
        case SHUTDOWN:
        {
            return "SHUTDOWN";
        }
        }
        return "UNKNOWN";
    }
}
