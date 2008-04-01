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
package com.ecyrd.jspwiki.ui.admin.beans;

import javax.management.NotCompliantMBeanException;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.ui.admin.SimpleAdminBean;

/**
 *  This class is still experimental.
 *
 *
 */
public class WikiWizardAdminBean
    extends SimpleAdminBean
{
    private static final String[] ATTRIBUTES = {};
    private static final String[] METHODS    = {};

    public WikiWizardAdminBean() throws NotCompliantMBeanException
    {
    }


    public String getTitle()
    {
        return "WikiWizard";
    }

    public int getType()
    {
        return EDITOR;
    }

    public boolean isEnabled()
    {
        return true;
    }

    public String getId()
    {
        return "editor.wikiwizard";
    }

    public String[] getAttributeNames()
    {
        return ATTRIBUTES;
    }

    public String[] getMethodNames()
    {
        return METHODS;
    }

    public String doGet()
    {
        return "(C) i3G Institut Hochschule Heilbronn";
    }

    public void initialize(WikiEngine engine)
    {
        // TODO Auto-generated method stub

    }
}
