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
package org.apache.wiki.ui.stripes;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.preferences.Preferences;

import net.sourceforge.stripes.localization.DefaultLocalePicker;

/**
 *  This is a simple Stripes LocalePicker which uses {@link Preferences#getLocale(HttpServletRequest)}
 *  to determine the request Locale.
 */
public class PreferencesLocalePicker extends DefaultLocalePicker
{

    /**
     *  JSPWiki only uses UTF-8.
     */
    @Override
    public String pickCharacterEncoding( HttpServletRequest request, Locale locale )
    {
        return "UTF-8";
    }

    /**
     *  Simply calls Preferences.getLocale() to pick the locale.
     */
    @Override
    public Locale pickLocale( HttpServletRequest request )
    {
        return Preferences.getLocale( request );
    }
    
}
