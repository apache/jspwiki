
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
