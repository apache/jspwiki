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

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.plugin.PluginException;
import java.util.*;

/**
 *  Closes a WikiForm.
 *
 *  @author ebu
 */
public class FormClose
    extends FormElement
{
    /**
     * Builds a Form close tag. Removes any information on the form from
     * the WikiContext.
     */
    public String execute( WikiContext ctx, Map params )
        throws PluginException
    {
        StringBuffer tags = new StringBuffer();
        tags.append( "</form>\n" );
        tags.append( "</div>" );

        // Don't render if no error and error-only-rendering is on.
        FormInfo info = getFormInfo( ctx );
        if( info != null )
        {
            if( info.hide() )
            {
                return( "<p>(no need to show close now)</p>" );
            }
        }

        // Get rid of remaining form data, so it doesn't mess up other forms.
        // After this, it is safe to add other Forms.
        storeFormInfo( ctx, null );

        return( tags.toString() );

    }
}
