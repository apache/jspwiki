/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */

package org.apache.wiki.ui.stripes;

import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.action.LoginActionBean;
import org.apache.wiki.action.WikiActionBean;
import org.apache.wiki.action.WikiContextFactory;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;

import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.*;

/**
 * <p>
 * Stripes {@link net.sourceforge.stripes.controller.Interceptor} that
 * instantiates the correct WikiContext associated with JSPs, checks for access,
 * and redirects users if necessary. The interceptor executes three times: the first
 * time is after the first lifecycle state, <em>aka</em>
 * {@link  net.sourceforge.stripes.controller.LifecycleStage#ActionBeanResolution}
 * but before the second stage.
 * {@link net.sourceforge.stripes.controller.LifecycleStage#HandlerResolution}.
 * The second time the interceptor executes is after the second lifecycle stage,
 * {@link net.sourceforge.stripes.controller.LifecycleStage#HandlerResolution}.
 * The third time the interceptor executes is after the third lifecycle stage,
 * aka
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation},
 * but before the fourth stage
 * {@link net.sourceforge.stripes.controller.LifecycleStage#CustomValidation}.
 * See the
 * </p>
 * <p>
 * WikiInterceptor assumes primary responsibility for making JSPWiki objects
 * available to JSPs as variables. In particular, when WikiInterceptor fires
 * during the binding and validation stage, sets the following PageContext
 * attributes, all in {@link PageContext#REQUEST_SCOPE}:
 * </p>
 * <ul>
 * <li><code>wikiEngine</code> - the {@link org.apache.wiki.WikiEngine}</li>
 * <li><code>wikiSession</code> - the user's
 * {@link org.apache.wiki.WikiSession}</li>
 * <li><code>wikiActionBean</code> - the
 * {@link org.apache.wiki.action.WikiActionBean} injected by Stripes</li>
 * <li><code>wikiPage</code> - the {@link org.apache.wiki.api.WikiPage}
 * associated with the WikiActionBean, or the "front page" if the WikiActionBean
 * is not a WikiContext</li>
 * </ul>
 * <p>
 * After the intercept method fires, calling classes can obtain the saved
 * WikiActionBean by calling
 * {@link WikiInterceptor#findActionBean(javax.servlet.ServletRequest)}. This
 * is the recommended method that JSP scriptlet code should use.
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
@Intercepts( { LifecycleStage.ActionBeanResolution, LifecycleStage.HandlerResolution, LifecycleStage.CustomValidation } )
public class WikiInterceptor implements Interceptor
{
    private static final Logger log = LoggerFactory.getLogger( WikiInterceptor.class );

    /**
     * The PageContext attribute name of the WikiActionBean stored by
     * WikiInterceptor.
     */
    public static final String ATTR_ACTIONBEAN = "wikiActionBean";

    /**
     * Intercepts the Stripes lifecycle stages and dispatches execution to
     * delegate methods
     * {@link #interceptAfterActionBeanResolution(ExecutionContext)} and
     * {@link #interceptAfterBindingAndValidation(ExecutionContext)}, whichever
     * is appropriate.
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
        if( LifecycleStage.ActionBeanResolution.equals( context.getLifecycleStage() ) )
        {
            return interceptAfterActionBeanResolution( context );
        }
        else if( LifecycleStage.HandlerResolution.equals( context.getLifecycleStage() ) )
        {
            return interceptAfterHandlerResolution( context );
        }
        else if( LifecycleStage.CustomValidation.equals( context.getLifecycleStage() ) )
        {
            return interceptAfterBindingAndValidation( context );
        }
        return null;
    }

    /**
     * After the Stripes event handler method has been identified, this method
     * sets the corresponding JSPWiki request context for the WikiContext.
     * Because Stripes event handler methods correspond exactly one-to-one
     * with pre-3.0 JSPWiki requests contexts, we allows Stripes to identify the
     * handler method first, then make sure the WikiContext's context is synchronized.
     * @param context the execution context
     * @return always returns <code>null</code>
     */
    protected Resolution interceptAfterHandlerResolution( ExecutionContext context ) throws Exception
    {
        // Did the handler resolution stage return a Resolution? If so, bail.
        Resolution r = context.proceed();
        if( r != null )
        {
            return r;
        }

        // Get the event handler method
        Method handler = context.getHandler();

        // Make sure we set the WikiContext request context, while we're at it
        WikiActionBean actionBean = (WikiActionBean) context.getActionBean();
        Map<Method, HandlerInfo> eventinfos = HandlerInfo.getHandlerInfoCollection( actionBean.getClass() );
        HandlerInfo eventInfo = eventinfos.get( handler );
        if( eventInfo != null )
        {
            String requestContext = eventInfo.getRequestContext();
            actionBean.getContext().setRequestContext( requestContext );
        }
        return null;
    }

    /**
     * After the Stripes
     * {@link net.sourceforge.stripes.controller.LifecycleStage#ActionBeanResolution}
     * executes, this method injects the current WikiEngine, WikiSession and
     * WikiActionBean into request scope, and returns <code>null</code>.
     * After the objects are injected, downstream classes like WikiTagBase can
     * use them. The attribute can also be accessed as variables using the JSP
     * Expression Language (example: <code>${wikiPage}</code>).
     * 
     * @param context the execution context
     * @return a Resolution if the
     *         {@link net.sourceforge.stripes.controller.LifecycleStage#ActionBeanResolution}
     *         lifecycle stage's normal execution returns one; <code>null</code>
     *         otherwise
     * @throws Exception if the underlying lifcycle stage's execution throws an
     *             Exception
     */
    protected Resolution interceptAfterActionBeanResolution( ExecutionContext context ) throws Exception
    {
        // Did the handler resolution stage return a Resolution? If so, bail.
        Resolution r = context.proceed();
        if( r != null )
        {
            return r;
        }

        // Retrieve the ActionBean, its ActionBeanContext, and HTTP request
        WikiActionBean actionBean = (WikiActionBean) context.getActionBean();
        WikiActionBeanContext actionBeanContext = actionBean.getContext();
        HttpServletRequest request = actionBeanContext.getRequest();

        // Set the WikiSession, if not set yet
        if( actionBeanContext.getWikiSession() == null )
        {
            WikiEngine engine = actionBeanContext.getEngine();
            WikiSession wikiSession = SessionMonitor.getInstance( engine ).find( request.getSession() );
            actionBeanContext.setWikiSession( wikiSession );
            
            // Stash WikiEngine as a request attribute (can be
            // used later as ${wikiEngine} in EL markup)
            request.setAttribute( WikiContextFactory.ATTR_WIKIENGINE, engine );

            // Stash the WikiSession as a request attribute
            request.setAttribute( WikiContextFactory.ATTR_WIKISESSION, wikiSession );
        }

        // Stash the ActionBean as request attribute, if not saved yet
        if( request.getAttribute( ATTR_ACTIONBEAN ) == null )
        {
            request.setAttribute( ATTR_ACTIONBEAN, actionBean );
        }

        // Stash the WikiContext
        WikiContextFactory.saveContext( request, actionBean.getContext() );

        if( log.isDebugEnabled() )
        {
            log.debug( "WikiInterceptor resolved ActionBean: " + actionBean );
        }

        return null;
    }

    /**
     * <p>
     * Intercepts the
     * {@link net.sourceforge.stripes.controller.LifecycleStage#CustomValidation}
     * lifecycle stage, to ensure that it runs after all
     * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
     * interceptors, and checks for proper access to the current ActionBean and
     * target event. The access-checking logic runs after after the rest of the
     * BindingAndValidation processing logic does, after which point Stripes has
     * already discovered the correct ActionBean, and bound and validated its
     * request parameters.
     * </p>
     * <p>
     * To determine if the user is allowed to access the target event method,
     * the method is examined to see if contains a
     * {@link org.apache.wiki.ui.stripes.HandlerPermission}) annotation that
     * specifies the required {@link java.security.Permission}. If the user
     * does not possess the Permission -- that is,
     * {@link org.apache.wiki.auth.AuthorizationManager#checkPermission(WikiSession, Permission)}
     * returns <code>false</code> -- this method returns a RedirectResolution
     * to the login page, with all current parameters appended.
     * </p>
     * 
     * @param context the execution context
     * @return a Resolution if the
     *         {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
     *         lifecycle stage's normal execution returns one; <code>null</code>
     *         otherwise
     * @throws Exception if the underlying lifcycle stage's execution throws an
     *             Exception
     */
    protected Resolution interceptAfterBindingAndValidation( ExecutionContext context ) throws Exception
    {
        // Stash the WikiActionBean as a PageContext attribute
        WikiActionBean actionBean = (WikiActionBean) context.getActionBean();
        PageContext pageContext = DispatcherHelper.getPageContext();
        if( pageContext != null )
        {
            pageContext.setAttribute( ATTR_ACTIONBEAN, actionBean );
        }

        // Did the handler resolution stage return a Resolution? If so, bail.
        Resolution r = context.proceed();
        if( r != null )
        {
            return r;
        }

        // Get the event handler method
        Method handler = context.getHandler();
        Map<Method, HandlerInfo> eventinfos = HandlerInfo.getHandlerInfoCollection( actionBean.getClass() );
        HandlerInfo eventInfo = eventinfos.get( handler );

        // Does the event handler have a required permission?
        boolean allowed = true;
        if( eventInfo != null )
        {
            Permission requiredPermission = eventInfo.getPermission( actionBean );
            if( requiredPermission != null )
            {
                WikiEngine engine = actionBean.getContext().getEngine();
                AuthorizationManager mgr = engine.getAuthorizationManager();
                WikiSession wikiSession = actionBean.getContext().getWikiSession();
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
                log.debug( "WikiInterceptor rejected access to ActionBean=" + actionBean.getClass().getCanonicalName()
                           + ", method=" + handler.getName() );
            }
            return r;
        }

        return null;
    }

    /**
     * Returns the WikiActionBean associated with the current
     * {@link javax.servlet.jsp.PageContext}, which may have been previously
     * stashed by {@link #interceptAfterBindingAndValidation(ExecutionContext)}.
     * Note that each PageContext can contain its own ActionBean. The ActionBean
     * will be retrieved from page-scope attribute
     * {@link WikiInterceptor#ATTR_ACTIONBEAN}. If the WikiActionBean cannot be
     * obtained as a page-scope attribute, the request scope will be tried also.
     * 
     * @param pageContext the page context
     * @return the WikiActionBean
     * @throws IllegalStateException if the WikiActionBean was not found in the
     *             page context or
     */
    public static WikiActionBean findActionBean( PageContext pageContext )
    {
        WikiActionBean bean = (WikiActionBean) pageContext.getAttribute( WikiInterceptor.ATTR_ACTIONBEAN );
        if( bean == null )
        {
            bean = findActionBean( pageContext.getRequest() );
            if( bean == null )
            {
                log.debug( "WikiActionBean not found under page context attribute '" + WikiInterceptor.ATTR_ACTIONBEAN
                           + "'! Something failed to stash it..." );
            }
        }
        return bean;
    }

    /**
     * Returns the WikiActionBean associated with the current
     * {@link javax.servlet.http.HttpServletRequest}, which was previously
     * stashed by {@link #interceptAfterActionBeanResolution(ExecutionContext)}.
     * Only the first ActionBean on a JSP will be stashed as a request-level
     * attribute. The ActionBean will be retrieved from attribute
     * {@link WikiInterceptor#ATTR_ACTIONBEAN}.
     * 
     * @param request the HTTP request
     * @return the WikiActionBean
     * @throws IllegalStateException if the WikiActionBean was not found in the
     *             request scope
     */
    public static WikiActionBean findActionBean( ServletRequest request )
    {
        WikiActionBean bean = (WikiActionBean) request.getAttribute( WikiInterceptor.ATTR_ACTIONBEAN );
        if( bean == null )
        {
            log.debug( "WikiActionBean not found under request attribute '" + WikiInterceptor.ATTR_ACTIONBEAN
                       + "'! Something failed to stash it..." );
            bean = (WikiActionBean) request.getAttribute( WikiInterceptor.ATTR_ACTIONBEAN );
        }
        return bean;
    }

}
