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

import org.apache.ecs.ConcreteElement;
import org.apache.ecs.html.Input;
import org.apache.ecs.html.Select;
import org.apache.ecs.html.TextArea;

import com.ecyrd.jspwiki.forms.*;
import com.ecyrd.jspwiki.util.FormUtil;

/**
 *  Opens a WikiForm.
 *
 * Builds the HTML code for opening a FORM.
 *
 * <p>Since we're only providing an opening FORM tag, we can't use
 * the ECS utilities.
 *
 * A Form plugin line that produces one looks like this:
 * <p><pre>
 *   [{FormOpen name='formname' handler='pluginname'
 *          submit='submitservlet'
 *          show='always'
 *   }]
 * </pre>
 *
 * <p>Mandatory parameters:
 * <br>The <i>element</i> field specifies that this is a form open 
 * invocation.
 * <br>The <i>name</i> field identifies this particular form to the 
 * Form plugin across pages.
 * <br>The <i>handler</i> field is a WikiPlugin name; it will be 
 * invoked with the form field values.
 *
 * <p>Optional parameters:
 * <p>The submitservlet is the name of a JSP/servlet capable of 
 * handling the input from this form. It is optional; the default
 * value is the current page (which can handle the input by using
 * this Plugin.)
 *
 * <p>The <i>hide</i> parameter affects the visibility of this
 * form. If left out, the form is always shown. If set to
 * 'onsuccess', the form is not shown if it was submitted
 * successfully. (Note that a reload of the page would cause the
 * context to reset, and the form would be shown again. This may
 * be a useless option.)
 *
 *  @author ebu
 */
public class FormOpen
    extends FormElement
{
    private static org.apache.log4j.Logger log = 
	org.apache.log4j.Logger.getLogger( FormOpen.class );


    /**
     */
    public String execute( WikiContext ctx, Map params )
        throws PluginException
    {
        String formName = (String)params.get( PARAM_FORM );
        if( formName == null )
            throw new PluginException( "The Form 'open' element is missing the 'name' parameter." ); 
        String hide     = (String)params.get( PARAM_HIDEFORM );
        String sourcePage = ctx.getPage().getName();
        String submitServlet = (String)params.get( PARAM_SUBMITHANDLER );
        if( submitServlet == null )
            submitServlet = ctx.getURL( WikiContext.VIEW, sourcePage );

        FormInfo info = getFormInfo( ctx );
        if( info != null )
        {
            // Previous information may be the result of submitting
            // this form, or of a FormSet plugin, or both. If it
            // exists and is for this form, fine.
            if( formName.equals( info.getName() ) )
            {
                log.debug( "Previous FormInfo for this form was found in context." );
                // If the FormInfo exists, and if we're supposed to display on
                // error only, we need to exit now.
                if( hide != null && HIDE_SUCCESS.equals( hide ) && 
		    info.getStatus() == FormInfo.EXECUTED )
                {
                    info.setHide( true );
                    return( "<p>(no need to show form open now)" );
                }
            }
            else
            {
                // This would mean that a new form was started without
                // closing an old one.  Get rid of the garbage.
                info = new FormInfo();
            }
        }
        else
        {
            // No previous FormInfo available; store now, so it'll be
            // available for upcoming Form input elements.
            info = new FormInfo();
            storeFormInfo( ctx, info );
        }

        info.setName( formName );
        info.setAction( submitServlet );

        StringBuffer tag = new StringBuffer();
        tag.append( "<div class=\"wikiform\">\n" );
        tag.append( "<form action=\"" + submitServlet );
        tag.append( "\" name=\"" + formName );
        tag.append( "\" method=\"post\" enctype=\"application/x-www-form-urlencoded\">\n" );
        tag.append( "  <input type=\"hidden\" name=\"" + PARAM_FORMNAMEHIDDEN );
        tag.append( "\" value=\"" + formName + "\"/>\n" );

        return( tag.toString() );
    }

}
