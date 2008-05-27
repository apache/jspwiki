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
import com.ecyrd.jspwiki.plugin.WikiPlugin;

import java.util.*;

import org.apache.ecs.xhtml.input;

/**
 *  Creates a simple input text field.
 */
public class FormInput
    extends FormElement
{
    public static final String PARAM_TYPE  = "type";
    public static final String PARAM_SIZE  = "size";

    /**
     * Generates a dynamic form element on the WikiPage.
     */
    public String execute( WikiContext ctx, Map params )
        throws PluginException
    {
        String inputName  = (String)params.get( PARAM_INPUTNAME );
        String inputValue = (String)params.get( PARAM_VALUE );
        String inputType  = (String)params.get( PARAM_TYPE );
        String size       = (String)params.get( PARAM_SIZE );
        ResourceBundle rb = ctx.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);

        if( inputName == null )
            throw new PluginException( rb.getString( "forminput.namemissing" ) );
        if( inputValue == null )
            inputValue = "";

        // Don't render if no error and error-only-rendering is on.
        FormInfo info = getFormInfo( ctx );
        Map previousValues = null;
        if( info != null )
        {
            if( info.hide() )
            {
                return( "<p>" + rb.getString( "forminput.noneedtoshow" ) + "</p>" );
            }
            previousValues = info.getSubmission();
        }

        if( previousValues == null )
        {
            previousValues = new HashMap();
        }

        // In order to isolate posted form elements into their own
        // map, prefix the variable name here. It will be stripped
        // when the handler plugin is executed.
        input field = new input( inputType, 
                                 HANDLERPARAM_PREFIX + inputName, 
                                 inputValue );

        String checked = (String)params.get("checked");
        field.setChecked( TextUtil.isPositive(checked)
                          || "checked".equalsIgnoreCase(checked) );
        
        String oldValue = (String)previousValues.get( inputName );
        if( oldValue != null )
        {
            field.setValue( oldValue );
        }

        if( size != null ) field.setSize( size );

        return field.toString(ctx.getEngine().getContentEncoding());
    }
}
