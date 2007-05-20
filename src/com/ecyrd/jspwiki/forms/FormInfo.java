/*
    WikiForms - a WikiPage FORM handler for JSPWiki.

    Copyright (C) 2003 BaseN.

    JSPWiki Copyright (C) 2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
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
