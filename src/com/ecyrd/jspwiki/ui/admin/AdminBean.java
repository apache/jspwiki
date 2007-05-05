package com.ecyrd.jspwiki.ui.admin;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.ui.GenericHTTPHandler;

/**
 *  Describes an administrative bean.
 *  
 *  @author jalkanen
 *
 */
public interface AdminBean
    extends GenericHTTPHandler
{
    public static final int UNKNOWN = 0;
    public static final int CORE    = 1;
    public static final int EDITOR  = 2;
    
    public void initialize( WikiEngine engine );
    
    /**
     *  Return a human-readable title for this AdminBean.
     *  
     *  @return the bean's title
     */
    public String getTitle();
    
    /**
     *  Returns a type (UNKNOWN, EDITOR, etc).
     *  
     *  @return the bean's type
     */
    public int getType();
}
