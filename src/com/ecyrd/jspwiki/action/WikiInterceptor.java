package com.ecyrd.jspwiki.action;

import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.*;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.SessionMonitor;

/**
 * <p>
 * Stripes {@link net.sourceforge.stripes.controller.Interceptor} that
 * instantiates the correct WikiContext associated with JSPs, checks for access,
 * and redirects users if necessary. The interceptor executes after the second
 * lifecycle stage, <em>aka</em>
 * {@link net.sourceforge.stripes.controller.LifecycleStage#HandlerResolution},
 * but before the third stage,
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}.
 * </p>
 * <p>
 * WikiInterceptor assumes primary responsibility for making JSPWiki objects
 * available to JSPs as variables. In particular, when WikiInterceptor fires
 * during the binding and validation stage, sets the following PageContext
 * attributes, all in {@link PageContext#REQUEST_SCOPE}:
 * </p>
 * <ul>
 * <li><code>wikiEngine</code> - the {@link com.ecyrd.jspwiki.WikiEngine}</li>
 * <li><code>wikiSession</code> - the user's
 * {@link com.ecyrd.jspwiki.WikiSession}</li>
 * <li><code>wikiActionBean</code> - the
 * {@link com.ecyrd.jspwiki.action.WikiActionBean} injected by Stripes</li>
 * <li><code>wikiPage</code> - the {@link com.ecyrd.jspwiki.WikiPage}
 * associated with the WikiActionBean, or the "front page" if the WikiActionBean
 * is not a WikiContext</li>
 * </ul>
 * <p>
 * After the intercept method fires, calling classes can obtain the saved
 * WikiActionBean by calling
 * {@link WikiActionBeanFactory#findActionBean(javax.servlet.ServletRequest)}. This is
 * the recommended method that  JSP scriptlet code should use.
 * </p>
 * <p>
 * Because these objects are saved as attributes, they are available to JSPs as
 * the Expression Language variables <code>${wikiEngine}</code>,
 * <code>${wikiSession}</code>, <code>${wikiActionBean}</code> and
 * <code>${wikiPage}</code>.
 * </p>
 * 
 * @author Andrew Jaquith
 */
@Intercepts( { LifecycleStage.HandlerResolution } )
public class WikiInterceptor implements Interceptor
{
    /**
     * The PageContext attribute name of the WikiActionBean stored by
     * WikiInterceptor.
     */
    public static final String ATTR_ACTIONBEAN = "wikiActionBean";

    /**
     * The PageContext attribute name of the WikiEngine stored by
     * WikiInterceptor.
     */
    public static final String ATTR_WIKIENGINE = "wikiEngine";

    /**
     * The PageContext attribute name of the WikiSession stored by
     * WikiInterceptor.
     */
    public static final String ATTR_WIKISESSION = "wikiSession";

    private static final Logger log = Logger.getLogger( WikiInterceptor.class );

    /**
     * <p>
     * Intercepts the
     * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
     * lifecycle stage and checks for proper access to the current ActionBean
     * and target event. The access-checking logic runs after after the rest of
     * the BindingAndValidation processing logic does, after which point Stripes
     * has already discovered the correct ActionBean, and bound and validated
     * its request parameters.
     * </p>
     * <p>
     * To determine if the user is allowed to access the target event method,
     * the method is examined to see if contains a
     * {@link com.ecyrd.jspwiki.action.HandlerPermission}) annotation that
     * specifies the required {@link java.security.Permission}. If the user
     * does not possess the Permission -- that is,
     * {@link com.ecyrd.jspwiki.auth.AuthorizationManager#checkPermission(WikiSession, Permission)}
     * returns <code>false</code> -- this method returns a RedirectResolution
     * to the login page, with all current parameters appended.
     * </p>
     * <p>
     * If access is allowed, this method injects the WikiEngine, WikiSession,
     * resolved WikiActionBean and WikiPage as request-scoped PageContext
     * attributes, and returns a <code>null</code>. After the objects are
     * injected, downstream classes like WikiTagBase can use them. The attribute
     * can also be accessed as variables using the JSP Expression Language
     * (example: <code>${wikiPage}</code>).
     * 
     * @param context the current execution context
     * @return a Resolution if the
     *         {@link net.sourceforge.stripes.controller.LifecycleStage#HandlerResolution}
     *         lifecycle stage's normal execution returns one; <code>null</code>
     *         otherwise
     * @throws Exception if the underlying lifcycle stage's execution throws an
     *             Exception
     */
    public Resolution intercept( ExecutionContext context ) throws Exception
    {
        // Did the handler resolution stage return a Resolution? If so, bail.
        Resolution r = context.proceed();
        if( r != null )
        {
            return r;
        }

        // Get the resolved ActionBean and event handler method
        WikiActionBean actionBean = (WikiActionBean) context.getActionBean();
        WikiActionBeanContext beanContext = actionBean.getContext();
        Method handler = context.getHandler();

        // Does the event handler have a required permission?
        boolean allowed = true;
        Map<Method, HandlerInfo> handlerInfos = HandlerInfo.getHandlerInfoCollection( actionBean.getClass() );
        HandlerInfo eventInfo = handlerInfos.get( handler );
        if( eventInfo != null )
        {
            Permission requiredPermission = eventInfo.getPermission( actionBean );
            if( requiredPermission != null )
            {
                WikiEngine engine = actionBean.getEngine();
                AuthorizationManager mgr = engine.getAuthorizationManager();
                WikiSession wikiSession = actionBean.getWikiSession();
                allowed = mgr.checkPermission( wikiSession, requiredPermission );
            }
        }

        // If not allowed, redirect to login page with all parameters intact;
        // otherwise proceed
        if( !allowed )
        {
            r = new RedirectResolution( LoginActionBean.class );
            ((RedirectResolution) r).includeRequestParameters( true );
            if( log.isDebugEnabled() )
            {
                log.debug( "WikiInterceptor rejected access to ActionBean=" + actionBean.getClass().getCanonicalName() + ", method=" + handler.getName() );
            }
            return r;
        }

        if( log.isDebugEnabled() )
        {
            log.debug( "WikiInterceptor resolved ActionBean: " + actionBean );
        }

        // If not already set, inject WikiEngine as a request attribute (can be
        // used later as ${wikiEngine} in EL markup)
        WikiEngine engine = beanContext.getWikiEngine();
        HttpServletRequest httpRequest = beanContext.getRequest();
        httpRequest.setAttribute( ATTR_WIKIENGINE, engine );

        // If not already set, Inject the WikiSession as a request attribute
        WikiSession wikiSession = SessionMonitor.getInstance( engine ).find( httpRequest.getSession() );
        httpRequest.setAttribute( ATTR_WIKISESSION, wikiSession );

        // Stash the WikiActionBean and WikiPage in the PageContext
        // Note: it probably seems a bit tricky that we're grabbing the
        // PageContext from Stripes. We happen
        // to know, thanks to the glories of open source code, that Stripes
        // calls DispatcherHelper's
        // setPageContext() method immediately before executing the
        // BindingAndValidation stage,
        // in *both* the <stripes:useActionBean> case and the StripesFilter
        // case.
        // So, the PageContext safe to grab, and boy are we glad that we can!
        PageContext pageContext = DispatcherHelper.getPageContext();
        if( pageContext != null )
        {
            // Save ActionBean to the current context
            WikiActionBeanFactory.saveActionBean( pageContext, actionBean );
        }

        return null;
    }

}
