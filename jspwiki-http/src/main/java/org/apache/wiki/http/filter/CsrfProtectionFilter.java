package org.apache.wiki.http.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.Wiki;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * CSRF protection Filter which uses the synchronizer token pattern – an anti-CSRF token is created and stored in the
 * user session and in a hidden field on subsequent form submits. At every submit the server checks the token from the
 * session matches the one submitted from the form.
 */
public class CsrfProtectionFilter implements Filter {

    private static final Logger LOG = LogManager.getLogger( CsrfProtectionFilter.class );

    public static final String ANTICSRF_PARAM = "X-XSRF-TOKEN";

    /** {@inheritDoc} */
    @Override
    public void init( final FilterConfig filterConfig ) {
    }

    /** {@inheritDoc} */
    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain ) throws IOException, ServletException {
        if( "POST".equalsIgnoreCase( ( ( HttpServletRequest ) request ).getMethod() ) ) {
            final Engine engine = Wiki.engine().find( request.getServletContext(), null );
            final Session session = Wiki.session().find( engine, ( HttpServletRequest ) request );
            if( !session.antiCsrfToken().equals( request.getParameter( ANTICSRF_PARAM ) ) ) {
                LOG.error( "Incorrect {} param with value '{}' received for {}",
                           ANTICSRF_PARAM, request.getParameter( ANTICSRF_PARAM ), ( ( HttpServletRequest ) request ).getPathInfo() );
                final PrintWriter out = response.getWriter();
                out.print("<!DOCTYPE html><html lang=\"en\"><head><title>Fatal problem with JSPWiki</title></head>");
                out.print("<body>");
                out.print("<h1>CSRF injection detected</h1>");
                out.print("<p>A CSRF injection has been detected, so the request has been stopped</p>");
                out.print("<p>Please check your system logs to pinpoint the request origin, someone's trying to mess with your installation.</p>");
                out.print("</body></html>");
                return;
            }
        }
        chain.doFilter( request, response );
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
    }

}
