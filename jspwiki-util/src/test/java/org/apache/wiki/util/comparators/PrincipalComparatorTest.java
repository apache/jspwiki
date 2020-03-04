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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.Principal;


public class PrincipalComparatorTest {

    @Test
    public void testComparatorOrder() {
        final Principal p1 = () -> "a";
        final Principal p2 = () -> "b";
        final PrincipalComparator pc = new PrincipalComparator();

        Assertions.assertEquals( -1, pc.compare( p1, p2 ) );
        Assertions.assertEquals( 0, pc.compare( p1, p1 ) );
        Assertions.assertEquals( 1, pc.compare( p2, p1 ) );

        Assertions.assertThrows( NullPointerException.class, () -> pc.compare( null, p1  ) );
        Assertions.assertThrows( NullPointerException.class, () -> pc.compare( p1, null  ) );
        Assertions.assertThrows( NullPointerException.class, () -> pc.compare( null, null  ) );
    }

}
