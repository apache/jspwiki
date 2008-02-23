/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.filters;

import net.sourceforge.stripes.action.Resolution;

/**
 *  Subclass of exception may be thrown if a filter wants to reject something and
 *  redirect the user elsewhere. Unlike {@link RedirectResolution}, this class
 *  requires the caller to supply an i18n message key and Stripes
 *  {@link net.sourceforge.stripes.action.Resolution} instead of a 
 *  full-text message and URI.
 *
 *  @since 3.0
 */
public class ResolutionException
    extends FilterException
{
    private static final long serialVersionUID = 0L;

    private final Resolution m_resolution;

    /**
     * Constructs a new exception with a supplied string message and
     * Stripes {@link net.sourceforge.stripes.action.Resolution}.
     * A typical Resolution is the {@link net.sourceforge.stripes.action.ForwardResolution},
     * which forwards the user to an ActionBean or application path. Another is
     * the {@link net.sourceforge.stripes.action.RedirectResolution}, which issues
     * a client-side redirect, and will send the user to an ActionBean or to any
     * arbitrary URI. The Resolution can be retrieved easily by
     * @param messageKey the i18n message key, representing the error 
     * to send back to the caller
     * @param resolution the resolution
     */
    public ResolutionException( String messageKey, Resolution resolution )
    {
        super( messageKey );
        
        if ( resolution == null )
        {
            throw new IllegalArgumentException("Resolution cannot be null.");
        }

        m_resolution = resolution;
    }

    /**
     *  Get the Resolution for redirection. If the constructor {@link #RedirectException(String, String)}
     *  was used, this method may return <code>null</code>.
     * @return the Resolution for redirection
     */
    public Resolution getResolution()
    {
        return m_resolution;
    }
}
