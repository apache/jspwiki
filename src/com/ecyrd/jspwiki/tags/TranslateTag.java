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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.ecyrd.jspwiki.WikiContext;

import org.apache.log4j.Category;

/**
 *  Converts the body text into HTML content.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class TranslateTag
    extends BodyTagSupport
{
    static    Category    log = Category.getInstance( TranslateTag.class );

    public final int doAfterBody()
        throws JspException
    {
        try
        {
            WikiContext context = (WikiContext) pageContext.getAttribute( WikiTagBase.ATTR_CONTEXT,
                                                                          PageContext.REQUEST_SCOPE );
            BodyContent bc = getBodyContent();
            String wikiText = bc.getString();
            bc.clearBody();

            String result = context.getEngine().textToHTML( context, wikiText );

            getPreviousOut().write( result );
        }
        catch( Exception e )
        {
            log.error( "Tag failed", e );
            throw new JspException( "Tag failed, check logs: "+e.getMessage() );
        }

        return SKIP_BODY;
    }
}
