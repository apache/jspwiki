/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package com.ecyrd.jspwiki.forms;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.WikiPlugin;

import java.util.*;

import org.apache.ecs.ConcreteElement;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;

/**
 *  Creates a Form select field.
 *  
 *  @author ebu
 */
public class FormSelect
    extends FormElement
{
    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext ctx, Map params )
        throws PluginException
    {
        // Don't render if no error and error-only-rendering is on.
        FormInfo info = getFormInfo( ctx );

        ResourceBundle rb = ctx.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);
        Map previousValues = null;
        
        if( info != null )
        {
            if( info.hide() )
            {
                return "<p>" + rb.getString( "forminput.noneedtoshow" ) + "</p>";
            }
            previousValues = info.getSubmission();
        }

        if( previousValues == null )
        {
            previousValues = new HashMap();
        }

        ConcreteElement field = null;
        
        field = buildSelect( params, previousValues, rb );

        // We should look for extra params, e.g. width, ..., here.
        if( field != null )
            return field.toString(ctx.getEngine().getContentEncoding());
        
        return "";
    }


    /**
     * Builds a Select element.
     */
    private select buildSelect( Map pluginParams, Map ctxValues, ResourceBundle rb )
        throws PluginException
    {
        String inputName = (String)pluginParams.get( PARAM_INPUTNAME );
        if( inputName == null )
        {
            throw new PluginException( rb.getString( "formselect.namemissing" ) );
        }
    
        String inputValue = (String)pluginParams.get( PARAM_VALUE );
        String previousValue = (String)ctxValues.get( inputName );
        //
        // We provide several ways to override the separator, in case
        // some input application the default value.
        //
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
        
        option[] optionElements = new option[options.length];
        
        //
        //  Figure out which one of the options to select: prefer the one
        //  that was previously selected, otherwise try to find the one
        //  with the "select" marker.
        //
        for( int i = 0; i < options.length; i++ )
        {
            int indicated = -1;
            options[i] = options[i].trim();
            
            if( optionSelector != null && options[i].startsWith( optionSelector ) ) 
            {
                options[i] = options[i].substring( optionSelector.length() );
                indicated = i;
            }
            if( previouslySelected == -1 )
            {
                if( !contextValueOverride && indicated > 0 )
                {
                    previouslySelected = indicated;
                }
                else if( previousValue != null && 
                        options[i].equals( previousValue ) )
                {
                    previouslySelected = i;
                }
            }
            
            optionElements[i] = new option( options[i] );
            optionElements[i].addElement( options[i] );
        }

        if( previouslySelected > -1 ) optionElements[previouslySelected].setSelected(true);
        select field = new select( HANDLERPARAM_PREFIX + inputName, optionElements );

        return field;
    }
}
