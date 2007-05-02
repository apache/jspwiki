package com.ecyrd.jspwiki.event;

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
 *   <li>user profile name changed</li>
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
 * @since 2.3.79
 */
public final class WikiSecurityEvent extends WikiEvent
{

    private static final long serialVersionUID    = -6751950399721334496L;

    /** When a user's attempts to log in as guest, via cookies, using a password or otherwise. */
    public static final int   LOGIN_INITIATED          = 30;
    
    /** When a user first accesses JSPWiki, but before logging in or setting a cookie. */
    public static final int   LOGIN_ANONYMOUS          = 31;
    
    /** When a user sets a cookie to assert their identity. */
    public static final int   LOGIN_ASSERTED           = 32;
    
    /** When a user authenticates with a username and password, or via container auth. */
    public static final int   LOGIN_AUTHENTICATED      = 40;

    /** When a login fails due to account expiration. */
    public static final int   LOGIN_ACCOUNT_EXPIRED    = 41;
    
    /** When a login fails due to credential expiration. */
    public static final int   LOGIN_CREDENTIAL_EXPIRED = 42;
    
    /** When a login fails due to wrong username or password. */
    public static final int   LOGIN_FAILED             = 43;
    
    /** When a user logs out. */
    public static final int   LOGOUT                   = 44;

    /** When a session expires. */
    public static final int   SESSION_EXPIRED          = 45;

    /** When a new wiki group is added. */
    public static final int   GROUP_ADD                = 46;

    /** When a wiki group is deleted. */
    public static final int   GROUP_REMOVE             = 47;

    /** When all wiki groups are removed from GroupDatabase. */
    public static final int   GROUP_CLEAR_GROUPS       = 48;

    /** When access to a resource is allowed. */
    public static final int   ACCESS_ALLOWED           = 51;
    
    /** When access to a resource is allowed. */
    public static final int   ACCESS_DENIED            = 52;
    
    /** When a user profile is saved. */
    public static final int   PROFILE_SAVE             = 53;
    
    /** When a user profile name changes. */
    public static final int   PROFILE_NAME_CHANGED     = 54;
    
    /** The security logging service. */
    public static final Logger LOGGER = Logger.getLogger( "SecurityLog" );
    
    private final Principal m_principal;
    
    private final Object      m_target;

    private static final int[] ERROR_EVENTS = { LOGIN_FAILED };
    
    private static final int[] WARN_EVENTS  = { LOGIN_ACCOUNT_EXPIRED,
                                                LOGIN_CREDENTIAL_EXPIRED };
    
    private static final int[] INFO_EVENTS  = { LOGIN_AUTHENTICATED,
                                                SESSION_EXPIRED, LOGOUT, PROFILE_NAME_CHANGED };
    
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
        super( source, type );
        if ( source == null )
        {
            throw new IllegalArgumentException( "Argument(s) cannot be null." );
        }
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
     * Prints a String (human-readable) representation of this object.
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        StringBuffer msg = new StringBuffer();
        msg.append( "WikiSecurityEvent." );
        msg.append(  eventName( getType() ) );
        msg.append( " [source=" + getSource().toString() );
        if( m_principal != null )
        {
            msg.append( ", princpal=" + m_principal.getClass().getName() );
            msg.append( " " + m_principal.getName() );
        }
        msg.append( ", target=" + m_target );
        msg.append( "]" );
        return msg.toString();
    }
    
    /**
     * Returns a textual representation of an event type.
     * @param type the type
     * @return the string representation
     */
    public final String eventName( int type )
    {
        switch( type )
        {
            case LOGIN_AUTHENTICATED:       return "LOGIN_AUTHENTICATED";
            case LOGIN_ACCOUNT_EXPIRED:     return "LOGIN_ACCOUNT_EXPIRED";
            case LOGIN_CREDENTIAL_EXPIRED:  return "LOGIN_ACCOUNT_EXPIRED";
            case LOGIN_FAILED:              return "LOGIN_FAILED";
            case LOGOUT:                    return "LOGOUT";
            case SESSION_EXPIRED:           return "SESSION_EXPIRED";
            case GROUP_ADD:                 return "GROUP_ADD";
            case GROUP_REMOVE:              return "GROUP_REMOVE";
            case GROUP_CLEAR_GROUPS:        return "GROUP_CLEAR_GROUPS";
            case ACCESS_ALLOWED:            return "ACCESS_ALLOWED";
            case ACCESS_DENIED:             return "ACCESS_DENIED";
            case PROFILE_NAME_CHANGED:      return "PROFILE_NAME_CHANGED";
            case PROFILE_SAVE:              return "PROFILE_SAVE";
            default:                        return super.eventName();
        }
    }

    /**
     *  Returns a human-readable description of the event type.
     *
     * @return a String description of the type
     */
    public final String getTypeDescription()
    {
        switch ( getType() )
        {
            case LOGIN_AUTHENTICATED:       return "login authenticated";
            case LOGIN_ACCOUNT_EXPIRED:     return "login failed: expired account";
            case LOGIN_CREDENTIAL_EXPIRED:  return "login failed: credential expired";
            case LOGIN_FAILED:              return "login failed";
            case LOGOUT:                    return "user logged out";
            case SESSION_EXPIRED:           return "session expired";
            case GROUP_ADD:                 return "new group added";
            case GROUP_REMOVE:              return "group removed";
            case GROUP_CLEAR_GROUPS:        return "all groups cleared";
            case ACCESS_ALLOWED:            return "access allowed";
            case ACCESS_DENIED:             return "access denied";
            case PROFILE_NAME_CHANGED:      return "user profile name changed";
            case PROFILE_SAVE:              return "user profile saved";
            default:                        return super.getTypeDescription();
        }
    }

}
