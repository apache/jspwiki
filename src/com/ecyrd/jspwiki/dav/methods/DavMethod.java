/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.methods;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.WikiEngine;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public abstract class DavMethod
{
    protected WikiEngine m_engine;
    
    /**
     * 
     */
    public DavMethod( WikiEngine engine )
    {
        m_engine = engine;
    }

    public abstract void execute( HttpServletRequest req, HttpServletResponse res )
    throws ServletException, IOException;
}
