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
import com.ecyrd.jspwiki.util.FormUtil;
import com.ecyrd.jspwiki.plugin.PluginException;
import java.util.*;

import org.apache.ecs.ConcreteElement;
import org.apache.ecs.html.Select;

/**
 *  @author ebu
 */
public class FormSelect
    extends FormElement
{
    private static org.apache.log4j.Logger log = 
	org.apache.log4j.Logger.getLogger( FormSelect.class );

    public String execute( WikiContext ctx, Map params )
        throws PluginException
    {
        // Don't render if no error and error-only-rendering is on.
        FormInfo info = getFormInfo( ctx );
        if( info != null )
        {
            if( info.hide() )
            {
                return( "<p>(no need to show input field now)</p>" );
            }
        }

        Map previousValues = info.getSubmission();
	if( previousValues == null )
	{
	    previousValues = new HashMap();
	}

        ConcreteElement field = null;

        field = buildSelect( params, previousValues );

        // We should look for extra params, e.g. width, ..., here.
        if( field != null )
            return( field.toString() );
        else
            return( "" );
    }


    /**
     * Builds a Select element.
     */
    private Select buildSelect( Map pluginParams, Map ctxValues )
        throws PluginException
    {
        String inputName = (String)pluginParams.get( PARAM_INPUTNAME );
        if( inputName == null )
            throw new PluginException( "Select element is missing parameter 'name'." );
        String inputValue = (String)pluginParams.get( PARAM_VALUE );
        String previousValue = (String)ctxValues.get( inputName );
        // We provide several ways to override the separator, in case
        // some input application the default value.
        String optionSeparator = (String)pluginParams.get( "separator" );
        if( optionSeparator == null )
            optionSeparator = (String)ctxValues.get( "separator." + inputName);
        if( optionSeparator == null )
            optionSeparator = (String)ctxValues.get( "select.separator" );
        if( optionSeparator == null )
            optionSeparator = ";";
        String optionSelector = (String)pluginParams.get( "selector" );
        if( optionSelector == null )
            optionSelector = (String)ctxValues.get( "selector." + inputName );
        if( optionSelector == null )
            optionSelector = (String)ctxValues.get( "select.selector" );
        if( optionSelector == null )
            optionSelector = "*";
        if( optionSelector.equals( optionSeparator ) )
            optionSelector = null;
        if( inputValue == null )
            inputValue = "";

        // If values from the context contain the separator, we assume
        // that the plugin or something else has given us a better
        // list to display.
        boolean contextValueOverride = false;
        if( previousValue != null )
        {
            if( previousValue.indexOf( optionSeparator ) != -1 )
            {
                inputValue = previousValue;
                previousValue = null;
            }
            else
            {
                // If a context value exists, but it's not a list,
                // it'll just override any existing selector
                // indications.
                contextValueOverride = true;
            }
        }

        String[] options = inputValue.split( optionSeparator );
        if( options == null )
            options = new String[0];
        int previouslySelected = -1;
        for( int i = 0; i < options.length; i++ )
        {
            int indicated = -1;
            options[i] = options[i].trim();
            if( options[i].startsWith( optionSelector ) ) 
            {
                options[i] = options[i].substring( optionSelector.length() );
                indicated = i;
            }
            if( previouslySelected == -1 )
            {
                if( !contextValueOverride && indicated > 0 )
                    previouslySelected = indicated;
                else if( previousValue != null && 
			 options[i].equals( previousValue ) )
                    previouslySelected = i;
            }
        }
        Select field = new Select( HANDLERPARAM_PREFIX + inputName, options );
        if( previouslySelected > -1 )
            field.selectOption( previouslySelected );

        return( field );
    }
}
