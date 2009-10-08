package org.apache.wiki.content.inspect;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.util.CryptoUtil;

import org.apache.wiki.WikiContext;

/**
 * <p>
 * {@link Inspector} implementation that validates whether the users's HTTP
 * session contains parameters that help detect potential bots.
 * </p>
 * 
 * @since 3.0
 */
public class BotTrapInspector implements Inspector
{
    /** Request parameter containing the UTF8 check. */
    public static final String REQ_ENCODING_CHECK = "__wikiEncodingcheck";

    public static final String REQ_TRAP_PARAM = "submit_auth";

    /** Request parameter containing the encoded payload. */
    public static final String REQ_SPAM_PARAM = "__wikiCheck";

    public void initialize( InspectionPlan config )
    {
    }

    public Finding[] inspect( Inspection inspection, String content, Change change )
    {
        WikiContext context = inspection.getContext();
        HttpServletRequest request = context.getHttpRequest();
        if( request == null )
        {
            return new Finding[] { new Finding( Topic.SPAM, Finding.Result.NO_EFFECT, "No HTTP request supplied." ) };
        }

        // If the trap field was set, it's probably a bot
        String trapParam = request.getParameter( REQ_TRAP_PARAM );
        if( trapParam != null && trapParam.length() > 0 )
        {
            return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED,
                                                "Bot detected: trap parameter was set (should have been null)." ) };
        }

        // Recover the encrypted parameter and then validate token field
        String encryptedParam = request.getParameter( REQ_SPAM_PARAM );
        if( encryptedParam == null )
        {
            return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED, "Missing encrypted spam parameter." ) };
        }
        else
        {
            // Token parameter should simply be the session ID
            String tokenParam = CryptoUtil.decrypt( encryptedParam );
            if( tokenParam == null )
            {
                return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED, "Could not obtain name of token parameter." ) };
            }
            String tokenValue = request.getParameter( tokenParam );
            if( tokenValue == null || !request.getSession().getId().equals( tokenValue ) )
            {
                return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED, "Could not obtain token value." ) };
            }
        }

        // Check for UTF-8 parameter
        String utf8field = request.getParameter( REQ_ENCODING_CHECK );
        if( utf8field == null || !utf8field.equals( "\u3041" ) )
        {
            return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED, "Bot detected: missing UTF-8 parameter." ) };
        }

        return new Finding[] { new Finding( Topic.SPAM, Finding.Result.PASSED, "Spam parameters ok." ) };
    }
}
