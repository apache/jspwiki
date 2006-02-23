package com.ecyrd.jspwiki.auth;

import com.ecyrd.jspwiki.event.WikiEvent;

/**
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2006-02-23 20:51:31 $
 * @since 2.3.79
 */
public final class WikiSecurityEvent extends WikiEvent
{

    private static final long serialVersionUID    = -6751950399721334496L;

    /**
     * When a user authenticates with a username and password, or via container
     * auth.
     */
    public static final int   LOGIN_AUTHENTICATED = 1;

    /** When a user logs out */
    public static final int   LOGOUT              = 2;

    /** When a new wiki group is added. */
    public static final int   GROUP_ADD           = 13;

    /** When a wiki group is deleted. */
    public static final int   GROUP_REMOVE        = 14;

    public static final int   GROUP_CLEAR_GROUPS  = 15;

    /** When a new member is added to a wiki group */
    public static final int   GROUP_ADD_MEMBER    = 16;

    /** When a member is removed from a wiki group */
    public static final int   GROUP_REMOVE_MEMBER = 17;

    /** When a all members are cleared from a wiki group */
    public static final int   GROUP_CLEAR_MEMBERS = 18;

    private final Object      target;

    private final int         type;

    /**
     * Constructs a new instance of this event type, which signals a security
     * event has occurred. The <code>source</code> and <code>type</code>
     * parameters are required, and may not be <code>null</code>.
     * @param source the source of the event, which can be any object: a wiki
     *            page, group or authentication/authentication/group manager.
     * @param type the type of event
     * @param target the changed Object, which may be <code>null</code>.
     */
    public WikiSecurityEvent( Object source, int type, Object target )
    {
        super( source );
        if ( source == null )
        {
            throw new IllegalArgumentException( "Argument(s) cannot be null." );
        }
        this.type = type;
        this.target = target;
    }

    /**
     * Returns the object that was operated on, if supplied. This method may
     * return <code>null</code>,
     * <em>-- and calling methods should check for this condition</em>.
     * @return the changed object
     */
    public final Object getTarget()
    {
        return target;
    }

    /**
     * Returns the type of event.
     * @return the event type
     */
    public final int getType()
    {
        return type;
    }

    /**
     * Prints a String (human-readable) representation of this object.
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        StringBuffer msg = new StringBuffer();
        msg.append( "WikiSecurityEvent." );
        switch( type )
        {
        case GROUP_ADD:
        {
            msg.append( "GROUP_ADD" );
            break;
        }
        case GROUP_REMOVE:
        {
            msg.append( "GROUP_REMOVE" );
            break;
        }
        case GROUP_ADD_MEMBER:
        {
            msg.append( "GROUP_ADD_MEMBER" );
            break;
        }
        case GROUP_CLEAR_MEMBERS:
        {
            msg.append( "GROUP_CLEAR_MEMBERS" );
            break;
        }
        case GROUP_REMOVE_MEMBER:
        {
            msg.append( "GROUP_REMOVE_MEMBER" );
            break;
        }
        case LOGIN_AUTHENTICATED:
        {
            msg.append( "LOGIN_AUTHENTICATED" );
            break;
        }
        case LOGOUT:
        {
            msg.append( "LOGOUT" );
            break;
        }
        }
        msg.append( " [source=" + getSource().toString() );
        msg.append( ",target=" + target );
        msg.append( "]" );
        return msg.toString();
    }

}
