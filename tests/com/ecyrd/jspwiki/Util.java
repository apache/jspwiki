package com.ecyrd.jspwiki;

import java.util.*;

/**
 * Utilities for tests.
 */
public class Util
{
    /**
     * Check that a collection contains the required string.
     */
    static public boolean collectionContains( Collection container, 
					      String captive )
    {
	Iterator i = container.iterator();
	while( i.hasNext() )
	{
	    Object cap = i.next();
	    if( cap instanceof String && captive.equals( cap ) )
		return( true );
	}

	return( false );
    }
}
