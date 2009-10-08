package org.apache.wiki.content.inspect;

import java.util.Properties;

import org.apache.wiki.WikiContext;
import org.apache.wiki.util.TextUtil;

/**
 * {@link Inspector} implementation that varies Spam scores depending on whether
 * the user is logged in, or is an administrative user.
 */
public class UserInspector implements Inspector
{
    /**
     * The filter property name for specifying whether authenticated users
     * should be ignored. Value is <tt>{@value}</tt>.
     */
    public static final String PROP_IGNORE_AUTHENTICATED = "ignoreauthenticated";

    /**
     * If set to true, will ignore anyone who is in Authenticated role.
     */
    private boolean m_ignoreAuthenticated = false;

    public void initialize( InspectionPlan config )
    {
        Properties props = config.getProperties();
        m_ignoreAuthenticated = TextUtil.getBooleanProperty( props, PROP_IGNORE_AUTHENTICATED, m_ignoreAuthenticated );
    }

    /**
     * If the user is an admin user, or if the Inspector has been configured to
     * ignore authenticated users, this method returns
     * {@link Finding.Result#PASSED}. Otherwise, the method returns {@code null}
     * but does not affect the score in any way.
     */
    public Finding[] inspect( Inspection inspection, String content, Change change )
    {
        WikiContext context = inspection.getContext();
        if( context.hasAdminPermissions() )
        {
            return new Finding[] { new Finding( Topic.SPAM, Finding.Result.PASSED, "User is admin." ) };
        }

        if( m_ignoreAuthenticated && context.getWikiSession().isAuthenticated() )
        {
            return new Finding[] { new Finding( Topic.SPAM, Finding.Result.PASSED, "User is authenticated." ) };
        }
        return null;
    }

}
