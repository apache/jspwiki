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
