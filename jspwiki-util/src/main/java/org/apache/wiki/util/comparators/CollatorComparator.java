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

import java.text.Collator;
import java.util.Comparator;

/**
 * A comparator that sorts Strings using a Collator. This class is needed
 * because, even though Collator implements
 * <code>Comparator&lt;Object&gt;</code> and the required
 * <code>compare(String, String)</code> method, you can't safely cast Collator
 * to <code>Comparator&lt;String&gt;</code>.
 * 
 */
public class CollatorComparator implements Comparator<String>
{
    // A special singleton instance for quick access
    public static final Comparator<String> DEFAULT_LOCALE_COMPARATOR = new CollatorComparator();

    protected Collator m_collator;

    /**
     * Default constructor uses the current locale's collator.
     */
    public CollatorComparator()
    {
        m_collator = Collator.getInstance();
    }

    /**
     * Construct with a specific collator.
     * 
     * @param collator the collator to be used for comparisons
     */
    public CollatorComparator( Collator collator )
    {
        m_collator = collator;
    }

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( String str1, String str2 )
    {
        if( StringUtils.equals( str1, str2 ) ) {
        	return 0; // the same object
        }
        if( str1 == null ) {
        	return -1; // str1 is null and str2 isn't so str1 is smaller
        }
        if( str2 == null ) {
        	return 1; // str2 is null and str1 isn't so str1 is bigger
        }
        return m_collator.compare( str1, str2 );
    }

    /**
     * Specify a new collator.
     * 
     * @param collator the collator to be used from now on
     */
    public void setCollator( Collator collator )
    {
        m_collator = collator;
    }
}
