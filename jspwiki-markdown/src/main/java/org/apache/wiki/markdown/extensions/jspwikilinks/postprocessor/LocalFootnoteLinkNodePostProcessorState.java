package org.apache.wiki.markdown.extensions.jspwikilinks.postprocessor;

import org.apache.wiki.WikiContext;
import org.apache.wiki.markdown.nodes.JSPWikiLink;

import com.vladsch.flexmark.util.NodeTracker;
import com.vladsch.flexmark.util.sequence.CharSubSequence;


/**
 * {@link NodePostProcessorState} which further post processes local footnote links.
 */
public class LocalFootnoteLinkNodePostProcessorState implements NodePostProcessorState< JSPWikiLink > {

    final WikiContext wikiContext;

    public LocalFootnoteLinkNodePostProcessorState( final WikiContext wikiContext ) {
        this.wikiContext = wikiContext;
    }

    /**
     * {@inheritDoc}
     *
     * @see NodePostProcessorState#process(NodeTracker, JSPWikiLink)
     */
    @Override
    public void process( final NodeTracker state, final JSPWikiLink link ) {
        link.setUrl( CharSubSequence.of( wikiContext.getURL( WikiContext.VIEW, link.getUrl().toString() ) ) );
    }

}
