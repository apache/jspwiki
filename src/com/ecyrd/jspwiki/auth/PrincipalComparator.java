package com.ecyrd.jspwiki.auth;

import java.security.Principal;
import java.text.Collator;
import java.util.Comparator;

/**
 * Comparator class for sorting objects of type Principal.
 * Used for sorting arrays or collections of Principals.
 * @since 2.3
 */
public class PrincipalComparator implements Comparator 
{
    /**
     * @see java.util.Comparator#compare(Object, Object)
     */
    public int compare( Object o1, Object o2 )
    {
        Collator collator = Collator.getInstance();
        if ( o1 instanceof Principal && o2 instanceof Principal )
        {
            return collator.compare( ((Principal)o1).getName(), ((Principal)o2).getName() );
        }
        throw new ClassCastException( "Objects must be of type Principal.");
    }
      
}