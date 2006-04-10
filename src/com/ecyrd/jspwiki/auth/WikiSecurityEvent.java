package com.ecyrd.jspwiki.auth;

import java.security.Principal;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.ecyrd.jspwiki.event.WikiEvent;

/**
 * <p>Event class for security events: login/logout, wiki group adds/changes, and
 * authorization decisions. When a WikiSecurityEvent is constructed, the
 * security logger {@link #LOGGER} is notified.</p>
 * <p>These events are logged with priority <code>ERROR</code>:</p>
 * <ul>
 *   <li>login failed - bad credential or password</li>
 * </ul>
 * <p>These events are logged with priority <code>WARN</code>:</p>
 * <ul>
 *   <li>access denied</li>
 *   <li>login failed - credential expired</li>
 *   <li>login failed - account expired</li>
 * </ul>
 * <p>These events are logged with priority <code>INFO</code>:</p>
 * <ul>
 *   <li>login succeeded</li>
 *   <li>logout</li>
 * </ul>
 * <p>These events are logged with priority <code>DEBUG</code>:</p>
 * <ul>
 *   <li>access allowed</li>
 *   <li>add group</li>
 *   <li>remove group</li>
 *   <li>clear all groups</li>
 *   <li>add group member</li>
 *   <li>remove group member</li>
 *   <li>clear all members from group</li>
 * </ul>
 * @author Andrew Jaquith
 * @version $Revision: 1.5 $ $Date: 2006-04-10 20:42:57 $
 * @since 2.3.79
 */
public final class WikiSecurityEvent extends WikiEvent
{

    private static final long serialVersionUID    = -6751950399721334496L;

    /** When a user authenticates with a username and password, or via container auth. */
    public static final int   LOGIN_AUTHENTICATED = 1;

    /** When a login fails due to account expiration. */
    public static final int   LOGIN_ACCOUNT_EXPIRED = 2;
    
    /** When a login fails due to credential expiration. */
    public static final int   LOGIN_CREDENTIAL_EXPIRED = 3;
    
    /** When a login fails due to wrong username or password. */
    public static final int   LOGIN_FAILED = 4;
    
    /** When a user logs out. */
    public static final int   LOGOUT = 5;

    /** When a new wiki group is added. */
    public static final int   GROUP_ADD = 11;

    /** When a wiki group is deleted. */
    public static final int   GROUP_REMOVE = 12;

    /** When all wiki groups are removed from GroupManager. */
    public static final int   GROUP_CLEAR_GROUPS = 13;

    /** When a new member is added to a wiki group. */
    public static final int   GROUP_ADD_MEMBER = 14;

    /** When a member is removed from a wiki group. */
    public static final int   GROUP_REMOVE_MEMBER = 15;

    /** When all members are cleared from a wiki group. */
    public static final int   GROUP_CLEAR_MEMBERS = 16;

    /** When access to a resource is allowed. */
    public static final int   ACCESS_ALLOWED = 20;
    
    /** When access to a resource is allowed. */
    public static final int   ACCESS_DENIED = 21;
    
    /** The security logging service. */
    public static final Logger LOGGER = Logger.getLogger( "SecurityLog" );
    
    private final Principal m_principal;
    
    private final Object      m_target;

    private final int         m_type;
    
    private static final int[] ERROR_EVENTS = { LOGIN_FAILED };
    
    private static final int[] WARN_EVENTS  = { LOGIN_ACCOUNT_EXPIRED,
                                                LOGIN_CREDENTIAL_EXPIRED };
    
    private static final int[] INFO_EVENTS  = { LOGIN_AUTHENTICATED,
                                                ACCESS_DENIED,
                                                LOGOUT };
    
    /**
     * Constructs a new instance of this event type, which signals a security
     * event has occurred. The <code>source</code> parameter is required, and
     * may not be <code>null</code>. When the WikiSecurityEvent is
     * constructed, the security logger {@link #LOGGER} is notified.
     * @param source the source of the event, which can be any object: a wiki
     *            page, group or authentication/authentication/group manager.
     * @param type the type of event
     * @param principal the subject of the event, which may be <code>null</code>
     * @param target the changed Object, which may be <code>null</code>
     */
    public WikiSecurityEvent( Object source, int type, Principal principal, Object target )
    {
        super( source );
        if ( source == null )
        {
            throw new IllegalArgumentException( "Argument(s) cannot be null." );
        }
        this.m_type = type;
        this.m_principal = principal;
        this.m_target = target;
        if ( LOGGER.isEnabledFor( Priority.ERROR ) && ArrayUtils.contains( ERROR_EVENTS, type ) )
        {
            LOGGER.error( this );
        }
        else if ( LOGGER.isEnabledFor( Priority.WARN ) && ArrayUtils.contains( WARN_EVENTS, type ) )
        {
            LOGGER.warn( this );
        }
        else if ( LOGGER.isEnabledFor( Priority.INFO ) && ArrayUtils.contains( INFO_EVENTS, type ) )
        {
            LOGGER.info( this );
        }
        LOGGER.debug( this );
    }

    /**
     * Constructs a new instance of this event type, which signals a security
     * event has occurred. The <code>source</code> parameter is required, and
     * may not be <code>null</code>. When the WikiSecurityEvent is
     * constructed, the security logger {@link #LOGGER} is notified.
     * @param source the source of the event, which can be any object: a wiki
     *            page, group or authentication/authentication/group manager.
     * @param type the type of event
     * @param target the changed Object, which may be <code>null</code>.
     */
    public WikiSecurityEvent( Object source, int type, Object target )
    {
        this( source, type, null, target );
    }

    /**
     * Returns the principal to whom the opeation applied, if supplied. This
     * method may return <code>null</code>
     * <em>&#8212; and calling methods should check for this condition</em>.
     * @return the changed object
     */
    public final Object getPrincipal()
    {
        return m_principal;
    }
    
    /**
     * Returns the object that was operated on, if supplied. This method may
     * return <code>null</code>
     * <em>&#8212; and calling methods should check for this condition</em>.
     * @return the changed object
     */
    public final Object getTarget()
    {
        return m_target;
    }

    /**
     * Returns the type of event.
     * @return the event type
     */
    public final int getType()
    {
        return m_type;
    }

    /**
     * Prints a String (human-readable) representation of this object.
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        StringBuffer msg = new StringBuffer();
        msg.append( "WikiSecurityEvent." );
        msg.append(  eventName( m_type ) );
        msg.append( " [source=" + getSource().toString() );
        msg.append( ", princpal=" + m_principal );
        msg.append( ", target=" + m_target );
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
        case ACCESS_ALLOWED:
        {
            return "ACCESS_ALLOWED";
        }
        case ACCESS_DENIED:
        {
            return "ACCESS_DENIED";
        }
        case GROUP_ADD:
        {
            return "GROUP_ADD";
        }
        case GROUP_ADD_MEMBER:
        {
            return "GROUP_ADD_MEMBER";
        }
        case GROUP_CLEAR_GROUPS:
        {
            return "GROUP_CLEAR_GROUPS";
        }
        case GROUP_CLEAR_MEMBERS:
        {
            return "GROUP_CLEAR_MEMBERS";
        }
        case GROUP_REMOVE:
        {
            return "GROUP_REMOVE";
        }
        case GROUP_REMOVE_MEMBER:
        {
            return "GROUP_REMOVE_MEMBER";
        }
        case LOGIN_ACCOUNT_EXPIRED:
        {
            return "LOGIN_ACCOUNT_EXPIRED";
        }
        case LOGIN_AUTHENTICATED:
        {
            return "LOGIN_AUTHENTICATED";
        }
        case LOGIN_CREDENTIAL_EXPIRED:
        {
            return "LOGIN_ACCOUNT_EXPIRED";
        }
        case LOGIN_FAILED:
        {
            return "LOGIN_FAILED";
        }
        case LOGOUT:
        {
            return "LOGOUT";
        }
        }
        return "UNKNOWN";
    }
    
}
