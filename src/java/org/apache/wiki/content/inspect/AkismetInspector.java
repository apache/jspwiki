package org.apache.wiki.content.inspect;

import javax.servlet.http.HttpServletRequest;

import net.sf.akismet.Akismet;

import org.apache.commons.lang.time.StopWatch;
import org.apache.wiki.WikiContext;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.util.TextUtil;

/**
 * {@link Inspector} implementation that consults an external spam engine
 * (Akismet).
 */
public class AkismetInspector implements Inspector
{
    private static Logger log = LoggerFactory.getLogger( AkismetInspector.class );

    private String m_akismetAPIKey = null;

    private Akismet m_akismet;

    /**
     * The filter property name for specifying the Akismet API-key. Value is
     * <tt>{@value}</tt>.
     */
    public static final String PROP_AKISMET_API_KEY = "akismet-apikey";

    public void initialize( InspectionPlan config )
    {
        m_akismetAPIKey = TextUtil.getStringProperty( config.getProperties(), PROP_AKISMET_API_KEY, m_akismetAPIKey );
    }
    
    /**
     * Always returns {@link Scope#REQUEST}.
     */
    public Scope getScope()
    {
        return Scope.REQUEST;
    }

    /**
     * Returns {@link Finding.Result#FAILED} if Akismet determines the change is
     * spam; {@code null} otherwise.
     * @param inspection the current Inspection
     * @param change the current contents, plus content that represents the added or
     *            deleted text since the last change
     * @return {@link Finding.Result#FAILED} if the test fails; {@code null} otherwise
     */
    public Finding[] inspect( Inspection inspection, Change change )
    {
        WikiContext context = inspection.getContext();
        HttpServletRequest req = context.getHttpRequest();
        if( m_akismetAPIKey == null || req == null )
        {
            return null;
        }

        if( m_akismet == null )
        {
            log.info( "Initializing Akismet spam protection." );

            m_akismet = new Akismet( m_akismetAPIKey, context.getEngine().getBaseURL() );

            if( !m_akismet.verifyAPIKey() )
            {
                log.error( "Akismet API key cannot be verified.  Please check your config." );
                m_akismetAPIKey = null;
                m_akismet = null;
                return null;
            }
        }

        // Akismet will mark all empty statements as spam, so we'll just
        // ignore them.
        if( change.getAdds() == 0 && change.getRemovals() > 0 )
        {
            return null;
        }

        log.debug( "Calling Akismet to check for spam..." );

        StopWatch sw = new StopWatch();
        sw.start();

        String ipAddress = req.getRemoteAddr();
        String userAgent = req.getHeader( "User-Agent" );
        String referrer = req.getHeader( "Referer" );
        String permalink = context.getViewURL( context.getPage().getName() );
        String commentType = context.getRequestContext().equals( WikiContext.COMMENT ) ? "comment" : "edit";
        String commentAuthor = context.getCurrentUser().getName();
        String commentAuthorEmail = null;
        String commentAuthorURL = null;

        boolean isSpam = m_akismet.commentCheck( ipAddress, userAgent, referrer, permalink, commentType, commentAuthor,
                                                 commentAuthorEmail, commentAuthorURL, change.toString(), null );

        sw.stop();

        log.debug( "Akismet request done in: " + sw );
        if( isSpam )
        {
            log.info( "Akismet thinks change is spam; added host " + ipAddress + " to temporary ban list. Change: "
                      + change.toString() );
            return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED, "You look like a spammer to me. (Incident code "
                                                                                   + inspection.getUid() + ")" ) };
        }
        return null;
    }
}
