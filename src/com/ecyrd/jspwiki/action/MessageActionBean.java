package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.*;

@UrlBinding( "/Message.jsp" )
public class MessageActionBean extends AbstractActionBean
{
    private String m_message = null;

    /**
     * Default event that forwards control to /Message.jsp.
     * 
     * @return the forward resolution
     */
    @DefaultHandler
    @HandlesEvent( "message" )
    @WikiRequestContext( "message" )
    public Resolution view()
    {
        return new ForwardResolution( "/Message.jsp" );
    }

    public String getMessage()
    {
        return m_message;
    }

    public void setMessage( String message )
    {
        m_message = message;
    }

}
