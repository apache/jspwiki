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
package org.apache.wiki.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Page;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

/**
 *  Compares the lastModified date of its arguments.  Both o1 and o2 MUST be WikiPage objects, or else you will receive a ClassCastException.
 *  <p>
 *  If the lastModified date is the same, then the next key is the page name. If the page name is also equal, then returns 0 for equality.
 */
public class PageTimeComparator implements Comparator< Page >, Serializable {
	
    private static final long serialVersionUID = 0L;
    private static final Logger LOG = LogManager.getLogger( PageTimeComparator.class );

    /**
     *  {@inheritDoc}
     */
    @Override
    public int compare(final Page p1, final Page p2 ) {
        if( p1 == null || p2 == null ) {
            LOG.error( "W1 or W2 is NULL in PageTimeComparator!");
            return 0; // FIXME: Is this correct?
        }

        final Date w1LastMod = p1.getLastModified();
        final Date w2LastMod = p2.getLastModified();

        if( w1LastMod == null ) {
            LOG.error( "NULL MODIFY DATE WITH " + p1.getName() );
            return 0;
        } else if( w2LastMod == null ) {
            LOG.error( "NULL MODIFY DATE WITH " + p2.getName() );
            return 0;
        }

        // This gets most recent on top
        final int timecomparison = w2LastMod.compareTo( w1LastMod );
        if( timecomparison == 0 ) {
            return p1.compareTo( p2 );
        }

        return timecomparison;
    }

}
