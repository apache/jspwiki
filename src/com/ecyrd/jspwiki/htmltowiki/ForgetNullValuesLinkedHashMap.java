package com.ecyrd.jspwiki.htmltowiki;

import java.util.LinkedHashMap;

/**
 * A LinkedHashMap that does not put null values into the map.
 * 
 * @author Sebastian Baltes (sbaltes@gmx.com)
 */
public class ForgetNullValuesLinkedHashMap extends LinkedHashMap
{
    private static final long serialVersionUID = 0L;
    
    public Object put( Object key, Object value )
    {
        if( value != null )
        {
            return super.put( key, value );
        }
        
        return null;
    }
}
