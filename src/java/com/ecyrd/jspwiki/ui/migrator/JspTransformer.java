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

package com.ecyrd.jspwiki.ui.migrator;

import java.util.Map;
import java.util.Set;

import net.sourceforge.stripes.action.ActionBean;

/**
 * Strategy interface for transforming JSPs.
 */
public interface JspTransformer
{
    /**
     * Initializes the transformer. This method should be called only once, when
     * the transformer is initialized.
     * @param migrator the JspMigrator initializing the transformer
     * @param beanClasses the Set of ActionBean classes discovered by
     * @param sharedState a Map containing key/value pairs that represent any
     *            shared-state information that this method might need during
     *            transformation.
     */
    public void initialize( JspMigrator migrator, Set<Class<? extends ActionBean>> beanClasses, Map<String, Object> sharedState );

    /**
     * Executes the transformation on the JSP and returns the result. This
     * method is called for each File migrated.
     * 
     * @param sharedState a map containing key/value pairs that represent any
     *            shared-state information that this method might need during
     *            transformation.
     * @param doc the JSP to transform
     */
    public void transform( Map<String, Object> sharedState, JspDocument doc );
}
