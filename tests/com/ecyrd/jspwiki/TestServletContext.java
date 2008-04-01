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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Mock response object that stashes a WikiEngine object and returns it later.
 * This is a temporary class and will go away in JSPWiki 3.0.
 * @author Andrew Jaquith
 * @deprecated
 */
public class TestServletContext implements ServletContext
{
    private WikiEngine m_engine;
    
    public TestServletContext(WikiEngine engine)
    {
        super();
        m_engine = engine;
    }
    
    /**
     * If the attribute requested is for the magic WikiEngine, we return it; otherwise this always returns null.
     */
    public Object getAttribute(String arg0)
    {
        if ("com.ecyrd.jspwiki.WikiEngine".equals(arg0))
        {
            return m_engine;
        }
        return null;
    }

    public Enumeration getAttributeNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ServletContext getContext(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getInitParameter(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration getInitParameterNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getMajorVersion()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getMimeType(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getMinorVersion()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public RequestDispatcher getNamedDispatcher(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRealPath(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public RequestDispatcher getRequestDispatcher(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public URL getResource(String arg0) throws MalformedURLException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public InputStream getResourceAsStream(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Set getResourcePaths(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getServerInfo()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Servlet getServlet(String arg0) throws ServletException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getServletContextName()
    {
        return "Test servlet context";
    }

    public Enumeration getServletNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration getServlets()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void log(String arg0)
    {
        // TODO Auto-generated method stub

    }

    public void log(Exception arg0, String arg1)
    {
        // TODO Auto-generated method stub

    }

    public void log(String arg0, Throwable arg1)
    {
        // TODO Auto-generated method stub

    }

    public void removeAttribute(String arg0)
    {
        // TODO Auto-generated method stub

    }

    public void setAttribute(String arg0, Object arg1)
    {
        // TODO Auto-generated method stub

    }

}
