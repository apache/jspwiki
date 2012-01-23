package org.apache.wiki.content.inspect;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.wiki.content.inspect.Finding.Result;
import org.apache.wiki.util.TextUtil;

/**
 * <p>A plan for inspection that defines the sequence of {@link Inspector} objects
 * for analyzing content. Inspector objects are added to the InspectionPlan by
 * calling {@link #addInspector(Inspector, float)} with a desired weight,
 * expressed as a floating-point number. The InspectionPlan includes convenience
 * methods for accessing WikiEngine properties to allow each Inspector to
 * initialize itself. References to shared-state objects such as the
 * {@link ReputationManager} are included for Inspectors to use.</p>
 * <p>The InspectionPlan class is not thread-safe. It may be extended.</p>
 */
public class InspectionPlan
{
    private final Properties m_props;

    private final ReputationManager m_reputationManager;

    /**
     * Returns the {@link ReputationManager} associated with the InspectionPlan.
     * 
     * @return the reputation manager
     */
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

    /**
     * Constructs a new InspectionPlan with a supplied set of properties for
     * configuration. When the plan is instantiated, a new
     * {@link ReputationManager} will also be created. Its ban-time will be set
     * to the value specified in the properties file by key
     * {@link #PROP_BANTIME}.
     * 
     * @param props the wiki properties
     */
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
    }

    /**
     * Returns the array of {@link Inspector} objects that are part of the
     * InspectionPlan. The order of the array reflects the order in which the
     * Inspectors were added.
     * 
     * @return the array, which may be zero-length if no Inspectors were added
     *         before calling this method
     */
    public Inspector[] getInspectors()
    {
        Set<Inspector> inspectors = m_inspectors.keySet();
        return inspectors.toArray( new Inspector[inspectors.size()] );
    }

    /**
     * Returns the weight for a particular Inspector. If no weight was assigned
     * or if the inspector does not exist, returns {0f}.
     * 
     * @param inspector the inspector whose weight is being sought
     * @return the weight
     */
    public float getWeight( Inspector inspector )
    {
        Float weight = m_inspectors.get( inspector );
        return weight == null ? 0f : weight.floatValue();
    }

    /**
     * The wiki properties used to initialize the InspectionPlan.
     * 
     * @return the properties
     */
    public Properties getProperties()
    {
        return m_props;
    }
}
