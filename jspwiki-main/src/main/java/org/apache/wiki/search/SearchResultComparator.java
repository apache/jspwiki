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
package org.apache.wiki.search;

import java.io.Serializable;
import java.util.Comparator;


/**
 *  Simple class that decides which search results are more important than others.
 */
public class SearchResultComparator implements Comparator< SearchResult >, Serializable {
	
    private static final long serialVersionUID = 1L;

    /**
     *  Compares two SearchResult objects, returning the one that scored higher.
     *  
     *  {@inheritDoc}
     */
    public int compare( final SearchResult s1, final SearchResult s2 ) {
        // Bigger scores are first.
        int res = s2.getScore() - s1.getScore();
        if( res == 0 ) {
            res = s1.getPage().getName().compareTo( s2.getPage().getName() );
        }

        return res;
    }

}
