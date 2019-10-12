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

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;


/**
 * Unit tests corresponding to {@link PageTimeComparator}.
 */
public class PageTimeComparatorTest {

    TestEngine engine = TestEngine.build();
    PageTimeComparator comparator = new PageTimeComparator();
    WikiPage p1 = new WikiPage( engine, "A" );
    WikiPage p2 = new WikiPage( engine, "B" );
    WikiPage p3 = new WikiPage( engine, "A" );

    @Test
    void shouldCheckCompareByTimeGetsMoreRecentOnTop() {
        p1.setLastModified( new Date( 0L ) );
        p2.setLastModified( new Date( 1L ) );

        Assertions.assertEquals( 1, comparator.compare( p1, p2 ) );
        Assertions.assertEquals( -1, comparator.compare( p2, p1 ) );
    }

    @Test
    void shouldCheckCompareByEqualsTimeUsesPageName() {
        p1.setLastModified( new Date( 0L ) );
        p2.setLastModified( new Date( 0L ) );

        Assertions.assertEquals( -1, comparator.compare( p1, p2 ) );
    }

    @Test
    void shouldCheckCompareByEqualsTimeAndNameUsesPageVersion() {
        p1.setLastModified( new Date( 0L ) );
        p3.setLastModified( new Date( 0L ) );
        p1.setVersion( 1 );
        p3.setVersion( 2 );

        Assertions.assertEquals( -1, comparator.compare( p1, p3 ) );

        p3.setVersion( 1 );
        Assertions.assertEquals( 0, comparator.compare( p1, p3 ) );
    }

}
