package com.ecyrd.jspwiki.render;

import java.io.IOException;

import org.jdom.Document;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;

/**
 *  Provides an interface to the basic rendering engine.
 *  This class is an abstract class instead of an interface because
 *  it is expected that rendering capabilities are increased at some
 *  point, and I would hate if renderers broke.  This class allows
 *  some sane defaults to be implemented.
 *  
 *  @author jalkanen
 *
 */
public abstract class WikiRenderer
{
    protected WikiContext     m_context;
    protected WikiDocument    m_document;
    protected boolean         m_enablePlugins;
    
    protected WikiRenderer( WikiContext context, WikiDocument doc )
    {
        m_context = context;
        m_document = doc;

        //
        //  Do some sane defaults
        //
        WikiEngine engine = m_context.getEngine();
        String runplugins = engine.getVariable( m_context, JSPWikiMarkupParser.PROP_RUNPLUGINS );
        if( runplugins != null ) enablePlugins( TextUtil.isPositive(runplugins));
    }

    /**
     *  Can be used to turn on plugin execution on a translator-reader basis
     */
    public void enablePlugins( boolean toggle )
    {
        m_enablePlugins = toggle;
    }

    public abstract String getString()
        throws IOException;

}
