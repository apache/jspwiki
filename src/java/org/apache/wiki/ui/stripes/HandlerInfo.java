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

import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.el.ELException;

import org.apache.wiki.action.WikiActionBean;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.util.bean.PropertyExpression;
import net.sourceforge.stripes.util.bean.PropertyExpressionEvaluation;


/**
 * Extracts {@link HandlerPermission} annotation metadata for a supplied
 * ActionBean class using introspection, and allows instantiation of dynamic
 * Permissions. This class caches HandlerInfo objects for each ActionBean class,
 * so introspection operations only happen once per class.
 * 
 * @author Andrew Jaquith
 */
public class HandlerInfo
{
    private static final Map<Class<? extends WikiActionBean>, Map<Method, HandlerInfo>> CACHED_INFO = new HashMap<Class<? extends WikiActionBean>, Map<Method, HandlerInfo>>();

    private final Class<? extends WikiActionBean> m_beanClass;

    private final Method m_handlerMethod;

    private final Class<? extends Permission> m_permissionClass;

    private final String m_permissionTarget;

    private final String m_permissionActions;

    private final String m_handlerName;

    private final PropertyExpression m_permissionTargetExpression;

    private final PropertyExpression m_permissionActionExpression;

    private final String m_requestContext;

    /**
     * Private constructor that identifies relevant Permission and wiki request
     * context information for a supplied WikiActionBean's event method. The
     * supplied event method must have previously been determined to have a
     * {@link HandlesEvent} annotation. This constructor also looks for a
     * {@link HandlerPermission} annotation to determine the correct Permission
     * needed to execute the event (if supplied). Finally, attempts to determine
     * the name of the wiki request context by looking for a
     * {@link WikiRequestContext} annotation. If the event handler method does
     * not contain a WikiRequestContext annotation, a default wiki request
     * context will be assigned, based on the ActionBean name plus "." plus the
     * method name.
     * 
     * @param beanClass the ActionBean implementation for which event
     *            information should be created
     * @param method the method that denotes the event handler
     * @param eventHandler the name of the event the method handles
     */
    private HandlerInfo( Class<? extends WikiActionBean> beanClass, Method method, String eventHandler )
    {
        // Determine the permission annotated by @HandlerPermission (if
        // supplied)
        Class<? extends Permission> permClass = null;
        String target = null;
        String actions = null;
        HandlerPermission permAnnotation = method.getAnnotation( HandlerPermission.class );
        if( permAnnotation != null )
        {
            permClass = permAnnotation.permissionClass();
            target = permAnnotation.target();
            actions = permAnnotation.actions();

            // If the class or target are null, this is an error (a
            // mistake by some developer or a classloader problem)
            if( permClass == null || target == null )
            {
                throw new AnnotationFormatError( "Malformed annotation: " + method.getClass().getName() + "." + method.getName() );
            }
        }

        m_beanClass = beanClass;
        m_handlerMethod = method;
        m_permissionClass = permClass;
        m_permissionTarget = (target == null || HandlerPermission.BLANK.equals( target )) ? null : target;
        m_permissionActions = (actions == null || HandlerPermission.BLANK.equals( actions )) ? null : actions;

        // Parse the expressions for the target and actions, if supplied as such
        if( target != null && target.startsWith( "${" ) && target.endsWith( "}" ) )
        {
            m_permissionTargetExpression = PropertyExpression.getExpression( target.substring( 2, target.length() - 1 ) );
        }
        else
        {
            m_permissionTargetExpression = null;
        }
        if( actions != null && actions.startsWith( "${" ) && actions.endsWith( "}" ) )
        {
            m_permissionActionExpression = PropertyExpression.getExpression( m_permissionTarget.substring( 2, m_permissionTarget
                .length() - 1 ) );
        }
        else
        {
            m_permissionActionExpression = null;
        }

        // Identify the wiki request context identified by @WikiRequestContext
        // (if supplied)
        WikiRequestContext requestContext = method.getAnnotation( WikiRequestContext.class );
        String defaultRequestContext = m_beanClass.getName() + "." + eventHandler;
        m_requestContext = requestContext != null ? requestContext.value() : defaultRequestContext;

        // Store the Stripes event handler name
        m_handlerName = eventHandler;
    }

    protected String getPermissionActions()
    {
        return m_permissionActions;
    }

    protected Class<? extends Permission> getPermissionClass()
    {
        return m_permissionClass;
    }

    protected String getPermissionTarget()
    {
        return m_permissionTarget;
    }

    /**
     * Returns the property expression for the target, if it specifies a valid
     * expression, or <code>null</code> otherwise.
     * 
     * @return the property expression
     */
    protected PropertyExpression getPermissionTargetExpression()
    {
        return m_permissionTargetExpression;
    }

    /**
     * Returns the property expression for the actions, if it specifies a valid
     * expression, or <code>null</code> otherwise.
     * 
     * @return the property expression
     */
    protected PropertyExpression getActionsExpression()
    {
        return m_permissionActionExpression;
    }

    /**
     * Returns the HandlerInfo object associated with the default Stripes event
     * handler method for a supplied class. All Stripes ActionBeans (and JSPWiki
     * WikiActionBeans, by definition) must contain a method with the
     * {@link net.sourceforge.stripes.action.DefaultHandler} annotation.
     * 
     * @param beanClass the ActionBean subclass to inspect
     * @return the event info object for the default handler method
     */
    public static final HandlerInfo getDefaultHandlerInfo( Class<? extends WikiActionBean> beanClass )
    {
        Map<Method, HandlerInfo> eventInfoCollection = CACHED_INFO.get( beanClass );
        if( eventInfoCollection == null )
        {
            throw new IllegalArgumentException( "Bean class " + beanClass.getCanonicalName() + " does not have cached event info." );
        }

        // Find the first @DefaultHandler annotation
        for( Map.Entry<Method, HandlerInfo> methodEvent : eventInfoCollection.entrySet() )
        {
            Method method = methodEvent.getKey();
            HandlerInfo handlerInfo = methodEvent.getValue();
            if( method.getAnnotation( DefaultHandler.class ) != null )
            {
                return handlerInfo;
            }
        }
        throw new IllegalArgumentException( "Bean class " + beanClass.getCanonicalName() + " has no @DefaultHandler!" );
    }

    /**
     * Looks up and returns the HandlerInfo for a supplied WikiActionBean class
     * and event handler name. The supplied WikiActionBean class must contain a
     * Stripes event handler method whose {@link HandlesEvent} annotation value
     * matches the <code>eventHandler</code> parameter value.
     * 
     * @param eventHandler the Stripes ActionBean method to inspect
     * @return the event info object for the handler method
     */
    public static final HandlerInfo getHandlerInfo( Class<? extends WikiActionBean> beanClass, String eventHandler )
    {
        Collection<HandlerInfo> handlerInfos = getHandlerInfoCollection( beanClass ).values();
        for( HandlerInfo handlerInfo : handlerInfos )
        {
            if( eventHandler.equals( handlerInfo.getEventName() ) )
            {
                return handlerInfo;
            }
        }
        throw new IllegalArgumentException( "ActionBean=" + beanClass.getCanonicalName() + ", handler=" + eventHandler
                                            + " has no HandlerInfo!" );
    }

    /**
     * For a supplied ActionBean class, returns a Map with the Stripes event
     * handler methods as keys, and HandlerInfo objects as values.
     * 
     * @param beanClass the ActionBean subclass to inspect
     * @return the map
     */
    public static final Map<Method, HandlerInfo> getHandlerInfoCollection( Class<? extends WikiActionBean> beanClass )
    {
        // If we've already figured out the method info, return the cached Map
        Map<Method, HandlerInfo> eventInfoCollection = CACHED_INFO.get( beanClass );
        if( eventInfoCollection != null )
        {
            return eventInfoCollection;
        }

        // Not there, eh? Ok, let's figure it out.
        eventInfoCollection = new HashMap<Method, HandlerInfo>();
        CACHED_INFO.put( beanClass, eventInfoCollection );

        Method[] methods = beanClass.getMethods();
        for( int i = 0; i < methods.length; i++ )
        {
            // Does the method have a @HandlesEvent annotation?
            Method method = methods[i];
            HandlesEvent eventAnnotation = method.getAnnotation( HandlesEvent.class );
            if( eventAnnotation != null )
            {
                // Create a new event info object
                HandlerInfo handlerInfo = new HandlerInfo( beanClass, method, eventAnnotation.value() );
                eventInfoCollection.put( method, handlerInfo );
            }
        }
        return eventInfoCollection;
    }

    /**
     * Returns the WikiActionBean class that is the parent of the event method
     * used to instantiate the HandlerInfo object.
     * 
     * @return the WikiActionBean class
     */
    public Class<? extends WikiActionBean> getActionBeanClass()
    {
        return m_beanClass;
    }

    /**
     * Returns the Method associated with the event handler. This is the same
     * method used to instantiate the HandlerInfo object in the private
     * constructor {@link #HandlerInfo(Class, Method, String)}.
     * 
     * @return the method
     */
    public Method getHandlerMethod()
    {
        return m_handlerMethod;
    }

    /**
     * Returns the name of the event, which equal to the value of the handler
     * method's {@link HandlesEvent} annotation.
     * 
     * @return the event name
     */
    public String getEventName()
    {
        return m_handlerName;
    }

    /**
     * <p>
     * Returns the request context associated with this HandlerInfo object. If
     * the event handler method contains an additional
     * {@link WikiRequestContext} annotation, the annotation value will be used.
     * Otherwise, a default wiki request context will be returned, based on the
     * WikiActionBean's class name plus the event name, separated by ".".
     * </p>
     * <p>
     * For example, consider the two event handler methods in FooActionBean
     * below:
     * </p>
     * 
     * <pre>
     * public class FooActionBean extends WikiActionBean
     * {
     *     &#064;WikiRequestContext(&quot;view&quot;)
     *     &#064;HandlesEvent(&quot;view&quot;)
     *     public void viewBean() { ... }
     *     
     *     &#064;HandlesEvent(&quot;save&quot;)
     *     public void saveBean() { ... }
     * }
     * </pre>
     * 
     * <p>
     * Because it contains a {@link WikiRequestContext} annotation, calling
     * <code>getRequestContext( FooActionBean.class, "view" )</code> returns
     * the value <code>view</code>. By contrast, calling
     * <code>getRequestContext( FooActionBean.class, "save" )</code> returns
     * the value <code>FooActionBean.save</code>. And because there is no
     * matching {@link HandlesEvent} annotation for "edit." calling
     * <code>getRequestContext( FooActionBean.class, "edit" )</code> throws an
     * {@link IllegalArgumentException}.
     * </p>
     * 
     * @return the wiki request context
     */
    public String getRequestContext()
    {
        return m_requestContext;
    }

    /**
     * <p>
     * Returns a dynamic Permission based on the combination of the handler
     * method's {@link HandlerPermission} annotation and a supplied object,
     * which is evaluated to populate the Permission target and/or actions. The
     * object supplied must be an object with standard bean property accessors
     * and mutators (get/set methods). Any EL expressions found in the
     * HandlerPermission's target or actions are evaluated against the object.
     * Note that this method returns <code>null</code> if no HandlerPermission
     * annotation was supplied for the handler method;
     * <em>callers should check for nulls</em>.
     * </p>
     * <p>
     * For example, suppose the HandlerPermission annotation for the
     * <code>view()</code> handler method is
     * </p>
     * <blockquote><code>&#064;HandlerPermission(permissionClass=PagePermission.class, target="${page.qualifiedName}", actions=PagePermission.VIEW_ACTION)</code></blockquote>
     * <p>
     * If <code>object</code> is a ViewActionBean whose <code>getPage()</code>
     * property returns page "Main" in the wiki named "Default", the returned
     * Permission will be:
     * </p>
     * <blockquote><code>PagePermission "Default:Main", "view"</code></blockquote>
     * 
     * @param object the Object that will be used as the base for any EL
     *            expressions
     * @return the resolved and instantiated Permission
     * @throws ELException if EL expressions cannot be parsed, or of the
     *             Permission itself cannot be instantiated for any reason
     */
    public Permission getPermission( Object object ) throws ELException
    {
        if( m_permissionClass == null )
        {
            return null;
        }

        // Get the target class, target and actions
        boolean hasOneParameter = m_permissionActions == null;
        String target = m_permissionTarget;
        String actions = m_permissionActions;

        // Evaluate the target, if it's an expression
        if( m_permissionTargetExpression != null )
        {
            PropertyExpressionEvaluation evaluation = new PropertyExpressionEvaluation( m_permissionTargetExpression, object );
            Object value = evaluation.getValue();
            if ( value != null )
            {
                target = value.toString();
            }
            if( target == null )
            {
                // If the target didn't evaluate, assume it's because some
                // property wasn't set (probably normal)
                // FIXME: should we throw a checked exception instead?
                return null;
            }
        }

        // Evaluate the actions, if it's an expression
        if( m_permissionActionExpression != null )
        {
            PropertyExpressionEvaluation evaluation = new PropertyExpressionEvaluation( m_permissionActionExpression, object );
            actions = (String) evaluation.getValue();
            if( actions == null )
            {
                throw new ELException( "Actions expression '${" + m_permissionTargetExpression + "} ' returned null!" );
            }
        }

        // Instantiate the permission!
        Permission perm = null;

        // Filthy hack to use caching for Permission classes we know about
        if( PagePermission.class.isAssignableFrom( m_permissionClass ) )
        {
            return PermissionFactory.getPagePermission( target, actions );
        }

        // Otherwise, create Permission manually
        try
        {
            if( hasOneParameter )
            {
                Constructor<?> c = m_permissionClass.getConstructor( new Class[] { String.class } );
                perm = (Permission) c.newInstance( new Object[] { target } );
            }
            else
            {
                Constructor<?> c = m_permissionClass.getConstructor( new Class[] { String.class, String.class } );
                perm = (Permission) c.newInstance( new Object[] { target, actions } );
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
            throw new ELException( "Could not evaluate permission info: " + e.getMessage() );
        }

        return perm;
    }

}
