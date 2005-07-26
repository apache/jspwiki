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
import com.ecyrd.jspwiki.plugin.PluginManager;
import java.util.*;

import com.ecyrd.jspwiki.util.FormUtil;

/**
 */
public class FormOutput
    extends FormElement
{
    private static org.apache.log4j.Logger log = 
	org.apache.log4j.Logger.getLogger( FormOutput.class );

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
     */
    public String execute( WikiContext ctx, Map params )
        throws PluginException
    {
        //
        //  If there is no HTTP request, returns immediately.
        //
        if( ctx.getHttpRequest() == null )
        {
            return "";
        }
        
        // If we are NOT here due to this form being submitted, we do nothing.
        // The submitted form MUST have parameter 'formname' equal to the name
        // parameter of this Form plugin.

        String formName   = (String)params.get( PARAM_FORM );
        String submitForm = ctx.getHttpParameter( PARAM_FORMNAMEHIDDEN );
        String populator  = (String)params.get( PARAM_POPULATE );

        if( submitForm == null || formName == null || 
            !formName.equals( submitForm ) )
        {
            // No submitForm -> this was not a submission from the
            // generated form.  If populate is specified, we'll go
            // ahead and let the handler (populator) put stuff into
            // the context, otherwise we'll just hide.
            if( populator == null || !PARAM_HANDLER.equals( populator ) )
                return( "" );
            // If population was allowed, we should first  
        }

        String handler = (String)params.get( PARAM_HANDLER );
        if( handler == null || handler.length() == 0 )
        {
            // Need to print out an error here as this form is misconfigured
            return( "<p class=\"error\">Argument '" +
                    PARAM_HANDLER + "' required for Form plugin</p>" );
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
        Map handlerParams = FormUtil.requestToMap( ctx.getHttpRequest(), 
                                                   HANDLERPARAM_PREFIX );
        // Previous submission info may be available from FormSet
        // plugin - add, don't replace.
        info.addSubmission( handlerParams );

        // Pass the _body parameter from FormOutput on to the handler
        info.getSubmission().put(PluginManager.PARAM_BODY, 
                                 params.get(PluginManager.PARAM_BODY)); 

        String handlerOutput = null;
        String error = null;
        try
        {
            // The plugin _can_ modify the parameters, so we make sure
            // they stay with us.
            handlerOutput = ctx.getEngine().getPluginManager().execute( ctx, handler, info.getSubmission() );
            info.setResult( handlerOutput );
            info.setStatus( FormInfo.EXECUTED );
        }
        catch( PluginException pe )
        {
            error = "<p class=\"error\">" + pe.getMessage();
            info.setError( error );
            info.setStatus( FormInfo.ERROR );
        }

        // We store the forminfo, so following Form plugin invocations on this
        // page can decide what to do based on its values.
        storeFormInfo( ctx, info );

        if( error != null )
            return( error );
        else
            return( handlerOutput != null ? handlerOutput : "" );
    }

}
