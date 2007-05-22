/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth.login;

import javax.security.auth.callback.Callback;

import com.ecyrd.jspwiki.WikiEngine;

/**
 * Callback for requesting and supplying the WikiEngine object required by a
 * LoginModule. This Callback is used by LoginModules needing access to the
 * external authorizer or group manager.
 * @author Janne Jalkanen
 * @since 2.5
 */
public class WikiEngineCallback implements Callback
{

    private WikiEngine m_engine;

    /**
     * Sets the engine object. CallbackHandler objects call this method.
     * @param engine the engine
     */
    public void setEngine( WikiEngine engine )
    {
        m_engine = engine;
    }

    /**
     * Returns the engine. LoginModules call this method after a
     * CallbackHandler sets the engine.
     * @return the engine
     */
    public WikiEngine getEngine()
    {
        return m_engine;
    }

}