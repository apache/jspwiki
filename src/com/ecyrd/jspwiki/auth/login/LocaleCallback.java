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
package com.ecyrd.jspwiki.auth.login;

import java.util.Locale;

import javax.security.auth.callback.Callback;

/**
 * Callback for requesting and supplying a {@link java.util.Locale}. This callback is
 * used by LoginModules that need access to a Locale for creating localized
 * messages.
 * @author Andrew Jaquith
 * @since 3.0
 */
public class LocaleCallback implements Callback
{

    private Locale m_locale;

    /**
     * Returns the Locale. LoginModules call this method after a
     * CallbackHandler sets the Locale.
     * @return the locale
     */
    public Locale getLocale()
    {
        return m_locale;
    }

    /**
     * Sets the Locale. CallbackHandler objects call this method.
     * @param locale the user locale
     */
    public void setLocale( Locale locale )
    {
        m_locale = locale;
    }

}
