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

package org.apache.wiki.ui.stripes;

import net.sourceforge.stripes.validation.OneToManyTypeConverter;

/**
 * More flexible version of
 * {@link net.sourceforge.stripes.validation.OneToManyTypeConverter} that allows
 * converted strings to be delimited by carriage return, carriage return +
 * linefeed, or commas. Leading and trailing whitespace after the CR or CR/LF is
 * trimmed.
 */
public class LineDelimitedTypeConverter extends OneToManyTypeConverter
{

    /**
     * Overrides the regular expression used by the 
     * {@link net.sourceforge.stripes.validation.OneToManyTypeConverter}
     * with one that permits line-break characters and commas.
     */
    @Override
    protected String getSplitRegex()
    {
        return "\\s*((\\r*?\\n)|,+)+?\\s*";
    }
}
