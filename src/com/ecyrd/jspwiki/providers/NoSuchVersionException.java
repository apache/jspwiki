package com.ecyrd.jspwiki.providers;

/**
 *  Indicates that an non-existing version was specified.
 */
public class NoSuchVersionException
    extends ProviderException
{
    public NoSuchVersionException( String msg )
    {
        super( msg );
    }
}
