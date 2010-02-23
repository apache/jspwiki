package org.apache.wiki.action;

import java.util.*;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;

import org.apache.commons.lang.time.StopWatch;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.ui.stripes.EventResolution;

public class EditDialogActionBean extends AbstractActionBean
{
    private static final Logger log = LoggerFactory.getLogger( EditDialogActionBean.class );
    
    /**
     *  Provides a list of suggestions to use for a page name links.
     *  Currently the algorithm just looks into the value parameter,
     *  and returns all page names from that.
     *
     *  @param wikiName the page name
     *  @param maxLength maximum number of suggestions
     *  @return an EventResolution containing the suggestions as a JavaScript array of Strings
     */
    @DefaultHandler
    @HandlesEvent( "suggestions" )
    public Resolution suggestions( String wikiName, int maxLength )
    {
        StopWatch sw = new StopWatch();
        sw.start();
        List<String> list = new ArrayList<String>(maxLength);

        if( wikiName.length() > 0 )
        {
            
            // split pagename and attachment filename
            String filename = "";
            int pos = wikiName.indexOf("/");
            if( pos >= 0 ) 
            {
                filename = wikiName.substring( pos ).toLowerCase();
                wikiName = wikiName.substring( 0, pos );
            }
            
            String cleanWikiName = MarkupParser.cleanLink(wikiName).toLowerCase() + filename;

            String oldStyleName = MarkupParser.wikifyLink(wikiName).toLowerCase() + filename;

            Set<String> allPages;
            try
            {
                allPages = getContext().getEngine().getReferenceManager().findCreated();
            }
            catch( ProviderException e )
            {
                // FIXME: THis is probably not very smart.
                allPages = new TreeSet<String>();
            }

            int counter = 0;
            for( Iterator<String> i = allPages.iterator(); i.hasNext() && counter < maxLength; )
            {
                String p = i.next();
                String pp = p.toLowerCase();
                if( pp.startsWith( cleanWikiName) || pp.startsWith( oldStyleName ) )
                {
                    list.add( p );
                    counter++;
                }
            }
        }

        sw.stop();
        if( log.isDebugEnabled() )
        {
            log.debug("Suggestion request for "+wikiName+" done in "+sw );
        }
        
        return new EventResolution( getContext(), list );
    }
}
