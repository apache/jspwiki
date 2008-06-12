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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * Mock response object that does absolutely nothing but allow cookies to be set.
 * This is a temporary class and will go away in JSPWiki 3.0.
 * @author Andrew Jaquith
 * @deprecated
 */
public class TestHttpServletResponse implements HttpServletResponse
{
    private Set<Cookie> m_cookies = new HashSet<Cookie>();
    
    public Cookie[] getCookies() {
        return m_cookies.toArray(new Cookie[m_cookies.size()]);
    }
    
    public void addCookie(Cookie arg0)
    {
        m_cookies.add(arg0);
    }

    public void addDateHeader(String arg0, long arg1)
    {
        // TODO Auto-generated method stub

    }

    public void addHeader(String arg0, String arg1)
    {
        // TODO Auto-generated method stub

    }

    public void addIntHeader(String arg0, int arg1)
    {
        // TODO Auto-generated method stub

    }

    public boolean containsHeader(String arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public String encodeRedirectURL(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String encodeRedirectUrl(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String encodeURL(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String encodeUrl(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void sendError(int arg0) throws IOException
    {
        // TODO Auto-generated method stub

    }

    public void sendError(int arg0, String arg1) throws IOException
    {
        // TODO Auto-generated method stub

    }

    public void sendRedirect(String arg0) throws IOException
    {
        // TODO Auto-generated method stub

    }

    public void setDateHeader(String arg0, long arg1)
    {
        // TODO Auto-generated method stub

    }

    public void setHeader(String arg0, String arg1)
    {
        // TODO Auto-generated method stub

    }

    public void setIntHeader(String arg0, int arg1)
    {
        // TODO Auto-generated method stub

    }

    public void setStatus(int arg0)
    {
        // TODO Auto-generated method stub

    }

    public void setStatus(int arg0, String arg1)
    {
        // TODO Auto-generated method stub

    }

    public void flushBuffer() throws IOException
    {
        // TODO Auto-generated method stub

    }

    public int getBufferSize()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getCharacterEncoding()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public String getContentType()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Locale getLocale()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ServletOutputStream getOutputStream() throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public PrintWriter getWriter() throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isCommitted()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void reset()
    {
        // TODO Auto-generated method stub

    }

    public void resetBuffer()
    {
        // TODO Auto-generated method stub

    }

    public void setBufferSize(int arg0)
    {
        // TODO Auto-generated method stub

    }

    public void setCharacterEncoding( String arg0 )
    {
        // TODO Auto-generated method stub
    }
    
    public void setContentLength(int arg0)
    {
        // TODO Auto-generated method stub

    }

    public void setContentType(String arg0)
    {
        // TODO Auto-generated method stub

    }

    public void setLocale(Locale arg0)
    {
        // TODO Auto-generated method stub

    }

}
