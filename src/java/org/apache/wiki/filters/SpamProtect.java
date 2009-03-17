/**
 * 
 */
package org.apache.wiki.filters;

import java.lang.annotation.*;

/**
 * Annotation indicating that an event handler method should check that the user
 * has submitted a series of expected {@link SpamFilter}-related parameters
 * with the POST or GET. The SpamProtect annotation can be applied to either
 * class or method targets. If the annotation applies to a Stripes event handler
 * method, the {@link SpamInterceptor} will apply spam filtering heuristics to
 * just the annotated event. If the annotation applies to a class, all event
 * handler methods will be filtered by default. Method-level annotations always
 * override class-level annotations.
 */
@Documented
@Inherited
@Retention( value = RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD, ElementType.TYPE } )
public @interface SpamProtect
{
}
