/**
 * 
 */
package org.apache.wiki.ui.stripes;

import java.lang.annotation.*;

import org.apache.wiki.filters.SpamFilter;

/**
 * Annotation indicating that an event handler method should check that the user
 * has submitted a series of expected {@link SpamFilter}-related parameters
 * with the POST or GET. The SpamProtect annotation can be applied to
 * method targets. When annotating Stripes event handler
 * method, the {@link SpamInterceptor} will apply spam filtering heuristics to
 * the annotated event.
 */
@Documented
@Inherited
@Retention( value = RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD } )
public @interface SpamProtect
{
    /**
     * The names of the bean properties containing the content being protected.
     * This can be used, for example, to extract the protected content from
     * annotated classes' by introspection. By default, this is a zero-argument
     * array.
     */
    String[] content() default {};
}
