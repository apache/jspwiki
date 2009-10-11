package org.apache.wiki.content.inspect;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.util.TextUtil;

/**
 * {@link Inspector} implementation that determines if the current IP address
 * has made too many changes in a specified time period.
 */
public class ChangeRateInspector implements Inspector
{
    /**
     * The filter property name for specifying how many changes is any given IP
     * address allowed to do per minute. Value is <tt>{@value}</tt>.
     */
    public static final String PROP_PAGECHANGES = "spam.limit.pageChanges";

    /**
     * The filter property name for specifying how many similar changes are
     * allowed before a host is banned. Value is <tt>{@value}</tt>.
     */
    public static final String PROP_SIMILARCHANGES = "spam.limit.similarChanges";

    private static Logger log = LoggerFactory.getLogger( ChangeRateInspector.class );

    /**
     * How many times a single IP address can change a page per minute?
     */
    private int m_limitSinglePageChanges = 5;

    /**
     * How many times can you add the exact same string to a page?
     */
    private int m_limitSimilarChanges = 2;

    public void initialize( InspectionPlan config )
    {
        Properties props = config.getProperties();
        m_limitSinglePageChanges = TextUtil.getIntegerProperty( props, PROP_PAGECHANGES, m_limitSinglePageChanges );

        m_limitSimilarChanges = TextUtil.getIntegerProperty( props, PROP_SIMILARCHANGES, m_limitSimilarChanges );

        log.info( "# Spam filter initialized.  Temporary ban time " + config.getReputationManager().getBanTime()
                  + " mins, max page changes/m inute: " + m_limitSinglePageChanges );
    }

    /**
     * Returns {@link Finding.Result#FAILED} if the user has recently submitted too many
     * aggregate or identical changes; {@code null} otherwise.
     * @param inspection the current Inspection
     * @param content the content that is being inspected
     * @param change the subset of the content that represents the added or
     *            deleted text since the last change
     * @return {@link Finding.Result#FAILED} if the test fails; {@code null} otherwise
     */
    public Finding[] inspect( Inspection inspection, String content, Change change )
    {
        HttpServletRequest req = inspection.getContext().getHttpRequest();
        if( req == null )
        {
            return null;
        }

        String addr = req.getRemoteAddr();
        int hostCounter = 0;
        int changeCounter = 0;

        log.debug( "Change is " + change.getChange() );

        for( ReputationManager.Host host : inspection.getPlan().getReputationManager().getModifiers() )
        {
            // Has this IP address has been seen lately?
            if( host.getAddress().equals( addr ) )
            {
                hostCounter++;
            }

            // Has this change been seen before?
            if( host.getChange() != null && host.getChange().equals( change ) )
            {
                changeCounter++;
            }
        }

        // Now, let's check against the limits.
        if( hostCounter >= m_limitSinglePageChanges )
        {
            inspection.getPlan().getReputationManager().banHost( req );
            log
                .info( "Too many modifications/minute. Added host " + addr + " to temporary ban list. Change: "
                       + change.getChange() );
            return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED, "You look like a spammer to me. (Incident code "
                                                                                   + inspection.getUid() + ")" ) };
        }

        if( changeCounter >= m_limitSimilarChanges )
        {
            inspection.getPlan().getReputationManager().banHost( req );
            log.info( "Too many similar modifications. Added host " + addr + " to temporary ban list. Change: "
                      + change.getChange() );
            return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED, "You look like a spammer to me. (Incident code "
                                                                                   + inspection.getUid() + ")" ) };
        }
        return null;
    }

}
