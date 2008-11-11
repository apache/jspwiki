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
package org.apache.jspwiki.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *  This annotation allows you to annotate your plugin in such a way
 *  that JSPWiki can locate some extra information about it.
 *  <p>
 *  This annotation replaces the old jspwiki_modules.xml file - it
 *  is far better to be able to annotate the class file directly
 *  than to put the stuff in a separate XML file.
 *  <p>
 *  JSPWiki will use Reflection to locate all WikiPlugin classes,
 *  so the jspwiki_module.xml is not strictly speaking needed. 
 *  
 *  @since 3.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleData
{
    /** The author of this module. */
    String author()         default "AnonymousCoward";
    
    /** The minimum version of JSPWiki that this module will work with.  This will be
     *  checked when the module is loaded.
     */
    String minVersion()     default "0.0";
    
    /** The maximum version of JSPWiki that this module will work with. */
    String maxVersion()     default "1000000.0";
    
    /** The minimum version of the JSPWiki API that this module will work with.
     *  Note the difference - you can create a dependency between either a particular
     *  JSPWiki version, or a particular JSPWiki API version.
     *  <p>
     *  Notice that there is no maxAPIVersion(), because JSPWiki API is supposed
     *  to be completely backwards compatible all the time. (Yeah right.)
     */
    String minAPIVersion()  default "0.0";
    
    /**
     *  Defines the style sheets which should be included whenever this module
     *  is used.  For PageFilters this means almost every single request.
     */
    String[] stylesheets()  default "";
    
    /**  
     *  Defines the Javascripts which should be included whenever this module
     *  is used.
     */
    String[] scripts()      default "";
    
    /**
     *  Returns the class name for the AdminBean which governs the use of this
     *  class.
     */
    String adminBeanClass() default "";
    
    /**
     *  Defines the different aliases which can also be used to access this
     *  module.  This allows you to define a "shorter name" for the module
     *  to be used in e.g. macros.
     */
    String[] aliases()      default "";
}
