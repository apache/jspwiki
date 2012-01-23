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

package org.apache.wiki.action;

import org.apache.wiki.ui.stripes.WikiActionBeanContext;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;

/**
 * ActionBean sub-interface.
 *
 */
public interface WikiActionBean extends ActionBean
{
    /**
     * Returns the ActionBeanContext for the WikiActionBean, using a co-variant
     * return type of WikiActionBeanContext. 
     */
    public WikiActionBeanContext getContext();

    /**
     * Sets the WikiActionBeanContext for the ActionBean. This method <em>should</em>
     * be called immediately after bean creation.
     */
    public void setContext(ActionBeanContext context);
}
