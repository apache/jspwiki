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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.ExecutionContext;
import net.sourceforge.stripes.controller.Interceptor;
import net.sourceforge.stripes.controller.Intercepts;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.util.CryptoUtil;
import net.sourceforge.stripes.util.bean.NoSuchPropertyException;
import net.sourceforge.stripes.util.bean.PropertyExpression;
import net.sourceforge.stripes.util.bean.PropertyExpressionEvaluation;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.ValidationError;

import org.apache.commons.jrcs.diff.DifferentiationFailedException;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.action.WikiActionBean;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.content.inspect.*;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.tags.SpamProtectTag;

/**
 * <p>
 * In collaboration with {@link org.apache.wiki.tags.SpamProtectTag}, intercepts
 * an ActionBean after the {@link net.sourceforge.stripes.controller.LifecycleStage#CustomValidation} 
 * to determine whether an ActionBean contains spam. 
 * An ActionBean contains spam if an {@link Inspection} produces 
 * a {@link Topic#SPAM} score that falls lower than
 * the threshold configured for the WikiEngine (which is always a negative
 * number).
 * </p>
 * <p>
 * SpamInterceptor fires only when the targeted event handler method is
 * annotated with the {@link SpamProtect} annotation. It fires after the
 * {@link net.sourceforge.stripes.controller.LifecycleStage#CustomValidation} stage; that is, after
 * ActionBean and event handler resolution, after parameter binding, and after
 * other all standard and custom validation routines have run. This ensures that
 * if the content needs to be inspected to see if the user needs to complete a
 * Challenge (such as a CAPTCHA), the ActionBean can be stashed away
 * <em>and</em> all of its parameters are guaranteed to be valid.
 * </p>
 */
@Intercepts( { LifecycleStage.EventHandling } )
public class SpamInterceptor implements Interceptor
{
    /**
     * Request parameter injected by {@link SpamProtectTag} and verified by
     * {@link #intercept(ExecutionContext)}.
     */
    public static final String CHALLENGE_REQUEST_PARAM = "_cn";

    /**
     * Key name of global {@link ValidationError}, which is added if
     * {@link #intercept(ExecutionContext)} determines that the ActionBean
     * contains spam.
     */
    public static final String SPAM_VALIDATION_ERROR = "validation.challenge.required";

    private static final Logger log = LoggerFactory.getLogger( SpamInterceptor.class );

    /**
     * Introspects an ActionBean and returns the value for one or more supplied
     * properties. Any properties not found will be cheerfully ignored.
     * 
     * @param actionBean the actionBean to inspect
     * @param beanProperties the bean properties to examine
     * @return the values if successfully evaluated, or <code>null</code> if not
     *         (or not set)
     */
    protected static Map<String, Object> getBeanProperties( WikiActionBean actionBean, String[] beanProperties )
    {
        Map<String, Object> map = new HashMap<String, Object>();
        for( String beanProperty : beanProperties )
        {
            try
            {
                PropertyExpression propExpression = PropertyExpression.getExpression( beanProperty );
                PropertyExpressionEvaluation evaluation = new PropertyExpressionEvaluation( propExpression, actionBean );
                Object value = evaluation.getValue();
                {
                    if( value == null )
                    {
                        value = "";
                    }
                    map.put( beanProperty, value );
                }
            }
            catch( NoSuchPropertyException e )
            {
                // Ignore any missing properties
            }
        }
        return map;
    }

    /**
     * Validates spam parameters contained in any requests targeting an
     * ActionBean method annotated with the {@link SpamProtect} annotation. This
     * creates a new {@link Inspection} that inspects each ActionBean property
     * indicated by the {@link SpamProtect#content()}. The
     * {@link InspectionPlan} for the Inspection is obtained by calling
     * {@link SpamInspectionFactory#getInspectionPlan(WikiEngine, java.util.Properties)}
     * . If any of the modifications are determined to be spam, a Stripes
     * {@link ValidationError} is added to the ActionBeanContext.
     * 
     * @return always returns {@code null}
     */
    public Resolution intercept( ExecutionContext context ) throws Exception
    {
        // If handler not protected by @SpamProtect, bail/execute next
        // interceptors in the chain
        HandlerInfo eventInfo = getHandlerInfo( context );
        if( !eventInfo.isSpamProtected() )
        {
            return context.proceed();
        }

        // Inspect the ActionBean contents for spam
        WikiActionBean actionBean = (WikiActionBean) context.getActionBean();
        boolean isSpam = false;

        switch( getChallengeRequest( actionBean.getContext() ) )
        {
            case CAPTCHA_ON_DEMAND: {
                // First-time submission; no challenge was requested
                isSpam = checkForSpam( actionBean, eventInfo );
                break;
            }

            case PASSWORD: {
                // Password challenge was requested
                // Not implemented yet
                checkForSpam( actionBean, eventInfo );
                break;
            }

            case CAPTCHA: {
                // CAPTCHA challenge was requested
                checkForSpam( actionBean, eventInfo );
                WikiEngine engine = actionBean.getContext().getEngine();
                Challenge captcha = SpamInspectionFactory.getCaptcha( engine );
                isSpam = !captcha.check( actionBean.getContext() );
                break;
            }

            case OMITTED: {
                // The Challenge param wasn't there. Naughty user!
                checkForSpam( actionBean, eventInfo );
                isSpam = true;
                break;
            }
        }

        // If it's spam, add a ValidationError and redirect back to source page
        if( isSpam )
        {
            ValidationError error = new LocalizableError( SPAM_VALIDATION_ERROR );
            actionBean.getContext().getValidationErrors().addGlobalError( error );
            return actionBean.getContext().getSourcePageResolution();
        }

        // Execute next interceptors in the chain
        return context.proceed();
    }

    /**
     * Looks up and returns the HandlerInfo object that corresponds to the
     * current Stripes ExecutionContext, based on the ActionBean and event
     * handler method being executed.
     * 
     * @param context the Stripes {@link ExecutionContext} that supplies the
     *            ActionBean, and the targeted event handler method
     * @return the HandlerInfo for the current event handler method
     * @throws WikiException if the event handler method does not have an
     *             associated HandlerInfo object. This should not happen, and if
     *             it does, it indicates a classpath error or other
     *             mis-configuration.
     */
    private HandlerInfo getHandlerInfo( ExecutionContext context ) throws WikiException
    {
        // Get the event handler method
        Method handler = context.getHandler();

        // Find the HandlerInfo method
        WikiActionBean actionBean = (WikiActionBean) context.getActionBean();
        Map<Method, HandlerInfo> eventinfos = HandlerInfo.getHandlerInfoCollection( actionBean.getClass() );
        HandlerInfo eventInfo = eventinfos.get( handler );
        if( eventInfo == null )
        {
            String message = "Event handler method " + actionBean.getClass().getName() + "#" + handler.getName()
                             + " does not have an associated HandlerInfo object. This should not happen.";
            log.error( message );
            throw new WikiException( message );
        }
        return eventInfo;
    }

    /**
     * Inspects an ActionBean to determine if any of its fields contains spam.
     * An ActionBean is considered to be spam if an {@link Inspection} results
     * in a {@link Topic#SPAM} score that exceeds the threshold configured for
     * the WikiEngine.
     * 
     * @param actionBean the ActionBean whose contents should be checked
     * @param eventInfo the current event's handler metadata, which contains
     *            information about which fields should be inspected
     * @return {@code true} if the {@link Topic#SPAM} score for the inspection
     *         exceeds the spam threshold, or {@code false} otherwise
     * @throws DifferentiationFailedException if it is not possible to create a
     *             {@link Change} object showing what contents changed
     */
    protected boolean checkForSpam( WikiActionBean actionBean, HandlerInfo eventInfo ) throws DifferentiationFailedException
    {
        // Retrieve all of the bean fields named in the @SpamProtect annotation
        WikiActionBeanContext actionBeanContext = actionBean.getContext();
        WikiEngine engine = actionBeanContext.getEngine();
        InspectionPlan plan = SpamInspectionFactory.getInspectionPlan( engine, engine.getWikiProperties() );
        Map<String, Object> fieldValues = getBeanProperties( actionBean, eventInfo.getSpamProtectedFields() );

        // Create an Inspection for analyzing the bean's contents
        Inspection inspection = new Inspection( actionBeanContext, plan );
        float spamScoreLimit = SpamInspectionFactory.defaultSpamLimit( engine );
        SpamInspectionFactory.setSpamLimit( inspection, spamScoreLimit );

        // Let's get to it!
        List<Change> changes = new ArrayList<Change>();
        for( Map.Entry<String, Object> entry : fieldValues.entrySet() )
        {
            String name = entry.getKey();
            String value = entry.getValue().toString();
            Change change;
            if( "page".equals( name ) )
            {
                change = Change.getPageChange( actionBeanContext, value );
            }
            else
            {
                change = Change.getChange( name, value );
            }
            changes.add( change );
        }

        // Run the Inspection
        inspection.inspect( changes.toArray( new Change[changes.size()] ) );
        float spamScore = inspection.getScore( Topic.SPAM );

        // Does the spam score exceed the threshold?
        return spamScore <= spamScoreLimit;
    }

    /**
     * Retrieves the encrypted parameter {@link #CHALLENGE_REQUEST_PARAM} from
     * the HTTP request and returns its value. If the parameter is not found in
     * the request, we assume that the request was made by a spammer or other
     * naughty person, and the method returns {@link org.apache.wiki.content.inspect.Challenge.Request#OMITTED}.
     * Otherwise, the value of the parameter is decrypted and returned. This
     * method is guaranteed to return a non-{@code null} value.
     * 
     * @param actionBeanContext the action bean context
     * @return the Challenge that was requested, or
     *         {@link org.apache.wiki.content.inspect.Challenge.Request#OMITTED} if the parameter
     *         {@link #CHALLENGE_REQUEST_PARAM} was not found.
     */
    protected Challenge.Request getChallengeRequest( ActionBeanContext actionBeanContext )
    {
        HttpServletRequest request = actionBeanContext.getRequest();
        String encryptedCaptchaParam = request.getParameter( CHALLENGE_REQUEST_PARAM );
        if( encryptedCaptchaParam != null )
        {
            String captchaParam = CryptoUtil.decrypt( encryptedCaptchaParam );
            if( captchaParam != null )
            {
                try
                {
                    Challenge.Request challenge = Challenge.Request.valueOf( captchaParam );
                    return challenge;
                }
                catch( IllegalArgumentException e )
                {
                    // The decrypted Challenge.Request was a funny value
                }
            }
        }

        // Challenge param not found or funny value: assume it's a spammer
        return Challenge.Request.OMITTED;
    }
}
