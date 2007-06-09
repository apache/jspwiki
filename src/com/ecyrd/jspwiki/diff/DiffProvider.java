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
package com.ecyrd.jspwiki.diff;

import java.io.IOException;
import java.util.Properties;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiProvider;

/**
 * TODO 
 */
public interface DiffProvider extends WikiProvider
{
    /**
     * The return string is to be XHTML compliant ready to display html.  No further
     * processing of this text will be done by the wiki engine.
     */
    String makeDiffHtml(String oldWikiText, String newWikiText);
    
    
    public static class NullDiffProvider implements DiffProvider
    {
        public String makeDiffHtml(String oldWikiText, String newWikiText)
        {
            return "You are using the NullDiffProvider, check your properties file.";
        }

        public void initialize(WikiEngine engine, Properties properties) 
            throws NoRequiredPropertyException, IOException
        {
        }

        public String getProviderInfo()
        {
            return "NullDiffProvider";
        }
    }
    
}
