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
import com.ecyrd.jspwiki.plugin.WikiPlugin;
import java.util.*;

/**
 */
public abstract class FormElement
    implements WikiPlugin
{
    private static org.apache.log4j.Logger log = 
	org.apache.log4j.Logger.getLogger( FormElement.class );

    /**
     * In order to make the form-to-handler parameter transfer easier,
     * we prefix all user-specified FORM element names with HANDLERPARAM_PREFIX
     * the HTML output. This lets us differentiate user-defined FormHandler
     * parameters from Form parameters.
     * The submit handler must then use MapUtil.requestToMap() to
     * strip them before executing the handler itself.
     */
    public static final String HANDLERPARAM_PREFIX = "nbf_";

    /**
     * The submit servlet may decide to store a FormInfo with user-entered
     * form values in the Session. It should use this name, and this class
     * checks for it to pre-fill fields from a previous form submit.
     */
    public static final String FORM_VALUES_CARRIER = "nbpf_values";

    // Show values:
    public static final String HIDE_SUCCESS = "onsuccess";

    // Parameter names:
    /** Plugin parameter, optional, indicates servlet to post to. */
    public static final String PARAM_SUBMITHANDLER = "submit";
    /** Plugin parameter, mandatory, indicates what form element to insert. */
    public static final String PARAM_ELEMENT    = "element";
    /** 
     * Plugin parameter, mandatory in output element, indicates
     * WikiPlugin to use to handle form submitted data.
     */
    public static final String PARAM_HANDLER    = "handler";
    /** Plugin parameter, mandatory in open/output: name of the form. */
    public static final String PARAM_FORM       = "form";
    /** Plugin parameter, mandatory in input elements: name of an element. */
    public static final String PARAM_INPUTNAME       = "name";
    /** Plugin parameter, optional: default value for an input. */
    public static final String PARAM_VALUE      = "value";
    /** Experimental: hide the form if it was submitted successfully. */ 
    public static final String PARAM_HIDEFORM   = "hide";

    /** If set to 'handler' in output element, the handler plugin is
     * called even on first invocation (no submit). The plugin can
     * then place values into its parameter map, and these are seen by
     * subsequent Form elements. (Store a value in the plugin with the
     * same key as an input element, and the value will be visible in
     * the initial form.)
     */
    public static final String PARAM_POPULATE = "populate";
    /** HTTP parameter, inserted as hidden variable into the generated form. */
    public static final String PARAM_FORMNAMEHIDDEN   = "formname";

    // Key to store the form info container in the context by:
    //public static final String CONTEXT_FORMINFO = "FormPluginInfo";

    /**
     * Utility method stores a FormInfo object into the WikiContext.
     */
    protected void storeFormInfo( WikiContext ctx, FormInfo info )
    {
        ctx.setVariable( FORM_VALUES_CARRIER, info );
    }

    /**
     * Attempts to retrieve information on the currently handled
     * form from the WikiContext.
     */
    protected FormInfo getFormInfo( WikiContext ctx )
    {
        return (FormInfo)ctx.getVariable( FORM_VALUES_CARRIER );
    }
}
