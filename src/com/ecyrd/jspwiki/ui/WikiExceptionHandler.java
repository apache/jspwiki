package com.ecyrd.jspwiki.ui;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.exception.AutoExceptionHandler;

public class WikiExceptionHandler implements AutoExceptionHandler
{

    public Resolution handle(Throwable exception, HttpServletRequest req, HttpServletResponse res)
    {
        exception.printStackTrace();
        return null;
    }
    
}
