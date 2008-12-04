package com.ecyrd.jspwiki.ui.stripes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.exception.DefaultExceptionHandler;

/**
 * Stripes ExceptionHandler that catches exceptions of various types and returns appropriate
 * Resolutions.
 */
public class WikiExceptionHandler extends DefaultExceptionHandler
{

    /**
     * Catches any Exceptions not handled by other methods in this class, and prints a stack trace.
     * @param exception the exception caught by StripesFilter
     * @param req the current HTTP request
     * @param res the current HTTP response
     * @return always returns <code>null</code> (that is, it does not redirect)
     */
    public Resolution catchAll(Throwable exception, HttpServletRequest req, HttpServletResponse res)
    {
        exception.printStackTrace();
        return null;
    }
    
}
