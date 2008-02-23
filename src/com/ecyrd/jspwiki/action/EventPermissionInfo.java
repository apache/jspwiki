package com.ecyrd.jspwiki.action;

import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.Permission;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.el.ELException;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.util.bean.PropertyExpression;
import net.sourceforge.stripes.util.bean.PropertyExpressionEvaluation;

import com.ecyrd.jspwiki.auth.permissions.GroupPermission;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.permissions.PermissionFactory;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

/**
 * Extracts {@link EventPermission} annotation metadata for a supplied ActionBean
 * class using introspection, and allows instantiation of dynamic Permissions.
 * This class caches EventPermissionInfo objects for each ActionBean class, so introspection operations
 * only happen once per class.
 * @author Andrew Jaquith
 */
public class EventPermissionInfo
{
    private static final Map<Class<? extends ActionBean>,Map<Method,EventPermissionInfo>> CACHED_INFO
        = new HashMap<Class<? extends ActionBean>,Map<Method,EventPermissionInfo>>();
    private final Class m_clazz;
    private final String m_target;
    private final String m_actions;
    private final PropertyExpression m_targetExpression;
    private final PropertyExpression m_actionExpression;
    
    private EventPermissionInfo(Class clazz, String target, String actions)
    {
        m_clazz = clazz;
        m_target = target.trim();
        m_actions = actions.trim();
        if (m_target.startsWith("${") && m_target.endsWith("}"))
        {
            m_targetExpression = PropertyExpression.getExpression(m_target.substring(2,m_target.length() -1));
        }
        else
        {
            m_targetExpression = null;
        }
        if (m_actions.startsWith("${") && m_actions.endsWith("}"))
        {
            m_actionExpression = PropertyExpression.getExpression(m_target.substring(2,m_target.length() -1));
        }
        else
        {
            m_actionExpression = null;
        }
    }
    
    public String getActions()
    {
        return m_actions;
    }

    public Class getPermissionClass()
    {
        return m_clazz;
    }

    public String getTarget()
    {
        return m_target;
    }

    /**
     * Returns the property expression for the target, if it specifies a valid expression, or <code>null</code> otherwise.
     * @return the property expression
     */
    public PropertyExpression getTargetExpression()
    {
        return m_targetExpression;
    }
    
    /**
     * Returns the property expression for the actions, if it specifies a valid expression, or <code>null</code> otherwise.
     * @return the property expression
     */
    public PropertyExpression getActionsExpression()
    {
        return m_actionExpression;
    }
    
    /**
     * For a supplied ActionBean class, returns a Map with the Stripes event handler methods as keys, and
     * EventPermissionInfo objects as values.
     * @param beanClass the ActionBean subclass to inspect
     * @return the map
     */
    public static final Map<Method,EventPermissionInfo> getEventPermissionInfo( Class<? extends ActionBean> beanClass)
    {
        // If we've already figured out the method info, return the cached Map
        Map<Method,EventPermissionInfo> methodInfo = CACHED_INFO.get(beanClass);
        if (methodInfo != null)
        {
            return methodInfo;
        }
        
        // Not there, eh? Ok, let's figure it out.
        methodInfo = new HashMap<Method,EventPermissionInfo>();
        CACHED_INFO.put(beanClass, methodInfo);
        
        Method[] methods = beanClass.getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            // Does the method have a @HandlesEvent annotation?
            Method method = methods[i];
            HandlesEvent eventAnnotation = method.getAnnotation(HandlesEvent.class);
            if (eventAnnotation != null)
            {
                // If yes, does it have an @EventHandlerPermission?
                EventPermission permAnnotation = method.getAnnotation(EventPermission.class);
                if ( permAnnotation != null)
                {
                    Class permClass = permAnnotation.permissionClass();
                    String target = permAnnotation.target();
                    String actions = permAnnotation.actions();
                    
                    // If the class or target are null, this is an error (a mistake by some developer or a classloader problem)
                    if ( permClass == null || target == null )
                    {
                        throw new AnnotationFormatError("Malformed annotation: " + beanClass.getName() + "." + method.getName());
                    }
                    
                    EventPermissionInfo info = new EventPermissionInfo(permClass, target, actions);
                    methodInfo.put(method,info);
                }
            }
        }
        return methodInfo;
    }
    
    /**
     * Creates a Permission based on the class, target and actions metadata, evaluating any EL expressions
     * found in the target or actions. In order for EL expressions to be evaluated, this method <em>must</em>
     * be called from inside an Interceptor (or other StripesFilter delegate) at runtime.
     * @param actionBean the ActionBean that will be used as the base for any EL expressions
     * @return the resolved and instantiated Permission
     * @throws ELException if EL expressions cannot be parsed, or of the Permission itself
     * cannot be instantiated for any reason
     */
    public Permission getPermission(WikiActionBean actionBean) throws ELException
    {
        // Get the target class, target and actions
        boolean hasOneParameter = m_actions == null;
        String target = m_target;
        String actions = m_actions;

        // Evaluate the target, if it's an expression
        if (m_targetExpression != null)
        {
            PropertyExpressionEvaluation evaluation = new PropertyExpressionEvaluation( m_targetExpression, actionBean );
            target = (String)evaluation.getValue();
            if ( target == null )
            {
                // If the target didn't evaluate, assume it's because some property wasn't set (probably normal)
                // FIXME: should we throw a checked exception instead?
                return null;
            }
        }

        // Evaluate the actions, if it's an expression
        if (m_actionExpression != null)
        {
            PropertyExpressionEvaluation evaluation = new PropertyExpressionEvaluation( m_actionExpression, actionBean );
            actions = (String)evaluation.getValue();
            if ( actions == null )
            {
                throw new ELException("Actions expression '${" + m_targetExpression + "} ' returned null!");
            }
        }

        // Instantiate the permission!
        Permission perm = null;
        
        // Filthy hack to use caching for Permission classes we know about
        if ( PagePermission.class.isAssignableFrom(m_clazz) )
        {
            return PermissionFactory.getPagePermission(target,actions);
        }
        if ( WikiPermission.class.isAssignableFrom(m_clazz) )
        {
            return PermissionFactory.getWikiPermission(target, actions);
        }
        if ( GroupPermission.class.isAssignableFrom(m_clazz) )
        {
            return PermissionFactory.getGroupPermission(target, actions);
        }
        
        try
        {
            if (hasOneParameter)
            {
                Constructor c = m_clazz.getConstructor(new Class[] { String.class });
                perm = (Permission) c.newInstance(new Object[] { target });
            }
            else
            {
                Constructor c = m_clazz.getConstructor(new Class[] { String.class, String.class });
                perm = (Permission) c.newInstance(new Object[] { target, actions });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ELException("Could not evaluate permission info: " + e.getMessage());
        }

        return perm;
    }

}
