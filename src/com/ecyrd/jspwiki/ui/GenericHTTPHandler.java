package com.ecyrd.jspwiki.ui;

import com.ecyrd.jspwiki.WikiContext;

public interface GenericHTTPHandler
{
    
    /**
     *  Get an identifier for this particular AdminBean.  This id MUST
     *  conform to URI rules.  The ID must also be unique across all HTTPHandlers.
     *  
     *  @return
     */
    public String getId();
    
    /**
     *  Return basic HTML.
     *  
     *  @param context
     *  @return
     */
    public String doGet( WikiContext context );
    
    /**
     *  Handles a POST response.
     *  @param context
     *  @return
     */
    public String doPost( WikiContext context );
}
