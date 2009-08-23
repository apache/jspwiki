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

import net.sourceforge.stripes.action.ActionBeanContext;


/**
 * <p>
 * Abstract ActionBean superclass for all wiki actions, such as page actions ({@link org.apache.wiki.WikiContext}
 * and subclasses), group actions (e.g., {@link GroupActionBean}), user
 * actions (e.g., {@link UserPreferencesActionBean}) and others.
 * </p>
 * 
 */
abstract class AbstractActionBean implements WikiActionBean
{
    private WikiActionBeanContext m_context = null;

    /**
     * Creates a new instance of this class, without a WikiEngine, Request or
     * WikiPage.
     */
    protected AbstractActionBean()
    {
        super();
    }

    /**
     * Returns the Stripes ActionBeanContext associated this WikiContext. This
     * method may return <code>null</code>, and callers should check for this
     * condition.
     * 
     * @throws IllegalStateException
     */
    public final WikiActionBeanContext getContext()
    {
        return m_context;
    }

    /**
     * Sets the Stripes ActionBeanContext associated with this WikiContext. It
     * will also update the cached HttpRequest.
     */
    public final void setContext( ActionBeanContext context )
    {
        m_context = ((WikiActionBeanContext) context);
    }

}
