/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.tags;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiContext;
import org.apache.log4j.Category;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Implement a "breadcrumb" (most recently visited) trail.  This tag can be added to any view jsp page.
 * Separate breadcrumb trails are not tracked across multiple browser windows.<br>
 * The optional attributes are:
 * <p>
 * <b>maxpages</b>, the number of pages to store, 10 by default<br>
 * <b>separator</b>, the separator string to use between pages, " | " by default<br>
 * </p>
 *
 * <p>
 * This class is implemented by storing a breadcrumb trail, which is a 
 * fixed size queue, into a session variable "breadCrumbTrail".  
 * This queue is displayed as a series of links separated by a separator
 * character.
 * </p>
 * @author Ken Liu ken@kenliu.net
 */
public class BreadcrumbsTag extends WikiTagBase
{
    private static final Category log = Category.getInstance(BreadcrumbsTag.class);
    private static final String BREADCRUMBTRAIL_KEY = "breadCrumbTrail";
    private int m_maxsize = 10;
    private String m_separator = " | ";

    public int getMaxpages()
    {
        return m_maxsize;
    }

    public void setMaxpages(int maxpages)
    {
        m_maxsize = maxpages;
    }

    public String getSeparator()
    {
        return m_separator;
    }

    public void setSeparator(String separator)
    {
        m_separator = separator;
    }

    public final int doWikiStartTag() throws IOException
    {
        HttpSession session = pageContext.getSession();
        FixedQueue  trail   = (FixedQueue) session.getAttribute(BREADCRUMBTRAIL_KEY);

        String page = m_wikiContext.getPage().getName();

        if( trail == null )
        {
            trail = new FixedQueue(m_maxsize);
        }

        if( m_wikiContext.getRequestContext().equals( WikiContext.VIEW ) )
        {
            if( trail.isEmpty() )
            {
                trail.push(page);
            }
            else
            {
                //
                // Don't add the page to the queue if the page was just refreshed
                //
                if( !((String) trail.getLast()).equals(page) )
                {
                    trail.push(page);
                    log.debug("added page: " + page);
                }
                log.debug("didn't add page because of refresh");
            }
        }

        session.setAttribute(BREADCRUMBTRAIL_KEY, trail);

        //
        //  Print out the breadcrumb trail
        //

        // FIXME: this code would be much simpler if we could just output the [pagename] and then use the
        // wiki engine to output the appropriate wikilink

        JspWriter out     = pageContext.getOut();
        int queueSize     = trail.size();
        String linkclass  = "wikipage";
        WikiEngine engine = m_wikiContext.getEngine();
        String curPage    = null;

        for( int i = 0; i < queueSize - 1; i++ )
        {
            curPage = (String) trail.get(i);

            //FIXME: I can't figure out how to detect the appropriate jsp page to put here, so I hard coded Wiki.jsp
            //This breaks when you view an attachment metadata page
            out.print("<A CLASS=\"" + linkclass + "\" HREF=\"" + 
                      engine.getViewURL(curPage)+ "\">" + curPage + "</A>");

            if( i < queueSize - 2 ) 
            {
                out.print(m_separator);
            }
        }

        return SKIP_BODY;
    }

    /**
     * Extends the LinkedList class to provide a fixed-size queue implementation
     */
    private class FixedQueue extends LinkedList
    {
        private int m_size;

        FixedQueue(int size)
        {
            m_size = size;
        }

        Object push(Object o)
        {
            add(o);
            if( size() > m_size )
                return removeFirst();
            else
                return null;
        }
    }

}

