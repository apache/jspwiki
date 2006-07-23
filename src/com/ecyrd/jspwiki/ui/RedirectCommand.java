package com.ecyrd.jspwiki.ui;

import java.security.Permission;

/**
 * <p>Defines Commands for redirections to off-site special pages. 
 * RedirectCommands do not have associated
 * permissions; the {@link #requiredPermission()} method will
 * always return <code>null</code>. When combined with a supplied String
 * url, the {@link #getTarget()} method will return a String, the
 * {@link #getURLPattern()} method will return the supplied target URL,
 * and {@link #getJSP()} method will return the "cleansed" URL.</p>
 * <p>This class is not <code>final</code>; it may be extended in
 * the future.</p>
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-07-23 20:10:52 $
 * @since 2.4.31
 */
public class RedirectCommand extends AbstractCommand
{

    public static final Command REDIRECT
        = new RedirectCommand( "", "%u%n", null, null );

    /**
     * Constructs a new Command with a specified wiki context, URL pattern,
     * type, and content template. The WikiPage for this action is initialized
     * to <code>null</code>.
     * @param requestContext the request context
     * @param urlPattern the URL pattern
     * @param contentTemplate the content template; may be <code>null</code>
     * @param target the target of the command
     * @return IllegalArgumentException if the request content, URL pattern, or
     *         type is <code>null</code>
     */
    private RedirectCommand( String requestContext, String urlPattern, String contentTemplate, String target )
    {
        super( requestContext, urlPattern, contentTemplate, target );
    }
    
    /**
     * Creates and returns a targeted Command by combining a URL
     * (as String) with this Command. The supplied <code>target</code>
     * object must be non-<code>null</code> and of type String.
     * The URL passed to the constructor is actually an
     * URL pattern, but it will be converted to a JSP page if it is a partial
     * URL. If it is a full URL (beginning with <code>http://</code> or
     * <code>https://</code>), it will be "passed through" without
     * conversion, and the URL pattern will be <code>null</code>.
     * @param target the object to combine
     * @throws IllegalArgumentException if the target is not of the correct type
     */
    public final Command targetedCommand( Object target )
    {
        if ( !( target != null && target instanceof String ) )
        {
            throw new IllegalArgumentException( "Target must non-null and of type String." );
        }
        return new RedirectCommand( getRequestContext(), (String)target, getContentTemplate(), (String)target );
    }
    
    /**
     * @see com.ecyrd.jspwiki.ui.Command#getName()
     */
    public final String getName()
    {
        Object target = getTarget();
        if ( target == null )
        {
            return getJSP();
        }
        return target.toString();
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see com.ecyrd.jspwiki.ui.Command#requiredPermission()
     */
    public final Permission requiredPermission()
    {
        return null;
    }
}
