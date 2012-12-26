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

import org.apache.ecs.xhtml.input;
import org.apache.wiki.TextUtil;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;

/**
 *  Creates a simple input text field.
 */
public class FormInput
    extends FormElement
{
    /** Parameter name for setting the type.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_TYPE  = "type";

    /** Parameter name for setting the size of the input field.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_SIZE  = "size";

    /**
     * Generates a dynamic form element on the WikiPage.
     * 
     * {@inheritDoc}
     */
    public String execute( WikiContext ctx, Map< String, String > params )
        throws PluginException
    {
        String inputName  = params.get( PARAM_INPUTNAME );
        String inputValue = params.get( PARAM_VALUE );
        String inputType  = params.get( PARAM_TYPE );
        String size       = params.get( PARAM_SIZE );
        ResourceBundle rb = ctx.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);

        if( inputName == null )
            throw new PluginException( rb.getString( "forminput.namemissing" ) );
        if( inputValue == null )
            inputValue = "";

        // Don't render if no error and error-only-rendering is on.
        FormInfo info = getFormInfo( ctx );
        Map< String, String > previousValues = null;
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
            previousValues = new HashMap< String, String >();
        }

        // In order to isolate posted form elements into their own
        // map, prefix the variable name here. It will be stripped
        // when the handler plugin is executed.
        input field = new input( inputType, 
                                 HANDLERPARAM_PREFIX + inputName, 
                                 inputValue );

        String checked = params.get("checked");
        field.setChecked( TextUtil.isPositive(checked)
                          || "checked".equalsIgnoreCase(checked) );
        
        String oldValue = previousValues.get( inputName );
        if( oldValue != null )
        {
            field.setValue( oldValue );
        }

        if( size != null ) field.setSize( size );

        return field.toString(ctx.getEngine().getContentEncoding());
    }
}
