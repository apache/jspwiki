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
import org.apache.ecs.xhtml.textarea;

/**
 *  @author ebu
 */
public class FormTextarea
    extends FormElement
{
    public static final String PARAM_ROWS = "rows";
    public static final String PARAM_COLS = "cols";

    public String execute( WikiContext ctx, Map params )
        throws PluginException
    {
        // Don't render if no error and error-only-rendering is on.
        FormInfo info = getFormInfo( ctx );
        if( info != null )
        {
            if( info.hide() )
            {
                return( "<p>(no need to show textarea field now)</p>" );
            }
        }

        Map previousValues = info.getSubmission();
        if( previousValues == null )
        {
            previousValues = new HashMap();
        }

        ConcreteElement field = null;

        field = buildTextArea( params, previousValues );

        // We should look for extra params, e.g. width, ..., here.
        if( field != null )
            return( field.toString() );
        
        return( "" );
    }

    private textarea buildTextArea( Map params,
				    Map previousValues )
        throws PluginException
    {
        String inputName = (String)params.get( PARAM_INPUTNAME );
        String rows = (String)params.get( PARAM_ROWS );
        String cols = (String)params.get( PARAM_COLS );

        if( inputName == null )
            throw new PluginException( "Textarea element is missing " +
				       "parameter 'name'." );
	
        // In order to isolate posted form elements into their own
        // map, prefix the variable name here. It will be stripped
        // when the handler plugin is executed.
        textarea field = new textarea( HANDLERPARAM_PREFIX + inputName,
                                       rows, cols);
	
        if( previousValues != null )
        {
            String oldValue = (String)previousValues.get( inputName );
            if( oldValue != null )
            {
                field.addElement( oldValue );
            }
            else
            {
                oldValue = (String)params.get( PARAM_VALUE );
                if( oldValue != null ) field.addElement( oldValue );
            }
        }
        return( field );
    }
}
