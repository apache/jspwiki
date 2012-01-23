package org.apache.wiki.ui.stripes;

import net.sourceforge.stripes.util.ResolverUtil.Test;

/**
 * Implementation of {@link net.sourceforge.stripes.util.ResolverUtil.Test} used
 * with Stripes {@link net.sourceforge.stripes.util.ResolverUtil} to discover
 * classes.
 */
public class IsOneOf implements Test
{
    private final Class<?>[] m_parents;

    /**
     * Constructs a new IsOneOf ResolverUtil tester with a series of specified
     * types.
     * 
     * @param parentTypes a series of parent types that the type under test must
     *            match.
     */
    public IsOneOf( Class<?>... parentTypes )
    {
        super();
        m_parents = parentTypes;
    }

    /**
     * Returns true if type is assignable to one of the parent types supplied in
     * the constructor.
     */
    @SuppressWarnings( "unchecked" )
    public boolean matches( Class type )
    {
        if( type == null )
        {
            return false;
        }
        for( Class<?> parent : m_parents )
        {
            if( parent.isAssignableFrom( type ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "is assignable to " + m_parents;
    }
}
