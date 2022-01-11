package org.apache.wiki.markdown.extensions.jspwikilinks.postprocessor;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import com.vladsch.flexmark.util.sequence.CharSubSequence;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.markdown.nodes.JSPWikiLink;


/**
 * {@link NodePostProcessorState} which further post processes local footnote links.
 */
public class LocalFootnoteLinkNodePostProcessorState implements NodePostProcessorState< JSPWikiLink > {

    final Context wikiContext;

    public LocalFootnoteLinkNodePostProcessorState( final Context wikiContext ) {
        this.wikiContext = wikiContext;
    }

    /**
     * {@inheritDoc}
     *
     * @see NodePostProcessorState#process(NodeTracker, Node) 
     */
    @Override
    public void process( final NodeTracker state, final JSPWikiLink link ) {
        link.setUrl( CharSubSequence.of( wikiContext.getURL( ContextEnum.PAGE_VIEW.getRequestContext(), link.getUrl().toString() ) ) );
    }

}
