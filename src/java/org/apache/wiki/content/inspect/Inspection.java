package org.apache.wiki.content.inspect;

import java.util.*;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.inspect.Inspector.Scope;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.util.TextUtil;

/**
 * Inspects content by iterating through a supplied set of {@link Inspector}
 * objects. The current {@link WikiContext} and {@link InspectionPlan} is
 * supplied during construction to allow each Inspector to access the HTTP
 * request, ReputationManager and other objects. This class is designed to be
 * used quickly within a single request. It is <em>not</em> thread-safe.
 */
public class Inspection
{
    private static final Logger log = LoggerFactory.getLogger( Inspection.class );

    private static final Logger c_spamlog = LoggerFactory.getLogger( "SpamLog" );

    /**
     * Returns a random string of six uppercase characters.
     * 
     * @return A random string
     */
    private static String getUniqueID()
    {
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();

        for( int i = 0; i < 6; i++ )
        {
            char x = (char) ('A' + rand.nextInt( 26 ));

            sb.append( x );
        }

        return sb.toString();
    }

    private final Map<Topic, Float> m_scores = new HashMap<Topic, Float>();

    private final Map<Topic, List<Finding>> m_findings = new HashMap<Topic, List<Finding>>();

    private final InspectionPlan m_plan;

    private final WikiContext m_context;

    private final String m_uid;

    /**
     * Constructs a new Inspection for a given WikiContext and InspectionPlan.
     * The InspectionPlan should have previously been initialized.
     * 
     * @param context the WikiContext
     * @param plan the InspectionPlan containing the Inspector objects used for
     *            the inspection
     */
    public Inspection( WikiContext context, InspectionPlan plan )
    {
        if( context == null || plan == null )
        {
            throw new IllegalArgumentException( "WikiContext and InspectionPlan must be supplied." );
        }
        m_context = context;
        m_plan = plan;
        m_uid = getUniqueID();
    }

    /**
     * Returns the WikiContext for the Inspection.
     * 
     * @return the WikiContext
     */
    public WikiContext getContext()
    {
        return m_context;
    }

    /**
     * Returns the Findings for a given Topic. If none of the Inspector objects
     * returned a Finding for {@code topic}, a zero-length array will be
     * returned.
     * 
     * @param topic the Topic to look up
     * @return the Findings
     */
    public Finding[] getFindings( Topic topic )
    {
        List<Finding> scores = m_findings.get( topic );
        if( scores == null )
        {
            return new Finding[0];
        }
        return scores.toArray( new Finding[scores.size()] );
    }

    /**
     * Returns the InspectionPlan for the Inspection.
     * 
     * @return the InspectionPlan
     */
    public InspectionPlan getPlan()
    {
        return m_plan;
    }

    /**
     * Returns the score for a given Topic. If none of the Inspector objects
     * returned a Finding for {@code topic}, zero ({@code 0f} will be returned.
     * The score is calculated by adding the weighted sum of
     * {@link Finding.Result#PASSED} results returned by all Inspectors, and
     * subtracting the sum of {@link Finding.Result#FAILED}.
     * 
     * @param topic the Topic to look up
     * @return the score
     */
    public float getScore( Topic topic )
    {
        if( m_scores.containsKey( topic ) )
        {
            Float score = m_scores.get( topic );
            return score.floatValue();
        }
        return 0;
    }

    /**
     * Returns the unique ID for Inspection. Each Inspection is created with a
     * random unique identifier assigned to it.
     * 
     * @return the unique ID
     */
    public String getUid()
    {
        return m_uid;
    }

    /**
     * Executes the chain of Inspector objects for a supplied number of
     * {@link Change} objects. All scores are initially reset to zero when this
     * method executes. The Inspectors with {@link Scope#REQUEST} execute first,
     * in order. Then the Inspectors with {@link Scope#FIELD} execute, in order,
     * once per Target.
     * 
     * @param changes one or more field changes; that is, a field-name,
     *            content and content-change tuple. If {@code null}, this method will
     *            execute only the Inspectors that have scope of
     *            {@link Scope#REQUEST}.
     */
    public void inspect( Change... changes )
    {
        m_scores.clear();
        m_findings.clear();
        Inspector[] inspectors = m_plan.getInspectors();

        // Execute the request-scoped inspectors
        for( Inspector inspector : inspectors )
        {
            if( inspector.getScope() == Scope.REQUEST )
            {
                Finding[] findings = inspector.inspect( this, null );
                processFindings( inspector, findings );
            }
        }

        if( changes == null )
        {
            return;
        }

        // Execute the field-scoped inspectors
        for( Change change : changes )
        {
            for( Inspector inspector : inspectors )
            {
                if( inspector.getScope() == Scope.FIELD )
                {
                    Finding[] findings = inspector.inspect( this, change );
                    processFindings( inspector, findings );
                }
            }
        }

        // Add the changes to the ReputationManager's list of recent changes
        ReputationManager mgr = m_plan.getReputationManager();
        for( Change change : changes )
        {
            mgr.addModifier( m_context.getHttpRequest(), change );
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        s.append( "Inspection[inspectors=" );
        s.append( m_plan.getInspectors().length );
        s.append( ",scores=" );
        for( Map.Entry<Topic, Float> score : m_scores.entrySet() )
        {
            s.append( score.getKey().toString() );
            s.append( '=' );
            s.append( score.getValue() );
            s.append( ' ' );
        }
        return s.toString();
    }

    /**
     * Processes any Findings that result from an Inspector's processing
     * @param inspector the inspector that executed
     * @param findings the findings it produced
     */
    private void processFindings( Inspector inspector, Finding[] findings )
    {
        float weight = m_plan.getWeight( inspector );
        if( findings != null )
        {
            for( Finding finding : findings )
            {
                // Increase/decrease score here
                Topic topic = finding.getTopic();
                updateScore( topic, finding, weight );
                log( inspector, finding, weight );
            }
        }
    }

    /**
     * Logs a Finding to the inspection log.
     * 
     * @param finding the Finding to log
     * @param weight the weight for the Inspector that generated the Finding
     */
    private void log( Inspector inspector, Finding finding, float weight )
    {
        String logMessage = finding.getMessage();
        log.info( logMessage );
        logMessage = TextUtil.replaceString( logMessage, "\r\n", "\\r\\n" );
        logMessage = TextUtil.replaceString( logMessage, "\"", "\\\"" );

        WikiPage page = m_context.getPage();
        String pageName = page == null ? "(no page)" : page.getName();
        String reason = "UNKNOWN";
        String addr = m_context.getHttpRequest() != null ? m_context.getHttpRequest().getRemoteAddr() : "-";
        String source = inspector.getClass().getName();

        Finding.Result result = finding.getResult();
        switch( result )
        {
            case PASSED: {
                reason = "PASSED" + "+" + weight;
                break;
            }
            case FAILED: {
                reason = "FAILED" + "-" + weight;
                break;
            }
            default: {
                reason = "" + " ";
            }
        }

        c_spamlog.info( m_uid + " " + reason + " " + source + " " + addr + " \"" + pageName + "\" " + logMessage );
    }

    /**
     * Updates the score for a topic.
     * 
     * @param finding the score to update
     */
    private void updateScore( Topic topic, Finding finding, float weight )
    {
        // Updates findings Map
        List<Finding> findings = m_findings.get( topic );
        if( findings == null )
        {
            findings = new ArrayList<Finding>();
            m_findings.put( topic, findings );
        }
        findings.add( finding );

        // Update score
        Float score = m_scores.get( topic );
        if( score == null )
        {
            score = 0f;
        }
        switch( finding.getResult() )
        {
            case PASSED: {
                score = Float.valueOf( score + weight );
                break;
            }
            case FAILED: {
                score = Float.valueOf( score - weight );
                break;
            }
        }
        m_scores.put( topic, score );
    }
}
