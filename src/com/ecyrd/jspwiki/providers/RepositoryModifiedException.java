/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.providers;

/**
 *  If the provider detects that someone has modified the repository
 *  externally, it should throw this exception.
 *  <p>
 *  Any provider throwing this exception should first clean up any references
 *  to the modified page it has, so that when we call this the next time,
 *  the page is handled as completely, and we don't get the same exception
 *  again.
 *
 *  @author Janne Jalkanen
 *  @since  2.1.25
 */
public class RepositoryModifiedException
    extends ProviderException
{
    private static final long serialVersionUID = 0L;
    
    protected String m_page;
    
    /**
     * Constructs the exception.
     * 
     * @param msg
     * @param pageName  The name of the page which was modified
     */
    public RepositoryModifiedException( String msg, String pageName )
    {
        super( msg );
        
        m_page = pageName;
    }
    
    public String getPageName()
    {
        return m_page;
    }
}
