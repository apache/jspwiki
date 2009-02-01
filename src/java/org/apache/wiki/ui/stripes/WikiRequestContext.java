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

/**
 * 
 */
package org.apache.wiki.ui.stripes;

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
