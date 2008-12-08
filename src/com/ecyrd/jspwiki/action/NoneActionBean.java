package com.ecyrd.jspwiki.action;

import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;

/**
 * Represents a dummy WikiContext that doesn't bind to a URL, and doesn't
 * contain any embedded logic. When the NoneActionBean class is passed as a
 * parameter to a method that produces an URL, the resulting URL does not
 * prepend anything before the page.
 * 
 * @author Andrew Jaquith
 */
public class NoneActionBean extends AbstractPageActionBean
{
    @DefaultHandler
    @HandlesEvent( "none" )
    @WikiRequestContext( "none" )
    public Resolution view()
    {
        return null;
    }
}
