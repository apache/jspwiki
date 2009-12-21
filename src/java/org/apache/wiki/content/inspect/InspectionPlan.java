package org.apache.wiki.content.inspect;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.wiki.content.inspect.Finding.Result;
import org.apache.wiki.util.TextUtil;

/**
 * A plan for inspection that defines the sequence of {@link Inspector} objects
 * for analyzing content. Inspector objects are added to the InspectionPlan by
 * calling {@link #addInspector(Inspector, float)} with a desired weight,
 * expressed as a floating-point number. The InspectionPlan includes convenience
 * methods for accessing WikiEngine properties to allow each Inspector to
 * initialize itself. References to shared-state objects such as the
 * {@link ReputationManager} are included for Inspectors to use.
 */
public class InspectionPlan
{
    private final Properties m_props;

    private final ReputationManager m_reputationManager;

    private Captcha m_captcha = null;

    public ReputationManager getReputationManager()
    {
        return m_reputationManager;
    }

    /**
     * The filter property name for specifying how long a host is banned, in
     * minutes. Value is <tt>{@value}</tt>.
     */
    public static final String PROP_BANTIME = "bantime";

    private final Map<Inspector, Float> m_inspectors;

    public InspectionPlan( Properties props )
    {
        super();
        m_props = props;
        int banTime = TextUtil.getIntegerProperty( props, PROP_BANTIME, 60 );
        m_inspectors = new LinkedHashMap<Inspector, Float>();
        m_reputationManager = new ReputationManager( banTime );
    }

    /**
     * Adds an Inspector to this InspectionPlan with a specified weight. If the
     * Inspector returns {@link Result#PASSED} the weight will be added to the
     * overall score for the {@link Topic}; if it returns {@link Result#FAILED}
     * it will be subtracted. When the Inspector is added, its
     * {@link Inspector#initialize(InspectionPlan)} method will be called.
     * 
     * @param inspector the inspector to add
     * @param weight the weight to assign to the inspector
     */
    public void addInspector( Inspector inspector, float weight )
    {
        m_inspectors.put( inspector, Float.valueOf( weight ) );
        inspector.initialize( this );
        if ( inspector instanceof CaptchaInspector )
        {
            m_captcha = ((CaptchaInspector)inspector).getCaptcha();
        }
    }

    /**
     * Returns the {@link Captcha} object, if one was initialized by
     * a {@link CaptchaInspector} passed to {@link #addInspector(Inspector, float)}.
     * @return the Captcha, or {@code null} if not supplied or configured
     */
    public Captcha getCaptcha()
    {
        return m_captcha;
    }

    public Inspector[] getInspectors()
    {
        Set<Inspector> inspectors = m_inspectors.keySet();
        return inspectors.toArray( new Inspector[inspectors.size()] );
    }

    public float getWeight( Inspector inspector )
    {
        Float weight = m_inspectors.get( inspector );
        return weight == null ? 0f : weight.floatValue();
    }

    public Properties getProperties()
    {
        return m_props;
    }
}
