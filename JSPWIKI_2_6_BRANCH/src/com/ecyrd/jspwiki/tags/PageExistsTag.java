/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Includes the body in case the set page does exist.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */

// FIXME: Logically, this should probably be the master one, then
//        NoSuchPageTag should be the one that derives from this.
public class PageExistsTag
    extends NoSuchPageTag
{
    private static final long serialVersionUID = 0L;
    
    public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        return (super.doWikiStartTag() == SKIP_BODY) ? EVAL_BODY_INCLUDE : SKIP_BODY;
    }
}
