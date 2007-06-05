package com.ecyrd.jspwiki.filters;

import java.util.Properties;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.filters.BasicPageFilter;
import com.ecyrd.jspwiki.filters.FilterException;
import com.ecyrd.jspwiki.parser.CreoleToJSPWikiTranslator;

/**
 * Provides the Implementation for mixed mode creole: If you activate
 * this filter, it will translate all markup that was saved as creole
 * markup to JSPWiki markup. Therefore the files will be saved 
 * with mixed markup.
 * 
 * WARNING: There's no turning back after insalling this
 * filter. Since your wiki pages are saved in Creole markup you can
 * not deactivate it afterwards.
 * 
 * @author Steffen Schramm
 * @author Hanno Eichelberger
 * @author Christoph Sauer
 * 
 * @see <a href="http://www.wikicreole.org/wiki/MixedMode">[[WikiCreole:MixedMode]]</a> 
 */

public class CreoleFilter extends BasicPageFilter {

	public void initialize(WikiEngine engine, Properties props) throws FilterException {
	}
	
	public String preSave( WikiContext wikiContext, String content )
    throws FilterException
    {
		try {
			String username=wikiContext.getCurrentUser().getName();
            Properties prop = wikiContext.getEngine().getWikiProperties();
			return new CreoleToJSPWikiTranslator().translateSignature(prop, content,username);
		}catch(Exception e ){
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public String preTranslate(WikiContext wikiContext, String content)
			throws FilterException {

		try{
            Properties prop = wikiContext.getEngine().getWikiProperties();
			return new CreoleToJSPWikiTranslator().translate(prop ,content);
			
		} catch (Exception e) {
			e.printStackTrace();
			return content
					+ "\n \n %%error \n"
					+ "[CreoleFilterError]: This page was not translated by the CreoleFilter due to "
					+ "the following error: " + e.getMessage() + "\n \n"
					+ "%%\n \n";
		}

	}

}