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
public class PropPatchMethod 
    extends DavMethod
{

    /**
     * @param engine
     */
    public PropPatchMethod( WikiEngine engine )
    {
        super( engine );
    }

    public void execute( HttpServletRequest req, HttpServletResponse res ) throws ServletException, IOException
    {
        res.sendError( HttpServletResponse.SC_UNAUTHORIZED, "JSPWiki is read-only" );
    }

}
