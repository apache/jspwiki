package org.apache.wiki.ui.stripes;

import java.lang.annotation.*;

/**
 * Method-level annotation indicating that an event is an AJAX event. Any
 * validation errors will be intercepted after the
 * {@link net.sourceforge.stripes.controller.LifecycleStage#CustomValidation}
 * stage and returned as an {@link org.apache.wiki.ui.stripes.EventResolution}.
 */
@Documented
@Inherited
@Retention( value = RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD } )
public @interface AjaxEvent
{
}
