/*
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
package org.apache.wiki.ui.admin.beans;

import org.apache.wiki.Release;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.ui.admin.SimpleAdminBean;

import javax.management.NotCompliantMBeanException;


/**
 *  An AdminBean which manages the JSPWiki core operations.
 */
public class CoreBean extends SimpleAdminBean {

    private static final String[] ATTRIBUTES = { "pages", "version" };
    private static final String[] METHODS = { };

    public CoreBean( final Engine engine ) throws NotCompliantMBeanException {
        m_engine = engine;
    }

    /**
     *  Return the page count in the Wiki.
     *
     *  @return the page content
     */
    public int getPages() {
        return m_engine.getManager( PageManager.class ).getTotalPageCount();
    }

    public String getPagesDescription()
    {
        return "The total number of pages in this wiki";
    }

    public String getVersion()
    {
        return Release.VERSTR;
    }

    public String getVersionDescription()
    {
        return "The JSPWiki engine version";
    }

    @Override public String getTitle()
    {
        return "Core bean";
    }

    @Override public int getType()
    {
        return CORE;
    }

    @Override public String getId()
    {
        return "corebean";
    }

    @Override public String[] getAttributeNames()
    {
        return ATTRIBUTES;
    }

    @Override public String[] getMethodNames()
    {
        return METHODS;
    }

}
