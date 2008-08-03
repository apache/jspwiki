package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.*;

@UrlBinding("/Message.jsp")
public class MessageActionBean extends AbstractActionBean
{
    /**
     * Default event that forwards control to /Message.jsp.
     * @return the forward resolution
     */
    @DefaultHandler
    @HandlesEvent("message")
    @WikiRequestContext("message")
    public Resolution view() {
        return new ForwardResolution( "/Message.jsp" );
    }

}
