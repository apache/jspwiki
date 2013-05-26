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

import java.text.Collator;
import java.util.Locale;

/**
 * A comparator that sorts Strings using the locale's collator.
 * 
 */
public class LocaleComparator extends CollatorComparator
{
    /**
     * Default constructor uses the current locale's collator.
     */
    public LocaleComparator()
    {
        m_collator = Collator.getInstance();
    }

    /**
     * use a specific locale's collator.
     */
    public LocaleComparator( Locale locale )
    {
        m_collator = Collator.getInstance( locale );
    }

    /**
     * Specify a new locale.
     * 
     * @param locale the locale for future comparisons
     */
    public void setLocale( Locale locale)
    {
        m_collator = Collator.getInstance(locale);
    }
}
