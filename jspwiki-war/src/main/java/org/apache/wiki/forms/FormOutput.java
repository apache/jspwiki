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
import org.apache.wiki.api.engine.PluginManager;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.plugin.DefaultPluginManager;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.util.FormUtil;

import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;

/**
 */
public class FormOutput
        extends FormElement
{
    /**
     * Executes the FormHandler specified in a Form 'output' plugin,
     * using entries provided in the HttpRequest as FormHandler
     * parameters.
     * <p>
     * If the parameter 'populate' was given, the WikiPlugin it names
     * is used to get default values. (It probably makes a lot of
     * sense for this to be the same plugin as the handler.) 
     * Information for the populator can be given with the FormSet
     * plugin. If 'populate' is not specified, the form is not
     * displayed.
     * <p>
     * Should there be no HTTP request associated with this request,
     * the method will return immediately with an empty string.
     * 
     * @param ctx {@inheritDoc}
     * @param params {@inheritDoc}
     * @return {@inheritDoc}
     */
    public String execute( WikiContext ctx, Map< String, String > params )
        throws PluginException
    {
        //
        //  If there is no HTTP request, returns immediately.
        //
        if( ctx.getHttpRequest() == null )
        {
            return "";
        }
        ResourceBundle rb = Preferences.getBundle( ctx, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );
        
        // If we are NOT here due to this form being submitted, we do nothing.
        // The submitted form MUST have parameter 'formname' equal to the name
        // parameter of this Form plugin.

        String formName   = params.get( PARAM_FORM );
        String submitForm = ctx.getHttpParameter( PARAM_FORMNAMEHIDDEN );
        String populator  = params.get( PARAM_POPULATE );

        if( submitForm == null || formName == null || 
            !formName.equals( submitForm ) )
        {
            // No submitForm -> this was not a submission from the
            // generated form.  If populate is specified, we'll go
            // ahead and let the handler (populator) put stuff into
            // the context, otherwise we'll just hide.
            if( populator == null || !PARAM_HANDLER.equals( populator ) )
                return "";
            // If population was allowed, we should first  
        }

        String handler = params.get( PARAM_HANDLER );
        if( handler == null || handler.length() == 0 )
        {
            // Need to print out an error here as this form is misconfigured
            return "<p class=\"error\">" + MessageFormat.format( rb.getString( "formoutput.missingargument" ), PARAM_HANDLER ) + "</p>";
        }

        String sourcePage = ctx.getPage().getName();
        String submitServlet = ctx.getURL( WikiContext.VIEW, sourcePage );

        // If there is previous FormInfo available - say, from a
        // FormSet plugin - use it.
        FormInfo info = getFormInfo( ctx );
        if( info == null )
        {
            // Reconstruct the form info from post data
            info = new FormInfo();
            info.setName( formName );
        }
        // Force override of handler and submit.
        info.setHandler( handler );
        info.setAction( submitServlet );

        // Sift out all extra parameters, leaving only those submitted
        // in the HTML FORM.
        Map< String, String > handlerParams = FormUtil.requestToMap( ctx.getHttpRequest(), 
                                                                     HANDLERPARAM_PREFIX );
        // Previous submission info may be available from FormSet
        // plugin - add, don't replace.
        info.addSubmission( handlerParams );

        // Pass the _body parameter from FormOutput on to the handler
        info.getSubmission().put( DefaultPluginManager.PARAM_BODY, 
                                  params.get(DefaultPluginManager.PARAM_BODY)); 

        String handlerOutput = null;
        String error = null;
        try
        {
            // The plugin _can_ modify the parameters, so we make sure
            // they stay with us.
            PluginManager pm = ctx.getEngine().getPluginManager();
            handlerOutput = pm.execute( ctx, handler, info.getSubmission() );
            info.setResult( handlerOutput );
            info.setStatus( FormInfo.EXECUTED );
        }
        catch( PluginException pe )
        {
            error = "<p class=\"error\">" + pe.getMessage() + "</p>";
            info.setError( error );
            info.setStatus( FormInfo.ERROR );
        }

        // We store the forminfo, so following Form plugin invocations on this
        // page can decide what to do based on its values.
        storeFormInfo( ctx, info );

        if( error != null )
            return error;

        return handlerOutput != null ? handlerOutput : "";
    }

}
