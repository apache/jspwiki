/*
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
package org.apache.wiki.tags;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.util.TextUtil;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
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
 */
public class BreadcrumbsTag extends WikiTagBase
{
    private static final long serialVersionUID = 0L;

    private static final Logger log = Logger.getLogger(BreadcrumbsTag.class);
    /** The name of the session attribute representing the breadcrumbtrail */
    public static final String BREADCRUMBTRAIL_KEY = "breadCrumbTrail";
    private int m_maxQueueSize = 11;
    private String m_separator = ", ";

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initTag()
    {
        super.initTag();
        m_maxQueueSize = 11;
        m_separator = ", ";
    }

    /**
     *  Returns the maxpages.  This may differ from what was set by setMaxpages().
     *
     *  @return The current size of the pages.
     */
    public int getMaxpages()
    {
        return m_maxQueueSize;
    }

    /**
     *  Sets how many pages to show.
     *
     *  @param maxpages The amount.
     */
    public void setMaxpages(int maxpages)
    {
        m_maxQueueSize = maxpages + 1;
    }

    /**
     *  Get the separator string.
     *
     *  @return The string set in setSeparator()
     */
    public String getSeparator()
    {
        return m_separator;
    }

    /**
     *  Set the separator string.
     *
     *  @param separator A string which separates the page names.
     */
    public void setSeparator(String separator)
    {
        m_separator = TextUtil.replaceEntities( separator );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public int doWikiStartTag() throws IOException
    {
        HttpSession session = pageContext.getSession();
        FixedQueue  trail = (FixedQueue) session.getAttribute(BREADCRUMBTRAIL_KEY);

        String page = m_wikiContext.getPage().getName();

        if( trail == null )
        {
            trail = new FixedQueue(m_maxQueueSize);
        } else {
            //  check if page still exists (could be deleted/renamed by another user)
            for (int i = 0;i<trail.size();i++) {
                if (!m_wikiContext.getEngine().getPageManager().wikiPageExists(trail.get(i))) {
                    trail.remove(i);
                }
            }
        }

        if (m_wikiContext.getRequestContext().equals(WikiContext.VIEW))
        {
            if (m_wikiContext.getEngine().getPageManager().wikiPageExists(page))
            {
                if (trail.isEmpty())
                {
                    trail.pushItem(page);
                }
                else
                {
                    //
                    // Don't add the page to the queue if the page was just refreshed
                    //
                    if (!trail.getLast().equals(page))
                    {
                        trail.pushItem(page);
                    }
                }
            }
            else
            {
                log.debug("didn't add page because it doesn't exist: " + page);
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
            curPage = trail.get(i);

            //FIXME: I can't figure out how to detect the appropriate jsp page to put here, so I hard coded Wiki.jsp
            //This breaks when you view an attachment metadata page
            out.print("<a class=\"" + linkclass + "\" href=\"" + m_wikiContext.getViewURL(curPage)+ "\">"
                        + TextUtil.replaceEntities( curPage ) + "</a>");

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
        extends LinkedList<String>
        implements Serializable
    {
        private int m_size;
        private static final long serialVersionUID = 0L;

        FixedQueue(int size)
        {
            m_size = size;
        }

        String pushItem(String o)
        {
            add(o);
            if( size() > m_size )
            {
                return removeFirst();
            }

            return null;
        }

        /**
         * @param pageName
         *            the page to be deleted from the breadcrumb
         */
        public void removeItem(String pageName)
        {
            for (int i = 0; i < size(); i++)
            {
                String page = get(i);
                if (page != null && page.equals(pageName))
                {
                    remove(page);
                }
            }
        }

    }
}

