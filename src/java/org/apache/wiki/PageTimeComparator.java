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
package org.apache.wiki;

import java.io.Serializable;
import java.util.*;

import org.apache.wiki.api.WikiPage;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;


/**
 *  Compares the lastModified date of its arguments.  Both o1 and o2 MUST
 *  be WikiPage objects, or else you will receive a ClassCastException.
 *  <p>
 *  If the lastModified date is the same, then the next key is the page name.
 *  If the page name is also equal, then returns 0 for equality.
 */
// FIXME: Does not implement equals().
// FIXME3.0: move to util package

public class PageTimeComparator
    implements Comparator<WikiPage>, Serializable
{
    private static final long serialVersionUID = 0L;

    static Logger log = LoggerFactory.getLogger( PageTimeComparator.class ); 

    /**
     *  {@inheritDoc}
     */
    public int compare( WikiPage w1, WikiPage w2 )
    {
        if( w1 == null || w2 == null ) 
        {
            log.error( "W1 or W2 is NULL in PageTimeComparator!");
            return 0; // FIXME: Is this correct?
        }

        Date w1LastMod = w1.getLastModified();
        Date w2LastMod = w2.getLastModified();

        if( w1LastMod == null )
        {
            log.error( "NULL MODIFY DATE WITH "+w1.getName() );
            return 0;
        }
        else if( w2LastMod == null )
        {
            log.error( "NULL MODIFY DATE WITH "+w2.getName() );
            return 0;
        }

        // This gets most recent on top
        int timecomparison = w2LastMod.compareTo( w1LastMod );

        if( timecomparison == 0 )
        {
            return w1.getName().compareTo( w2.getName() );
        }

        return timecomparison;
    }
}
