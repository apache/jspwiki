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


/**
 * A FormHandler performs logic based on input from an
 * HTTP FORM, transmitted through a JSPWiki WikiPlugin
 * (see Form.java).
 * 
 * <P>This interface is currently empty and unused. It acts
 * as a place holder: we probably want to switch from 
 * WikiPlugins to FormHandlers in Form.java, to enforce
 * authentication, form execution permissions, and so on.
 */
public interface FormHandler
{
}
