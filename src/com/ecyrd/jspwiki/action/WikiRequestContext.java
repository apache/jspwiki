/**
 * 
 */
package com.ecyrd.jspwiki.action;

import java.lang.annotation.*;

import net.sourceforge.stripes.action.HandlesEvent;

/**
 * WikiActionBean method annotation that maps a Stripes ActionBean event name to
 * a JSPWiki request context (e.g., <code>edit</code>). The method containing
 * the WikiRequestContext annotation must also have a {@link HandlesEvent}
 * annotation. The values returned by both annotations do not need to match.
 * 
 * @see HandlerInfo#getRequestContext()
 * @author Andrew Jaquith
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD } )
@Documented
@Inherited
public @interface WikiRequestContext
{
    /**
     * The request context associated with an ActionBean event handler method.
     */
    String value();
}
