/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.methods;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.dav.DavPath;
import com.ecyrd.jspwiki.dav.DavProvider;

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
    public PropPatchMethod( DavProvider provider )
    {
        super( provider );
    }

    public void execute( HttpServletRequest req, HttpServletResponse res, DavPath dp ) throws ServletException, IOException
    {
        res.sendError( HttpServletResponse.SC_UNAUTHORIZED, "JSPWiki is read-only" );
    }

}
