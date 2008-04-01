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
package com.ecyrd.jspwiki;

import java.util.Enumeration;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

/**
 * Mock servlet filter configuration. This is a temporary class and will go away in JSPWiki 3.0.
 * @author Andrew Jaquith
 * @deprecated
 */
public class TestFilterConfig implements FilterConfig
{
    private ServletContext m_servletContext;

    public TestFilterConfig(ServletContext servletContext)
    {
        m_servletContext = servletContext;
    }
    
    public String getFilterName()
    {
        return "Mock servlet config";
    }

    /**
     * Returns null for all values.
     */
    public String getInitParameter(String arg0)
    {
        return null;
    }

    /**
     * Returns null, for now.
     */
    public Enumeration getInitParameterNames()
    {
        return null;
    }

    /**
     * Returns the servlet context used to create this config.
     */
    public ServletContext getServletContext()
    {
        return m_servletContext;
    }

}
