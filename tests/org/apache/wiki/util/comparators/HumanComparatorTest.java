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

package org.apache.wiki.util.comparators;

import java.util.Comparator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class HumanComparatorTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( HumanComparatorTest.class );
    }

    public void testCharOrder()
    {
        HumanComparator comparator = new HumanComparator();

        // Default order first
        assertTrue( comparator.compare( "a c", "a1c" ) < 0 );
        assertTrue( comparator.compare( "a1c", "abc" ) < 0 );

        // Now letters then numbers then other
        HumanComparator.CharType sortOrder[] = { HumanComparator.CharType.TYPE_LETTER, HumanComparator.CharType.TYPE_DIGIT,
                                                HumanComparator.CharType.TYPE_OTHER };
        comparator.setSortOrder( sortOrder );
        assertTrue( comparator.compare( "a c", "a1c" ) > 0 );
        assertTrue( comparator.compare( "a1c", "abc" ) > 0 );

        // Now numbers then letters then other
        sortOrder[0] = HumanComparator.CharType.TYPE_DIGIT;
        sortOrder[1] = HumanComparator.CharType.TYPE_LETTER;
        sortOrder[2] = HumanComparator.CharType.TYPE_OTHER;
        comparator.setSortOrder( sortOrder );
        assertTrue( comparator.compare( "a c", "a1c" ) > 0 );
        assertTrue( comparator.compare( "a1c", "abc" ) < 0 );

        // Finally try to break it
        try
        {
            sortOrder[0] = HumanComparator.CharType.TYPE_DIGIT;
            sortOrder[1] = HumanComparator.CharType.TYPE_DIGIT;
            sortOrder[2] = HumanComparator.CharType.TYPE_OTHER;
            comparator.setSortOrder( sortOrder );
            fail( "Expected IllegalArgumentException" );
        }
        catch( IllegalArgumentException e )
        {
            // All worked
        }
    }

    public void testCompare()
    {
        Comparator<String> comparator = new HumanComparator();

        assertTrue( comparator.compare( "abcd001", "ABCD001" ) > 0 );
        assertTrue( comparator.compare( "abcd001a", "ABCD001z" ) < 0 );
        assertTrue( comparator.compare( "abc8", "abcd1" ) < 0 );
        assertTrue( comparator.compare( "abc 8", "abc1" ) < 0 );
        assertTrue( comparator.compare( "abc  8", "abc 1" ) < 0 );
        assertTrue( comparator.compare( "abdc001", "ABCD001" ) > 0 );
        assertTrue( comparator.compare( "ab cd001", "ABDC001" ) < 0 );
        assertTrue( comparator.compare( "10", "01" ) > 0 );
        assertTrue( comparator.compare( "10", "00000001" ) > 0 );
        assertTrue( comparator.compare( "01", "00000001" ) < 0 );
    }
}