package org.apache.wiki.api.filters;

import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;


class FilterSupportOperations {

    private static final Logger LOG = Logger.getLogger( FilterSupportOperations.class );

    /**
     * Checks if a given object is using old, non public API for a given Filter. This check is two-fold:
     * <ul>
     *     <li>if the page filter class name starts with JSPWiki base package we assume we're using latest JSPWiki, and the public API</li>
     *     <li>we try to execute the old, non public API equivalent method passed as parameters. If it exists, well, it is using the
     *     old, non public API, and we return the method execution. In any other case we return null</li>
     * </ul>
     *
     * @param pf given object to check
     * @param method requested method
     * @param params class names denoting method parameters
     * @return old, non public api method if it exists, {@code null} otherwise
     */
    static Method methodOfNonPublicAPI( final PageFilter pf, final String method, final String... params ) {
        if( !pf.getClass().getName().startsWith( "org.apache.wiki" ) ) {
            try {
                final Class< ? >[] classes = new Class< ? >[ params.length ];
                for( int i = 0; i < params.length; i++ ) {
                    classes[ i ] = Class.forName( params[ i ] );
                }
                return pf.getClass().getMethod( method, classes );
            } catch( final ClassNotFoundException | NoSuchMethodException e ) {
                return null;
            }
        }

        return null;
    }

    static < R > R executePageFilterPhase( final Supplier< R > s, final Method m, final PageFilter pf, final Object... params ) {
        if( m != null ) {
            try {
                return ( R )m.invoke( pf, params );
            } catch( final IllegalAccessException | InvocationTargetException e ) {
                LOG.warn( "Problems using filter adapter: " + e.getMessage(), e );
            }
        }
        return s.get();
    }

}
