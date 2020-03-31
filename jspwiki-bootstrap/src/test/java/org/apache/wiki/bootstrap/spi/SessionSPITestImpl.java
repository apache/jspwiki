package org.apache.wiki.bootstrap.spi;

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.SessionSPI;

import javax.servlet.http.HttpServletRequest;


public class SessionSPITestImpl implements SessionSPI {

    @Override
    public void remove( final Engine engine, final HttpServletRequest request ) {

    }

    @Override
    public Session find( final Engine engine, final HttpServletRequest request ) {
        return null;
    }

    @Override
    public Session guest( final Engine engine ) {
        return null;
    }
}
