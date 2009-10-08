package org.apache.wiki.content.inspect;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiContext;
import org.apache.wiki.content.inspect.ReputationManager.Host;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.util.TextUtil;

/**
 * {@link Inspector} implementation that determines if a change is spam by
 * checking to see if it contains too many links.
 */
public class LinkCountInspector implements Inspector
{
    private Pattern m_urlPattern;

    private static final String URL_REGEXP = "(http://|https://|mailto:)([A-Za-z0-9_/\\.\\+\\?\\#\\-\\@=&;]+)";

    private static Logger log = LoggerFactory.getLogger( LinkCountInspector.class );

    private InspectionPlan m_config;

    /**
     * The filter property name for specifying how many URLs can any given edit
     * contain. Value is <tt>{@value}</tt>
     */
    public static final String PROP_MAXURLS = "maxurls";

    /**
     * How many URLs can be added at maximum.
     */
    private int m_maxUrls = 10;

    public void initialize( InspectionPlan config )
    {
        m_config = config;
        try
        {
            m_urlPattern = Pattern.compile( URL_REGEXP );
        }
        catch( PatternSyntaxException e )
        {
            log.error( "Internal error: Someone put in a faulty pattern.", e );
            throw new InternalWikiException( "Faulty pattern." );
        }

        m_maxUrls = TextUtil.getIntegerProperty( config.getProperties(), PROP_MAXURLS, m_maxUrls );
    }

    public Finding[] inspect( Inspection inspection, String content, Change change )
    {
        // Calculate the number of links in the addition.
        String tstChange = change.toString();
        int urlCounter = 0;

        Matcher matcher = m_urlPattern.matcher( tstChange );
        while ( matcher.find() )
        {
            urlCounter++;
        }

        if( urlCounter > m_maxUrls )
        {
            WikiContext context = inspection.getContext();
            Host host = m_config.getReputationManager().banHost( context.getHttpRequest() );
            log.info( "Added host " + host.getAddress() + " to temporary ban list for adding too many URLs. Change: "
                      + change.toString() );
            return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED, "You look like a spammer to me. (Incident code "
                                                                                   + inspection.getUid() + ")" ) };
        }
        return new Finding[] { new Finding( Topic.SPAM, Finding.Result.PASSED, "Change does not have too many links." ) };
    }

}
