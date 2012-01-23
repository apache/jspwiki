package org.apache.wiki.content.inspect;

import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;

/**
 * Spam-oriented {@link InspectionPlan} that includes a static factory method
 * for retrieving the SpamInspectionPlan for a supplied WikiEngine.
 */
public final class SpamInspectionPlan extends InspectionPlan
{
    private static final Logger log = LoggerFactory.getLogger( SpamInspectionPlan.class );

    protected static final String PROP_INSPECTOR_WEIGHT_PREFIX = "inspectorWeight.spam.";

    /**
     * The filter property name for specifying the threshold for scoring above
     * which a change should be considered spam.
     */
    public static final String PROP_SCORE_LIMIT = "spamScoreLimit";

    /** Default limit at which we consider something to be spam. */
    protected static final float DEFAULT_SCORE_LIMIT = -0.01f;

    protected static final String DEFAULT_CAPTCHA_CLASS = AsirraCaptcha.class.getName();

    /**
     * Default weight for Inspectors.
     */
    protected static final float DEFAULT_WEIGHT = 0f;

    protected static final String PROP_CAPTCHA_CLASS = "jspwiki.captcha.implementation";

    private static final Map<WikiEngine, SpamInspectionPlan> c_plans = new WeakHashMap<WikiEngine, SpamInspectionPlan>();
    
    /**
     * <p>
     * Looks up and returns the spam InspectionPlan for a given WikiEngine. If
     * the InspectionPlan does not exist, it will be created. The InpsectionPlan
     * will add the following Inspectors, in order:
     * </p>
     * <ul>
     * <li>{@link UserInspector}</li>
     * <li>{@link BanListInspector}</li>
     * <li>{@link ChangeRateInspector}</li>
     * <li>{@link LinkCountInspector}</li>
     * <li>{@link BotTrapInspector}</li>
     * <li>{@link AkismetInspector}</li>
     * <li>{@link PatternInspector}</li>
     * </ul>
     * <p>
     * The weights for each Inspector will be determined by examining {@code
     * props}. The property name that indicates the weight to be used is of the
     * form
     * <code>{@value #PROP_INSPECTOR_WEIGHT_PREFIX}<var>fullyQualifiedClassname</var></code>.
     * 
     * @param engine the wiki engine
     * @return the InspectionPlan
     */
    public static SpamInspectionPlan getInspectionPlan( WikiEngine engine ) throws WikiException
    {
        SpamInspectionPlan plan = c_plans.get( engine );
        if( plan != null )
        {
            return plan;
        }

        // Create new InspectionPlan for this WikiEngine
        Properties props = engine.getWikiProperties();
        plan = new SpamInspectionPlan( props );
        plan.addInspector( new UserInspector(), plan.getWeight( props, UserInspector.class ) );
        plan.addInspector( new BanListInspector(), plan.getWeight( props, BanListInspector.class ) );
        plan.addInspector( new ChangeRateInspector(), plan.getWeight( props, ChangeRateInspector.class ) );
        plan.addInspector( new LinkCountInspector(), plan.getWeight( props, LinkCountInspector.class ) );
        plan.addInspector( new BotTrapInspector(), plan.getWeight( props, BotTrapInspector.class ) );
        plan.addInspector( new AkismetInspector(), plan.getWeight( props, AkismetInspector.class ) );
        plan.addInspector( new PatternInspector(), plan.getWeight( props, PatternInspector.class ) );
        c_plans.put( engine, plan );

        // Figure out the Challenge to use for this WikiEngine
        return plan;
    }

    /**
     * Returns a new instance of the configured CAPTCHA implementation class,
     * if one was specified by the {@link #PROP_CAPTCHA_CLASS} property. If one
     * was not specified, this method will attempt to initialize the class
     * named by {@link #DEFAULT_CAPTCHA_CLASS}. This method is guaranteed to
     * return a non-{@code null} value.
     * 
     * @param props the properties to examine
     * @return an initialized CAPTCHA implementing class
     * @throws WikiException if the CAPTCHA implementation cannot be found or
     * initialized for any reason; the original Exception will be wrapped
     */
    protected static Captcha initCaptcha( Properties props ) throws WikiException
    {
        String captchaClassName = props.getProperty( PROP_CAPTCHA_CLASS, DEFAULT_CAPTCHA_CLASS );
        Class<?> captchaClass = null;
        try
        {
            captchaClass = Class.forName( captchaClassName );
        }
        catch( ClassNotFoundException e )
        {
            String msg = "No CAPTCHA implementation found for class " + captchaClassName;
            log.error( msg, e );
            throw new WikiException( msg, e );
        }

        Captcha captcha = null;
        if( captchaClass != null )
        {
            try
            {
                captcha = (Captcha) captchaClass.newInstance();
            }
            catch( Exception e )
            {
                String msg = "Could not create CAPTCHA instance for class " + captchaClassName;
                log.error( msg, e );
                throw new WikiException( msg, e );
            }
        }
        return captcha;
    }
    
    private final Captcha m_captcha;
    
    private final float m_limit;

    /**
     * Private constructor to prevent direct instantiation.
     * @throws WikiException if the SpamInspectionPlan cannot be
     * initialized for any reason; the original Exception will be wrapped
     */
    private SpamInspectionPlan( Properties properties ) throws WikiException
    {
       super( properties ); 
       
       // Configure the CAPTCHA
       m_captcha = initCaptcha( properties );
       
       // Get the default spam score limits
       float limit = DEFAULT_SCORE_LIMIT;
       String limitString = properties.getProperty( PROP_SCORE_LIMIT, String.valueOf( DEFAULT_SCORE_LIMIT ) );
       try
       {
           limit = Float.parseFloat( limitString );
       }
       catch( NumberFormatException e )
       {
           log.error( "Property value " + PROP_SCORE_LIMIT + " did not parse to a float. Using " + DEFAULT_SCORE_LIMIT );
       }
       m_limit = limit;
    }

    /**
     * Returns the CAPTCHA ({@link Challenge}) for a the SpamInspectionPlan.
     * @return the CAPTCHA
     */
    public Captcha getCaptcha() throws WikiException
    {
        return m_captcha;
    }

    public float getSpamLimit()
    {
        return m_limit;
    }

    /**
     * Determines the desired weight for an Inspector class, based on looking up
     * the value in the WikiEngine Properties
     * @param props the properties used to initialize the WikiEngine
     * @param inspectorClass the Inspector class
     * @return if found, the desired weight; if not, returns {@link #DEFAULT_WEIGHT}
     */
    protected float getWeight( Properties props, Class<? extends Inspector> inspectorClass )
    {
        String key = PROP_INSPECTOR_WEIGHT_PREFIX + inspectorClass.getCanonicalName();
        float weight = DEFAULT_WEIGHT;
        String weightString = props.getProperty( key, String.valueOf( DEFAULT_WEIGHT ) );
        try
        {
            weight = Float.parseFloat( weightString );
        }
        catch( NumberFormatException e )
        {
        }
        return weight;
    }
}
