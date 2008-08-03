/**
 * 
 */
package com.ecyrd.jspwiki.action;

import java.lang.annotation.*;
import java.security.Permission;

/**
 * <p>Permission annotation that specifies the the class, target and actions of a
 * Permission required to execute a Stripes event handler method. Annotations are
 * specified as follows:</p>
 * <blockquote>
 * <code>
 * &#64;HandlerPermission(permissionClass="<var>className</var>",target="<var>target</var>",actions="<var>actions</var>")
 * </code>
 * </blockquote>
 * </p>
 * <p>The target and actions can be expression language (EL) expressions;
 * if so, they must start with <code>${</code> and end with <code>}</code>.
 * The expression is evaluated with the ActionBean as the "root." Thus, the expression
 * <code>${foo}</code> evaluates to the value of the bean's <code>foo</code>
 * property, typically as returned by an accessor such as <code>getFoo()</code>.
 *  Mapped properties can also be used. For example, if the <code>Map</code>
 *  property's accessor method <code>getBarMap()</code> returns a Map, the expression
 * <code>${barMap['Alice']}</code> evaluates to the Map value whose key is
 * <code>Alice</code>. <em>EL expressions that do not evaluate to valid values
 * should be discarded by callers.</em></p>
 * <p>When the target or actions parameter values are <em>not</em> EL expressions, they
 * are interpreted as literals.</p>
 * <p>For example, the following are all valid annotations:</p>
 * <blockquote>
 * <code>
 * &#64;HandlerPermission(permissionClass="java.io.FilePermission", target="/tmp/-", actions="read,write")
 * <br/><br/>
 * &#64;HandlerPermission(permissionClass="com.ecyrd.jspwiki.auth.permissions.PagePermission", target="${page.name}", actions = "edit")
 * <br/><br/>
 * &#64;HandlerPermission(permissionClass="com.ecyrd.jspwiki.auth.permissions.GroupPermission", target="${context.request.parameterMap['group'][0]}",actions="view")
 * </code>
 * </blockquote>
 * <p>These examples assume that the ActionBeans they annotate contain the appropriate properties;
 * in this case, <code>page</code> is assumed to be a {@link com.ecyrd.jspwiki.WikiPage} property,
 * and <code>context</code> is assumed to be a {@link com.ecyrd.jspwiki.action.WikiActionBeanContext}
 * property.</p>
 * <p>This Annotation class does not parse, process or instantiate Permissions; it merely specifies
 * the syntax for annotating event handler methods. The collaborating class {@link HandlerInfo}
 * actually does the heavy lifting.</p>
 * @author Andrew Jaquith
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD })
@Documented
@Inherited
public @interface HandlerPermission
{
    public static final String BLANK = "";
    
    /**
     * The class of the Permission.
     */
    Class<? extends Permission> permissionClass();

    /**
     * The Permission target, supplied as a static String or as an EL
     * expression.
     * 
     * @return the target
     */
    String target() default BLANK;

    /**
     * The Permission actions, supplied as a static String or as an EL
     * expression.
     * 
     * @return the actions
     */
    String actions() default BLANK;
}
