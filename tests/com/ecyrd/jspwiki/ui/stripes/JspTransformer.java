package com.ecyrd.jspwiki.ui.stripes;

import java.util.Map;

/**
 * Strategy interface for transforming JSPs.
 */
public interface JspTransformer
{
    /**
     * Initializes the transformer. This method should be called only once, when
     * the transformer is initialized.
     * 
     * @param sharedState a map containing key/value pairs that represent any
     *            shared-state information that this method might need during
     *            transformation.
     * @param doc the JSP to transform
     */
    public void initialize( Map<String, Object> sharedState, JspDocument doc );

    /**
     * Executes the transformation on the JSP and returns the result. This
     * method is called for each File migrated.
     * 
     * @param sharedState a map containing key/value pairs that represent any
     *            shared-state information that this method might need during
     *            transformation.
     * @param doc the JSP to transform
     */
    public void transform( Map<String, Object> sharedState, JspDocument doc );
}
