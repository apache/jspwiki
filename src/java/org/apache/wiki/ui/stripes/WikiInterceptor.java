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
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.*;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.action.*;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.preferences.Preferences;
import org.slf4j.MDC;

/**
 * <p>
 * Stripes {@link net.sourceforge.stripes.controller.Interceptor} that
 * instantiates the correct WikiContext associated with JSPs, checks for access,
 * and redirects users if necessary. The interceptor interleaves essential
 * JSPWiki features before and after the major Stripes lifecycle request states
 * as follows:
 * </p>
 * <p>
 * <strong>After first stage:
 * {@link net.sourceforge.stripes.controller.LifecycleStage#ActionBeanResolution}
 * </strong>
 * </p>
 * <p>
 * After the ActionBeanResolution stage executes, WikiInterceptor injects the
 * current WikiEngine, WikiSession and WikiActionBean into request scope as
 * attributes. The WikiActionBean is also injected into page scope. These can be
 * used in JSP EL expressions using the variables {@code wikiEngine}, {@code
 * wikiSession}, and {@code wikiActionBean}. The user's {@link Preferences} are
 * also set up by parsing request cookies. Java classes can obtain the saved
 * WikiActionBean by calling
 * {@link WikiInterceptor#findActionBean(javax.servlet.ServletRequest)}.
 * </p>
 * <p>
 * <strong>After second stage:
 * {@link net.sourceforge.stripes.controller.LifecycleStage#HandlerResolution}
 * </strong>
 * </p>
 * <p>
 * After the event handler method has been identified by Stripes,
 * WikiInterceptor sets the JSPWiki request context by calling
 * {@link org.apache.wiki.WikiContext#setRequestContext(String)}. Because
 * Stripes event handler methods correspond exactly one-to-one with pre-3.0
 * JSPWiki requests contexts, we allow Stripes to identify the handler method
 * first, then set the WikiContext request context to the corresponding request
 * context value. Also, if the ActionBean is of type ViewActionBean, the page is
 * set to the WikiEngine's front page as the default. Finally, the WikiEngine is
 * examined to see if it has been configured. If not, WikiInterceptor returns a
 * {@link RedirectResolution} that redirects the user to the Installer page.
 * </p>
 * <p>
 * <strong>After third stage:
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
 * </strong>
 * </p>
 * <p>
 * After the ActionBean and event method is resolved, and all of the binding and
 * validation activities have completed, WikiInterceptor checks to see if the
 * user has privileges to access the target ActionBean and event. If not,
 * WikiInterceptor returns a {@link RedirectResolution} that redirects the user
 * to the Login page.
 * </p>
 */
@Intercepts( { LifecycleStage.RequestInit, LifecycleStage.ActionBeanResolution, LifecycleStage.HandlerResolution, LifecycleStage.CustomValidation, LifecycleStage.RequestComplete } )
public class WikiInterceptor implements Interceptor
{
    /**
     * Internal flag indicating whether the WikiEngine has been configured
     * properly.
     */
    private boolean m_isConfigured = false;

    private static final Logger log = LoggerFactory.getLogger( WikiInterceptor.class );

    /**
     * The PageContext attribute name of the WikiActionBean stored by
     * WikiInterceptor.
     */
    public static final String ATTR_ACTIONBEAN = "wikiActionBean";

    /**
     * Intercepts the Stripes lifecycle stages and dispatches execution to
     * delegate methods
     * {@link #interceptAfterActionBeanResolution(ExecutionContext)},
     * {@link #interceptAfterBindingAndValidation(ExecutionContext)}, and
     * {@link #interceptAfterHandlerResolution(ExecutionContext)} whichever is
     * appropriate.
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
        switch( context.getLifecycleStage() )
        {
            case RequestInit: {
                HttpServletRequest request = context.getActionBeanContext().getRequest();
                WikiEngine engine = ((WikiActionBeanContext) context.getActionBeanContext()).getEngine();
                MDC.put( engine.getApplicationName() + ":" + request.getRequestURI(), "WikiInterceptor" );
                break;
            }
            case ActionBeanResolution: {
                return interceptAfterActionBeanResolution( context );
            }
            case HandlerResolution: {
                return interceptAfterHandlerResolution( context );
            }
            case CustomValidation: {
                return interceptAfterBindingAndValidation( context );
            }
            case RequestComplete: {
                HttpServletRequest request = context.getActionBeanContext().getRequest();
                WikiEngine engine = ((WikiActionBeanContext) context.getActionBeanContext()).getEngine();
                MDC.remove( engine.getApplicationName() + ":" + request.getRequestURI() );
                break;
            }
        }
        return null;
    }

    /**
     * After the Stripes event handler method has been identified, this method
     * sets the corresponding JSPWiki request context for the WikiContext.
     * Because Stripes event handler methods correspond exactly one-to-one with
     * pre-3.0 JSPWiki requests contexts, we allows Stripes to identify the
     * handler method first, then make sure the WikiContext's context is
     * synchronized. Also, if the ActionBean is of type ViewActionBean, the page
     * is set to the WikiEngine's front page as the default. Finally, the
     * WikiEngine is examined to see if it is configured. If not, the redirected
     * to the Installer page.
     * 
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

        // If it's the VIEW context, set page to "front page" to be safe
        if( actionBean instanceof ViewActionBean )
        {
            WikiEngine engine = actionBean.getContext().getEngine();
            ((ViewActionBean) actionBean).setPage( engine.getFrontPage( null ) );
        }

        // Make sure the WikiEngine is configured
        r = checkConfiguration( context );
        if( r != null )
        {
            return r;
        }
        return null;
    }

    /**
     * After the {@link LifecycleStage#ActionBeanResolution} stage fires, this
     * method intercepts all requests and redirects to the
     * {@link org.apache.wiki.action.InstallActionBean} if the WikiEngine has
     * not been configured yet.
     */
    protected Resolution checkConfiguration( ExecutionContext context ) throws Exception
    {
        // If already configured, exit quickly.
        ActionBean actionBean = context.getActionBean();
        if( m_isConfigured )
        {
            // Force login as admin when installer requested after previous install
            if ( actionBean instanceof InstallActionBean )
            {
                return new RedirectResolution( LoginActionBean.class );
            }
            return null;
        }

        // Is the wiki is already being installed, don't interrupt.
        if( actionBean instanceof InstallActionBean )
        {
            return null;
        }

        // If base URL and admin password not set, redirect to Installer
        ServletContext servletContext = context.getActionBeanContext().getServletContext();
        WikiEngine engine = WikiEngine.getInstance( servletContext, null );
        Properties props = engine.getWikiProperties();
        boolean hasBaseUrl = isPropertySet( WikiEngine.PROP_BASEURL, props );
        boolean hasAdminPassword = isPropertySet( InstallActionBean.PROP_ADMIN_PASSWORD_HASH, props );
        if( hasBaseUrl && hasAdminPassword )
        {
            m_isConfigured = true;
        }
        else
        {
            return new RedirectResolution( InstallActionBean.class );
        }

        // These are not the droids you were looking for
        return null;
    }

    private boolean isPropertySet( String name, Properties props )
    {
        String value = props.getProperty( name );
        return value != null && value.length() > 0;
    }

    /**
     * After the Stripes
     * {@link net.sourceforge.stripes.controller.LifecycleStage#ActionBeanResolution}
     * executes, this method runs the login stack to check for user authentication,
     * injects the current WikiActionBean into request scope, and returns
     * <code>null</code>. After the action bean is injected, downstream classes
     * like WikiTagBase can use it. The attribute can also be accessed as variables
     * using the JSP Expression Language (example: <code>${wikiActionBean}</code>).
     * User preferences are also set up.
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

        // If no WikiSession, we have a problem
        if( actionBeanContext.getWikiSession() == null )
        {
            throw new WikiException( "No WikiSession found!" );
        }

        // Run the login stack
        WikiEngine engine = actionBeanContext.getEngine();
        engine.getAuthenticationManager().login( request );

        // Stash the WikiActionBean
        request.setAttribute( ATTR_ACTIONBEAN, actionBean );

        // Stash the ActionBean as a PageContext attribute too
        PageContext pageContext = DispatcherHelper.getPageContext();
        if( pageContext != null )
        {
            pageContext.setAttribute( ATTR_ACTIONBEAN, actionBean );
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
     * interceptors, checks for validation errors generated by AJAX event methods,
     * and then checks for proper access to the current ActionBean and
     * target event.</p>
     * <p>
     * To ensure that AJAX-related validation errors are handled correctly, if the
     * current event method is annotated with {@link org.apache.wiki.ui.stripes.AjaxEvent},
     * an {@link org.apache.wiki.ui.stripes.EventResolution} will be returned
     * if one or more validation errors were generated earlier in the lifecycle.
     * </p>
     * <p>
     * To determine if the user is allowed to access the target event method,
     * the method is examined to see if contains a
     * {@link org.apache.wiki.ui.stripes.HandlerPermission}) annotation that
     * specifies the required {@link java.security.Permission}. If the user does
     * not possess the Permission -- that is,
     * {@link org.apache.wiki.auth.AuthorizationManager#checkPermission(WikiSession, Permission)}
     * returns <code>false</code> -- this method returns a RedirectResolution to
     * the login page, with all current parameters appended.
     * </p>
     * <p>Both the AJAX-error and access-checking logic run after after the
     * rest of the BindingAndValidation processing logic does, after which
     * point Stripes will have already discovered the correct ActionBean,
     * and bound and validated its request parameters.
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
        // Did the handler resolution stage return a Resolution? If so, bail.
        Resolution r = context.proceed();
        if( r != null )
        {
            return r;
        }

        // If handler is AJAX method, check for validation errors (and bail if there were any)
        ActionBeanContext actionBeanContext = context.getActionBeanContext();
        ValidationErrors errors = actionBeanContext.getValidationErrors();
        Method handler = context.getHandler();
        if ( handler.getAnnotation( AjaxEvent.class ) != null && errors.size() > 0 )
        {
            return new EventResolution( actionBeanContext );
        }

        // Get the event handler method
        WikiActionBean actionBean = (WikiActionBean) context.getActionBean();
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
