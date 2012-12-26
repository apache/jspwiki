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

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.ecs.ConcreteElement;
import org.apache.ecs.xhtml.textarea;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;

/**
 *  Creates a Form text area element.   You may specify the size of the textarea
 *  by using the {@link #PARAM_COLS} and {@link #PARAM_ROWS} to signify the width
 *  and height of the area.
 *  
 */
public class FormTextarea extends FormElement
{
    /** Parameter name for setting the rows value.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_ROWS = "rows";
    
    /** Parameter name for setting the columns value.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_COLS = "cols";

    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext ctx, Map< String, String > params )
        throws PluginException
    {
        // Don't render if no error and error-only-rendering is on.
        FormInfo info = getFormInfo( ctx );
        Map< String, String > previousValues = null;
        ResourceBundle rb = ctx.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);

        if( info != null )
        {
            if( info.hide() )
            {
                return "<p>" + rb.getString( "formclose.noneedtoshow" ) + "</p>";
            }
            previousValues = info.getSubmission();
        }

        if( previousValues == null )
        {
            previousValues = new HashMap< String, String >();
        }

        ConcreteElement field = null;

        field = buildTextArea( params, previousValues, rb );

        // We should look for extra params, e.g. width, ..., here.
        if( field != null )
            return field.toString( ctx.getEngine().getContentEncoding() );

        return "";
    }

    private textarea buildTextArea( Map< String, String > params, Map< String, String > previousValues, ResourceBundle rb )
        throws PluginException
    {
        String inputName = params.get( PARAM_INPUTNAME );
        String rows = params.get( PARAM_ROWS );
        String cols = params.get( PARAM_COLS );

        if( inputName == null )
            throw new PluginException( rb.getString( "formtextarea.namemissing" ) );

        // In order to isolate posted form elements into their own
        // map, prefix the variable name here. It will be stripped
        // when the handler plugin is executed.
        textarea field = new textarea( HANDLERPARAM_PREFIX + inputName,
                                       rows, cols);

        if( previousValues != null )
        {
            String oldValue = previousValues.get( inputName );
            if( oldValue != null )
            {
                field.addElement( oldValue );
            }
            else
            {
                oldValue = params.get( PARAM_VALUE );
                if( oldValue != null ) field.addElement( oldValue );
            }
        }
        return field;
    }
}
