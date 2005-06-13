/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.methods;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.dav.DavProvider;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public abstract class DavMethod
{
    protected DavProvider m_provider;
    
    /**
     * 
     */
    public DavMethod( DavProvider provider )
    {
        m_provider = provider;
    }

    public abstract void execute( HttpServletRequest req, HttpServletResponse res )
    throws ServletException, IOException;
}
