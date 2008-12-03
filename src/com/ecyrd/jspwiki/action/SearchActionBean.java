package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.*;

@UrlBinding( "/Search.jsp" )
public class SearchActionBean extends AbstractActionBean
{
    @DefaultHandler
    @HandlesEvent( "find" )
    @WikiRequestContext( "find" )
    public Resolution view()
    {
        return new ForwardResolution( "/Search.jsp" );
    }
}
