package org.apache.wiki.api.spi;

import org.apache.wiki.api.core.Command;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;

import javax.servlet.http.HttpServletRequest;


public class ContextSPITestImpl implements ContextSPI {

    @Override
    public Context create( final Engine engine, final Page page ) {
        return null;
    }

    @Override
    public Context create( final Engine engine, final HttpServletRequest request, final Command command ) {
        return null;
    }

    @Override
    public Context create( final Engine engine, final HttpServletRequest request, final Page page ) {
        return null;
    }

    @Override
    public Context create( final Engine engine, final HttpServletRequest request, final String requestContext ) {
        return null;
    }

}
