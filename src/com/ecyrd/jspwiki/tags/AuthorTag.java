package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import java.io.StringReader;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.TranslatorReader;

/**
 *  Writes the author name of the current page.
 */
public class AuthorTag
    extends WikiTagBase
{
    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        String author = page.getAuthor();

        if( author != null )
        {
            if( engine.pageExists(author) )
            {
                // FIXME: It's very boring to have to do this.
                //        Slow, too.

                TranslatorReader tr = new TranslatorReader( m_wikiContext, 
                                                            new StringReader("") );
                author = tr.makeLink( TranslatorReader.READ,
                                      author,
                                      author );
            }
            pageContext.getOut().print( author );

        }
        else
        {
            pageContext.getOut().print( "unknown" );
        }

        return SKIP_BODY;
    }
}
