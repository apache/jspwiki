/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 JSPWiki development group

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.ui.admin;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.WikiDocument;
import com.ecyrd.jspwiki.render.RenderingManager;

/**
 *  This class is still experimental.
 * @author jalkanen
 *
 */
public abstract class WikiFormAdminBean
    implements AdminBean
{
    public abstract String getForm( WikiContext context );
    
    public abstract void handleResponse( WikiContext context, Map params );

    public String doGet(WikiContext context)
    {
        String result = "";
        
        String wikiMarkup = getForm(context);
        
        RenderingManager mgr = context.getEngine().getRenderingManager();
        
        WikiDocument doc;
        try
        {
            doc = mgr.getParser( context, wikiMarkup ).parse();
            result = mgr.getHTML(context, doc);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return result;
    }

    public String handlePost(WikiContext context, HttpServletRequest req, HttpServletResponse resp)
    {
        return null;
        // FIXME: Not yet implemented
    }
}
