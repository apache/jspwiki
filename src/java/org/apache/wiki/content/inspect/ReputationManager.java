package org.apache.wiki.content.inspect;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;

/**
 * Stores a list of hosts that are temporarily banned.
 */
public class ReputationManager
{
    /**
     * A local class for storing host information.
     * 
     * @since
     */
    static class Host
    {
        private final long m_addedTime = System.currentTimeMillis();

        private final long m_releaseTime;

        private final String m_address;

        private final String m_change;

        /**
         * @param ipaddress
         * @param change
         * @param releaseTime the time until release, in seconds
         */
        public Host( String ipaddress, Change change, int releaseTime )
        {
            m_address = ipaddress;
            m_change = change == null ? null : change.getChange();
            m_releaseTime = System.currentTimeMillis() + releaseTime * 60 * 1000L;
        }

        public long getAddedTime()
        {
            return m_addedTime;
        }

        public String getAddress()
        {
            return m_address;
        }

        public String getChange()
        {
            return m_change;
        }

        public long getReleaseTime()
        {
            return m_releaseTime;
        }
        
        /**
         * {@inheritDoc}
         */
        public String toString()
        {
            return "[" + m_address + " " + new Date( m_addedTime ).toString()  + " " + m_change.toString() + "]";
        }
    }

    private final List<ReputationManager.Host> m_modifiers = new ArrayList<ReputationManager.Host>();

    private final int m_banTime;

    private Map<String, ReputationManager.Host> m_temporaryBanList = new HashMap<String, ReputationManager.Host>();

    private static Logger log = LoggerFactory.getLogger( ReputationManager.class );

    public static final long NOT_BANNED = 0;

    /**
     * Constructs a new ReputationManager with a defined expiration time.
     * 
     * @param minutes
     */
    public ReputationManager( int minutes )
    {
        m_banTime = minutes;
    }

    public synchronized Host addModifier( HttpServletRequest request, Change change )
    {
        String addr = request.getRemoteAddr();
        Host host = new ReputationManager.Host( addr, change, 0 );
        m_modifiers.add( host );
        return host;
    }

    public Host banHost( HttpServletRequest request )
    {
        String ipAddress = request.getRemoteAddr();
        ReputationManager.Host host = new ReputationManager.Host( ipAddress, null, m_banTime );
        m_temporaryBanList.put( ipAddress, host );
        return host;
    }

    /**
     * The time until a ban expires, in minutes.
     * 
     * @return the minutes until the ban expires
     */
    public int getBanTime()
    {
        return m_banTime;
    }

    /**
     * Returns a list of hosts that have modified wiki pages in the last minute.
     * 
     * @return the vector
     */
    public synchronized ReputationManager.Host[] getModifiers()
    {
        Iterator<Host> iterator = m_modifiers.iterator();
        long time = System.currentTimeMillis() - 60 * 1000L; // 1 minute
        while ( iterator.hasNext() )
        {
            // Check if this item is invalid
            Host modifier = iterator.next();
            if( modifier.getAddedTime() < time )
            {
                log.debug( "Removed host " + modifier.getAddress() + " from modification queue (expired)" );
                iterator.remove();
                continue;
            }
        }
        return m_modifiers.toArray( new ReputationManager.Host[m_modifiers.size()] );
    }

    /**
     * Returns the number of seconds until the ban expires for a particular
     * host. If the host is not banned, returns
     * {@link ReputationManager#NOT_BANNED}.
     * 
     * @param ipAddress the IP address to check
     * @return the number of seconds until the ban expires, or
     *         {@link ReputationManager#NOT_BANNED} if no ban
     */
    public long getRemainingBan( String ipAddress )
    {
        long now = System.currentTimeMillis();
        if( m_temporaryBanList.containsKey( ipAddress ) )
        {
            Host host = m_temporaryBanList.get( ipAddress );
            if( host.getReleaseTime() < now )
            {
                log.debug( "Removed host " + host.getAddress() + " from temporary ban list (expired)" );
                m_temporaryBanList.remove( ipAddress );
                return NOT_BANNED;
            }
            long timeleft = (host.getReleaseTime() - now) / 1000L;
            return timeleft;
        }
        return NOT_BANNED;
    }

    /**
     * Removes a Host from the ban list.
     * @param request the HTTP request containing the IP address to un-ban.
     */
    public Host unbanHost( HttpServletRequest request )
    {
        String ipAddress = request.getRemoteAddr();
        if ( m_temporaryBanList.containsKey( ipAddress ) )
        {
            return m_temporaryBanList.remove( ipAddress );
        }
        return null;
    }

    // TODO: need to restore cleaning of ban-list, preferably via timed MBean or
    // background thread
    // Every request cycle was a bit excessive, though...
}
