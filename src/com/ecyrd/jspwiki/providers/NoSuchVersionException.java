package com.ecyrd.jspwiki.providers;

/**
 *  Indicates that an non-existing version was specified.
 */
public class NoSuchVersionException
    extends ProviderException
{
    private static final long serialVersionUID = 0L;
    
    public NoSuchVersionException( String msg )
    {
        super( msg );
    }
}
