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
package com.ecyrd.jspwiki.filters;

import java.util.Properties;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.filters.BasicPageFilter;
import com.ecyrd.jspwiki.filters.FilterException;
import com.ecyrd.jspwiki.parser.CreoleToJSPWikiTranslator;

/**
 * <p>Provides the Implementation for mixed mode creole: If you activate
 * this filter, it will translate all markup that was saved as creole
 * markup to JSPWiki markup. Therefore the files will be saved 
 * with mixed markup.
 * <p>
 * <b>WARNING</b>: There's no turning back after insalling this
 * filter. Since your wiki pages are saved in Creole markup you can
 * not deactivate it afterwards.
 * <p>
 * <b>WARNING</b>: This feature is completely experimental, and is known to be
 * broken.  Use at your own risk.
 * <p>
 * <b>WARNING</b>: The CreoleFilter feature is deprecated.  JSPWiki is likely
 * to implement a non-mixed mode Creole at some point, since turning on
 * Creole will make new pages obsolete.
 * 
 * @author Steffen Schramm
 * @author Hanno Eichelberger
 * @author Christoph Sauer
 * 
 * @see <a href="http://www.wikicreole.org/wiki/MixedMode">[[WikiCreole:MixedMode]]</a> 
 */

public class CreoleFilter extends BasicPageFilter 
{
    /**
     *  {@inheritDoc}
     */
    public void initialize(WikiEngine engine, Properties props) throws FilterException 
    {
    }

    /**
     *  {@inheritDoc}
     */
    public String preSave( WikiContext wikiContext, String content )
    throws FilterException
    {
        try 
        {
            String username=wikiContext.getCurrentUser().getName();
            Properties prop = wikiContext.getEngine().getWikiProperties();
            return new CreoleToJSPWikiTranslator().translateSignature(prop, content,username);
        }
        catch(Exception e )
        {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     *  {@inheritDoc}
     */

    public String preTranslate(WikiContext wikiContext, String content)
        throws FilterException 
    {
        try
        {
            Properties prop = wikiContext.getEngine().getWikiProperties();
            return new CreoleToJSPWikiTranslator().translate(prop ,content);
            
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            return content
                   + "\n \n %%error \n"
                   + "[CreoleFilterError]: This page was not translated by the CreoleFilter due to "
                   + "the following error: " + e.getMessage() + "\n \n"
                   + "%%\n \n";
        }
    }

}