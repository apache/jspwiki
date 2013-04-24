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
package org.apache.wiki.auth;

import java.io.Serializable;
import java.security.Principal;
import java.text.Collator;
import java.util.Comparator;

/**
 * Comparator class for sorting objects of type Principal.
 * Used for sorting arrays or collections of Principals.
 * @since 2.3
 */
public class PrincipalComparator
    implements Comparator<Principal>, Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Compares two Principal objects.
     * @param o1 the first Principal
     * @param o2 the second Principal
     * @return the result of the comparison
     * @see java.util.Comparator#compare(Object, Object)
     */
    public int compare( Principal o1, Principal o2 )
    {
        Collator collator = Collator.getInstance();
        return collator.compare( o1.getName(), o2.getName() );
    }

}
