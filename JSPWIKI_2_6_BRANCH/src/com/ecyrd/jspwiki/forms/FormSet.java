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
import com.ecyrd.jspwiki.plugin.*;
import java.util.*;

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
 * @author ebu
 */
public class FormSet
    implements WikiPlugin
{    
    public String execute( WikiContext ctx, Map params )
        throws PluginException
    {
        String formName = (String)params.get( FormElement.PARAM_FORM );
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
        HashMap hm = new HashMap();
        hm.putAll( params );
        
        hm.remove( FormElement.PARAM_FORM );
        info.addSubmission( hm );
        
        return "";
    }
}
