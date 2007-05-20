package com.ecyrd.jspwiki.util;

/**
 *  A watchdog needs something to watch.  If you wish to be watched,
 *  implement this interface.
 *
 *  @author jalkanen
 */
public interface Watchable
{
    /**
     *  This is a callback which is called whenever your expected
     *  completion time is exceeded.  The current state of the
     *  stack is available.
     *
     *  @param state The state in which your Watchable is currently.
     */
    public void timeoutExceeded( String state );

    /**
     *  Returns a human-readable name of this Watchable.  Used in
     *  logging.
     *
     *  @return The name of the Watchable.
     */
    public String getName();

    /**
     *  Returns <code>true</code>, if this Watchable is still alive and can be
     *  watched; otherwise <code>false</code>. For example, a stopped Thread
     *  is not very interesting to watch.
     *
     *  @return the result
     */
    public boolean isAlive();
}
