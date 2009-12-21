package org.apache.wiki.content.inspect;

import java.io.IOException;

import org.apache.wiki.WikiContext;
import org.apache.wiki.tags.SpamProtectTag;
import org.apache.wiki.ui.stripes.SpamInterceptor;
import org.apache.wiki.ui.stripes.SpamProtect;

/**
 * <p>
 * Describes how CAPTCHA classes should implement scripting and processing
 * methods test particular CAPTCHA schemes.
 * </p>
 * <p>
 * Captcha specifies three logical states, which are invoked by a combination of
 * custom JSP tags and the {@link SpamInterceptor} interceptor.
 * </p>
 * <ol>
 * <li><strong>formContent</strong>. When a JSP containing a &lt;SpamProtect&gt;
 * tag is encountered, the {@link SpamProtectTag#doEndTag()} method calls
 * {@link #formContent(WikiContext)} to generate any content needed to be
 * included in the &lt;form&gt; element of the page. Note that the
 * &lt;SpamProtect&gt tag is not actually guaranteed to be inside of the form
 * element, but in practice, it should be.</li>
 * <li><strong>check</strong>. This method provides any back-end CAPTCHA
 * processing that the implementation might need when the form contents are
 * posted, for example calling out to a remote service. It is invoked as part of
 * the content-inspection chain called by {@link SpamInterceptor}, which itself
 * is triggered whenever a Stripes event method with a {@link SpamProtect}
 * annotation is called. Implementations only need to return a simple boolean
 * value: {@code true} means the user passed the test; {@code false} means
 * failure.</li>
 * </ol>
 */
public interface Captcha
{
    /**
     * Generates any content needed in the &lt;form&gt; element of an HTML page.
     * For example, this method might return hidden &lt;input&gt; elements whose
     * contents are submitted to a remote CAPTCHA service. The content is
     * actually injected by the {@link SpamProtectTag} tag, when it is
     * encountered on the JSP.
     * 
     * @param context the current wiki context
     * @return the form content
     */
    public String formContent( WikiContext context ) throws IOException;

    /**
     * Tests the CAPTCHA.
     * 
     * @param inspection the current inspection. Callers can obtain the complete
     *            wiki and request context by calling
     *            {@link Inspection#getContext()}.
     * @return {@code true} if the test succeeded; {@code false} otherwise
     */
    public boolean check( Inspection inspection ) throws IOException;
    
    /**
     * Policy that specifies when a CAPTCHA is needed.
     */
    public static enum Policy
    {
        /** Always. */
        ALWAYS,
        
        /** Never. */
        NEVER
    }
}
