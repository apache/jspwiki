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

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Includes body, if the request context matches.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class CheckRequestContextTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    private String m_context;
    private String[] m_contextList = {};


    public void initTag()
    {
        super.initTag();
        m_context = null;
        m_contextList = new String[0];
    }
    
    public String getContext()
    {
        return m_context;
    }

    public void setContext( String arg )
    {
        m_context = arg;
        
        m_contextList = StringUtils.split(arg,'|');
    }

    public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        for(int i = 0; i < m_contextList.length; i++ )
        {
            String ctx = m_wikiContext.getRequestContext();
            
            String checkedCtx = m_contextList[i];

            if( checkedCtx.length() > 0 )
            {
                if( checkedCtx.charAt(0) == '!' )
                {
                    if( !ctx.equalsIgnoreCase(checkedCtx.substring(1) ) )
                    {
                        return EVAL_BODY_INCLUDE;
                    }
                }
                else if( ctx.equalsIgnoreCase(m_contextList[i]) )
                {
                    return EVAL_BODY_INCLUDE;
                }
            }
        }

        return SKIP_BODY;
    }
}
