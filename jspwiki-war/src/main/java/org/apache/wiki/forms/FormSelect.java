/* 
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

package org.apache.wiki.forms;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.util.XHTML;
import org.apache.wiki.util.XhtmlUtil;
import org.jdom2.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 *  Creates a Form select field.
 */
public class FormSelect
    extends FormElement
{
    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext ctx, Map< String, String > params )
        throws PluginException
    {
        // Don't render if no error and error-only-rendering is on.
        FormInfo info = getFormInfo( ctx );

        ResourceBundle rb = Preferences.getBundle( ctx, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );
        Map< String, String > previousValues = null;
        
        if ( info != null )
        {
            if ( info.hide() )
            {
                return "<p>" + rb.getString( "forminput.noneedtoshow" ) + "</p>";
            }
            previousValues = info.getSubmission();
        }

        if ( previousValues == null )
        {
            previousValues = new HashMap< String, String >();
        }

        Element field = buildSelect( params, previousValues, rb );

        // We should look for extra params, e.g. width, ..., here.
        return XhtmlUtil.serialize(field); // ctx.getEngine().getContentEncoding()
    }


    /**
     * Builds a Select element.
     */
    private Element buildSelect(
            Map<String,String> pluginParams,
            Map<String,String> ctxValues, 
            ResourceBundle rb )
            throws PluginException
    {
        String inputName = pluginParams.get( PARAM_INPUTNAME );
        if ( inputName == null ) {
            throw new PluginException( rb.getString( "formselect.namemissing" ) );
        }
    
        String inputValue = pluginParams.get( PARAM_VALUE );
        String previousValue = ctxValues.get( inputName );
        //
        // We provide several ways to override the separator, in case
        // some input application the default value.
        //
        String optionSeparator = pluginParams.get( "separator" );
        if ( optionSeparator == null ) {
        	optionSeparator = ctxValues.get( "separator." + inputName);
        }
        if ( optionSeparator == null ) {
        	optionSeparator = ctxValues.get( "select.separator" );
        }
        if ( optionSeparator == null ) {
        	optionSeparator = ";";
        }
        
        String optionSelector = pluginParams.get( "selector" );
        if ( optionSelector == null ) {
        	optionSelector = ctxValues.get( "selector." + inputName );
        }
        if ( optionSelector == null ) {
        	optionSelector = ctxValues.get( "select.selector" );
        }
        if ( optionSelector == null ) {
        	optionSelector = "*";
        }
        if ( optionSelector.equals( optionSeparator ) ) {
        	optionSelector = null;
        }
        if ( inputValue == null ) {
        	inputValue = "";
        }

        // If values from the context contain the separator, we assume
        // that the plugin or something else has given us a better
        // list to display.
        boolean contextValueOverride = false;
        if ( previousValue != null ) {
            if ( previousValue.indexOf( optionSeparator ) != -1 ) {
                inputValue = previousValue;
                previousValue = null;
            } else {
                // If a context value exists, but it's not a list,
                // it'll just override any existing selector
                // indications.
                contextValueOverride = true;
            }
        }

        String[] options = inputValue.split( optionSeparator );
        int previouslySelected = -1;
        
        Element[] optionElements = new Element[options.length];
        
        //
        //  Figure out which one of the options to select: prefer the one
        //  that was previously selected, otherwise try to find the one
        //  with the "select" marker.
        //
        for( int i = 0; i < options.length; i++ ) {
            int indicated = -1;
            options[i] = options[i].trim();
            
            if ( optionSelector != null && options[i].startsWith( optionSelector ) ) {
                options[i] = options[i].substring( optionSelector.length() );
                indicated = i;
            }
            if ( previouslySelected == -1 ) {
                if ( !contextValueOverride && indicated > 0 ) {
                    previouslySelected = indicated;
                } else if ( previousValue != null && options[i].equals( previousValue ) ) {
                    previouslySelected = i;
                }
            }
            
            // huh?
//          optionElements[i] = new option( options[i] );
//          optionElements[i].addElement( options[i] );
            
            optionElements[i] = XhtmlUtil.element(XHTML.option,options[i]);
        }

        if ( previouslySelected > -1 ) {
        	optionElements[previouslySelected].setAttribute(XHTML.ATTR_selected,"true");
        }

        Element select = XhtmlUtil.element(XHTML.select);
        select.setAttribute(XHTML.ATTR_name,HANDLERPARAM_PREFIX + inputName);
        for ( Element option : optionElements ) {
            select.addContent(option);
        }
        return select;
    }
}
