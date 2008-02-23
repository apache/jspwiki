/**
 * 
 */
package com.ecyrd.jspwiki.action;

import java.lang.annotation.*;

/**
 * WikiActionBean annotation that specifies the request context for an ActionBean (e.g., <code>edit</code>).
 * @author Andrew Jaquith
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Inherited
public @interface WikiRequestContext
{
    /**
     * The request context for looking up an ActionBean.
     * If not supplied and the class is a subclass of {@link AbstractActionBean}
     * the superclass default value will be inherited ("none").
     */
    String value();
}
