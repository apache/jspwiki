/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package com.ecyrd.jspwiki.tags;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.action.ViewActionBean;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
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
    private static final long serialVersionUID = 0L;

    private static final Logger log = Logger.getLogger(BreadcrumbsTag.class);
    private static final String BREADCRUMBTRAIL_KEY = "breadCrumbTrail";
    private int m_maxQueueSize = 11;
    private String m_separator = ", ";

    public void initTag()
    {
        super.initTag();
        m_maxQueueSize = 11;
        m_separator = ", ";
    }

    public int getMaxpages()
    {
        return m_maxQueueSize;
    }

    public void setMaxpages(int maxpages)
    {
        m_maxQueueSize = maxpages + 1;
    }

    public String getSeparator()
    {
        return m_separator;
    }

    public void setSeparator(String separator)
    {
        m_separator = separator;
    }

    public int doWikiStartTag() throws IOException
    {
        HttpSession session = pageContext.getSession();
        FixedQueue  trail   = (FixedQueue) session.getAttribute(BREADCRUMBTRAIL_KEY);

        if( trail == null )
        {
            trail = new FixedQueue(m_maxQueueSize);
        }

        if( m_actionBean instanceof ViewActionBean && m_page != null )
        {
            String pageName = m_page.getName();
            if( trail.isEmpty() )
            {
                trail.pushItem( pageName );
            }
            else
            {
                //
                // Don't add the page to the queue if the page was just refreshed
                //
                if( !((String) trail.getLast()).equals( pageName ) )
                {
                    trail.pushItem( pageName );
                    log.debug( "added page: " + pageName );
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
        String curPage    = null;

        for( int i = 0; i < queueSize - 1; i++ )
        {
            curPage = (String) trail.get(i);

            //FIXME: I can't figure out how to detect the appropriate jsp page to put here, so I hard coded Wiki.jsp
            //This breaks when you view an attachment metadata page
            if ( m_actionBean instanceof WikiContext )
            {
                WikiContext context = (WikiContext)m_actionBean;
                out.print("<a class=\"" + linkclass + "\" href=\"" + 
                          context.getViewURL(curPage)+ "\">" + curPage + "</a>");
            }
            
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
    public static class FixedQueue
        extends LinkedList<Object>
        implements Serializable
    {
        private int m_size;
        private static final long serialVersionUID = 0L;

        FixedQueue(int size)
        {
            m_size = size;
        }

        Object pushItem(Object o)
        {
            add(o);
            if( size() > m_size )
            {
                return removeFirst();
            }

            return null;
        }
    }

}

