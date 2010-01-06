package org.apache.wiki.content.inspect;

import java.io.IOException;

import org.apache.wiki.WikiContext;
import org.apache.wiki.tags.SpamProtectTag;
import org.apache.wiki.ui.stripes.SpamInterceptor;
import org.apache.wiki.ui.stripes.SpamProtect;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;

/**
 * <p>
 * Describes how Challenge classes should implement scripting and processing
 * methods to test particular Challenge schemes, such as CAPTCHA.
 * </p>
 * <p>
 * Challenge specifies three logical states, which are invoked by a combination
 * of custom JSP tags and the {@link SpamInterceptor} interceptor.
 * </p>
 * <ol>
 * <li><strong>formContent</strong>. When a JSP containing a &lt;SpamProtect&gt;
 * tag is encountered, the {@link SpamProtectTag#doEndTag()} method calls
 * {@link #formContent(WikiContext)} to generate any content needed to be
 * included in the &lt;form&gt; element of the page. Note that the
 * &lt;SpamProtect&gt tag is not actually guaranteed to be inside of the form
 * element, but in practice, it should be.</li>
 * <li><strong>check</strong>. This method provides any back-end Challenge
 * processing that the implementation might need when the form contents are
 * posted, for example calling out to a remote service. It is invoked as part of
 * the content-inspection chain called by {@link SpamInterceptor}, which itself
 * is triggered whenever a Stripes event method with a {@link SpamProtect}
 * annotation is called. Implementations only need to return a simple boolean
 * value: {@code true} means the user passed the test; {@code false} means
 * failure.</li>
 * </ol>
 */
public interface Challenge
{
    /**
     * Generates any content needed in the &lt;form&gt; element of an HTML page.
     * For example, this method might return hidden &lt;input&gt; elements whose
     * contents are submitted to a remote CAPTCHA service. The content is
     * actually injected by the {@link SpamProtectTag} tag, when it is
     * encountered on the JSP.
     * 
     * @param actionBeanContext the current ActionBeanContext. Callers can obtain the complete
     *            request context by calling {@link ActionBeanContext#getRequest().
     * @return the form content
     */
    public String formContent( WikiActionBeanContext actionBeanContext ) throws IOException;

    /**
     * Tests the Challenge.
     * 
     * @param actionBeanContext the current ActionBeanContext. Callers can obtain the complete
     *            request context by calling {@link ActionBeanContext#getRequest().
     * @return {@code true} if the test succeeded; {@code false} otherwise
     */
    public boolean check( WikiActionBeanContext actionBeanContext ) throws IOException;

    public enum Request
    {
        /**
         * No challenge requested, but CAPTCHA should be generated if
         * content-inspection determines that the ActionBean contains spam.
         */
        CAPTCHA_ON_DEMAND,

        /** CAPTCHA was requested. */
        CAPTCHA,

        /**
         * Challenge parameter was omitted, possibly because a spammer submitted
         * the request.
         */
        OMITTED,

        /** Password challenge was requested. */
        PASSWORD
    }
}
