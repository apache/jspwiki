/* 
   JSPWiki - a JSP-based WikiWiki clone.

   Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Category;

import com.ecyrd.jspwiki.FileUtil;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;

/**
 * This DiffProvider allows external command line tools to be used to generate
 * the diff.
 */
public class ExternalDiffProvider implements DiffProvider
{
    private static final Category log = Category.getInstance(ExternalDiffProvider.class);

    /**
     * Determines the command to be used for 'diff'. This program must be able
     * to output diffs in the unified format. For example 'diff -u %s1 %s2'.
     */
    public static final String PROP_DIFFCOMMAND = "jspwiki.diffCommand";

    private String m_diffCommand = null;
    private String m_encoding;
    
    //FIXME This could/should be a property for this provider, there is not guarentee that
    //the external program generates a format suitible for the colorization code of the 
    //TraditionalDiffProvider, currently set to true for legacy compatibility.  
    //I don't think this 'feature' ever worked right, did it?...
    private boolean m_traditionalColorization = true; 


    public ExternalDiffProvider()
    {
    }

    /**
     * @see com.ecyrd.jspwiki.WikiProvider#getProviderInfo()
     */
    public String getProviderInfo()
    {
        return "ExternalDiffProvider";
    }

    /**
     * @see com.ecyrd.jspwiki.WikiProvider#initialize(com.ecyrd.jspwiki.WikiEngine, java.util.Properties)
     */
    public void initialize(WikiEngine engine, Properties properties)
	throws NoRequiredPropertyException, IOException
    {
        m_diffCommand = properties.getProperty(PROP_DIFFCOMMAND);
        
        if ((null == m_diffCommand) || (m_diffCommand.trim().equals("")))
            throw new NoRequiredPropertyException("ExternalDiffProvider missing required property", PROP_DIFFCOMMAND);
        
        m_encoding = engine.getContentEncoding();
    }
    
    
    /**
     * Makes the diff by calling "diff" program.
     */
    public String makeDiffHtml(String p1, String p2)
    {
        File f1 = null, f2 = null;
        String diff = null;

        try
        {
            f1 = FileUtil.newTmpFile(p1, m_encoding);
            f2 = FileUtil.newTmpFile(p2, m_encoding);

            String cmd = TextUtil.replaceString(m_diffCommand, "%s1", f1.getPath());
            cmd = TextUtil.replaceString(cmd, "%s2", f2.getPath());

            String output = FileUtil.runSimpleCommand(cmd, f1.getParent());

            // FIXME: Should this rely on the system default encoding?
            String rawWikiDiff = new String(output.getBytes("ISO-8859-1"), m_encoding);
            
            String htmlWikiDiff = TextUtil.replaceEntities( rawWikiDiff );

            if (m_traditionalColorization) //FIXME, see comment near declaration...
            	diff = TraditionalDiffProvider.colorizeDiff(diff);
            else
                diff = htmlWikiDiff;
            
        }
        catch (IOException e)
        {
            log.error("Failed to do file diff", e);
        }
        catch (InterruptedException e)
        {
            log.error("Interrupted", e);
        }
        finally
        {
            if (f1 != null)
                f1.delete();
            if (f2 != null)
                f2.delete();
        }

        return diff;
    }


}
