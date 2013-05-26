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

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;

/**
 * FormSet is a companion WikiPlugin for Form. 
 * 
 * <p>The mandatory 'form' parameter specifies which form the variable
 * applies to.  Any other parameters are put directly into a FormInfo
 * object that will be available to a Form plugin called 'form'
 * (presumably invoked later on the same WikiPage).
 * 
 * <p>If the name of a FormSet parameter is the same as the name of
 * a Form plugin input element later on the same page, the Form will
 * consider the given value the default for the form field. (However,
 * the handler for the Form is free to use the value as it wishes, and
 * even override it.)
 *
 * <p>If the name of a parameter is not present in Form input fields,
 * the parameter is presumably meant for sending initial information
 * to the Form handler. If this is the case, you may want to specify the
 * <i>populate=''</i> in the Form <i>open</i> element, otherwise the
 * form won't be displayed on the first invocation.
 *  
 * <p>This object looks for a FormInfo object named
 * FORM_VALUES_CARRIER in the WikiContext. If found, it checks that
 * its name matches the 'form' parameter, and if it does, adds the
 * plugin parameters to the FormInfo. If the names don't match, the
 * old FormInfo is discarded and a new one is created. Only one
 * FormInfo is supported at a time. A practical consequence of this is
 * that a FormSet invocation only applies to the Form plugins that
 * follow it; any further Forms need new FormSet calls.
 *
 * @see FormInfo
 */
public class FormSet
    implements WikiPlugin
{    
    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext ctx, Map< String, String > params )
        throws PluginException
    {
        String formName = params.get( FormElement.PARAM_FORM );
        if( formName == null || formName.trim().length() == 0 )
        {
            return "";
        }

        FormInfo info = (FormInfo)ctx.getVariable( FormElement.FORM_VALUES_CARRIER );

        if( info == null || formName.equals( info.getName() ) == false )
        {
            info = new FormInfo();
            ctx.setVariable( FormElement.FORM_VALUES_CARRIER, info );
        }

        //
        //  Create a copy for the context.  Unfortunately we need to 
        //  create slightly modified copy, because otherwise on next
        //  invocation this might be coming from a cache; so we can't
        //  modify the original param string.
        //
        info.setName( formName );
        Map< String, String > hm = new HashMap< String, String >();
        hm.putAll( params );
        
        hm.remove( FormElement.PARAM_FORM );
        info.addSubmission( hm );
        
        return "";
    }
}
