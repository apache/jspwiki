package org.apache.wiki.content.inspect;

/**
 * Inspects content for relative goodness or badness and returns one or more
 * {@link Finding} objects indicating the result of the inspection. Inspectors
 * should be coded as immutable objects and therefore thread-safe.
 */
public interface Inspector
{
    /**
     * Initializes the inspector with a given InspectionPlan. The InspectionPlan
     * contains references to the WikiEngine, the properties used to initialize
     * the WikiEngine, the ban list, and the list of IP addresses that have
     * successfully made modifications.
     * 
     * @param config the inspection context
     */
    public void initialize( InspectionPlan config );

    /**
     * Inspects the content and returns one or more Findings resulting from the
     * inspection. If the inspection is inconclusive, implementations can return
     * a Finding whose result is {@link Finding.Result#NO_EFFECT} or just
     * {@code null}.
     * 
     * @param inspection the current Inspection
     * @param change the current contents, plus content that represents the added or
     *            deleted text since the last change
     * @return the Findings
     */
    public Finding[] inspect( Inspection inspection, Change change );

    /**
     * Returns the {@link Scope} of the inspector: per-field or per-request.
     * Request-scoped inspectors are invoked just once for the entire request.
     * Field-scoped inspectors are invoked on every field that is being
     * protected.
     */
    public Scope getScope();

    /**
     * The scope of an inspector, which can operate on individual fields or for
     * the entire request.
     */
    public enum Scope
    {
        /** Inspector that should be executed for every field. */
        FIELD,

        /** Inspector that should be executed just once per form. */
        REQUEST
    }
}
