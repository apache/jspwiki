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

import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.VariableInfo;

/**
 *  Just provides the TEI data for IteratorTag.
 *
 *  @since 2.0
 */
public class SearchResultIteratorInfo extends TagExtraInfo
{
    public VariableInfo[] getVariableInfo(TagData data)
    {
        VariableInfo[] var = { new VariableInfo( data.getAttributeString("id"),
                                                 "com.ecyrd.jspwiki.SearchResult",
                                                 true,
                                                 VariableInfo.NESTED )
        };

        return var;

    }
}
