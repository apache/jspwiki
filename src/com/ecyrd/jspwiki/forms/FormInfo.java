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
package com.ecyrd.jspwiki.forms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for carrying HTTP FORM information between
 * WikiPlugin invocations in the Session.
 *
 *  @author ebu
 */
public class FormInfo
    implements Serializable
{
    private static final long serialVersionUID = 0L;

    public static final int EXECUTED =  1;
    public static final int OK       =  0;
    public static final int ERROR    = -1;

    public int    m_status;
    public boolean m_hide;
    public String m_action;
    public String m_name;
    public String m_handler;
    public String m_result;
    public String m_error;
    //public PluginParameters submission;
    public Map m_submission;

    public FormInfo()
    {
        m_status = OK;
    }

    public void setStatus( int val )
    {
        m_status = val;
    }

    public int getStatus()
    {
        return m_status;
    }

    public void setHide( boolean val )
    {
        m_hide = val;
    }

    public boolean hide()
    {
        return m_hide;
    }

    public void setAction( String val )
    {
        m_action = val;
    }

    public String getAction()
    {
        return m_action;
    }

    public void setName( String val )
    {
        m_name = val;
    }

    public String getName()
    {
        return m_name;
    }

    public void setHandler( String val )
    {
        m_handler = val;
    }

    public String getHandler()
    {
        return m_handler;
    }

    public void setResult( String val )
    {
        m_result = val;
    }

    public String getResult()
    {
        return m_result;
    }

    public void setError( String val )
    {
        m_error = val;
    }

    public String getError()
    {
        return m_error;
    }

    /**
     * Copies the given values into the handler parameter map using Map.putAll().
     * @param val parameter name-value pairs for a Form handler WikiPlugin
     */
    public void setSubmission( Map val )
    {
        m_submission = new HashMap();
        m_submission.putAll( val );
    }

    /**
     * Adds the given values into the handler parameter map.
     * @param val parameter name-value pairs for a Form handler WikiPlugin
     */
    public void addSubmission( Map val )
    {
        if( m_submission == null )
            m_submission = new HashMap();
        m_submission.putAll( val );
    }

    /**
     * Returns parameter name-value pairs for a Form handler WikiPlugin.
     * The names are those of Form input fields, and the values whatever
     * the user selected in the form. The FormSet plugin can also be used
     * to provide initial values.
     *
     * @return Handler parameter name-value pairs.
     */
    public Map getSubmission()
    {
        return m_submission;
    }
}
