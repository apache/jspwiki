package com.ecyrd.jspwiki.ui.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.WikiContext;

/**
 *  Describes an administrative bean.
 *  
 *  @author jalkanen
 *
 */
public interface AdminBean
{
    public static final int UNKNOWN = 0;
    public static final int EDITOR  = 1;
    
    /**
     *  Return a human-readable title for this AdminBean.
     *  
     *  @return
     */
    public String getTitle();
    
    /**
     *  Get an identifier for this particular AdminBean.  This id MUST
     *  conform to URI rules.
     *  
     *  @return
     */
    public String getId();
    
    public boolean isEnabled();
    
    /**
     *  Returns a type (UNKNOWN, EDITOR, etc).
     *  
     *  @return
     */
    public int getType();
    
    /**
     *  Return basic HTML.
     *  
     *  @param context
     *  @return
     */
    public String getHTML( WikiContext context );
    
    public String handlePost( WikiContext context, HttpServletRequest req, HttpServletResponse resp );
}
