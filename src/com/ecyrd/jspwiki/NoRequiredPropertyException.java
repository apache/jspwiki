package com.ecyrd.jspwiki;

public class NoRequiredPropertyException
    extends Exception
{
    public NoRequiredPropertyException( String msg, String key )
    {
        super(msg+": key="+key);
    }
}
