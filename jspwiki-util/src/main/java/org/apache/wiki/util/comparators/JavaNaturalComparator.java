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

package org.apache.wiki.util.comparators;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;

/**
 * A comparator that sorts Strings using Java's "natural" order.
 * 
 */
public class JavaNaturalComparator implements Comparator<String>
{
    // A special singleton instance for quick access
    public static final Comparator<String> DEFAULT_JAVA_COMPARATOR = new JavaNaturalComparator();

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( String str1, String str2 ) {
        if (StringUtils.equals( str1, str2 ) ) {
        	return 0; // the same object
        }
        if( str1 == null ) {
        	return -1; // str1 is null and str2 isn't so str1 is smaller
        }
        if( str2 == null ) {
        	return 1; // str2 is null and str1 isn't so str1 is bigger
        }
        return str1.compareTo( str2 );
    }
}
