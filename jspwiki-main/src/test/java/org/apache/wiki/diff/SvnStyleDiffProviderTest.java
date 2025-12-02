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
package org.apache.wiki.diff;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.spi.Wiki;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;


public class SvnStyleDiffProviderTest {


    @Test
    public void testNoChanges() throws IOException, WikiException {
        diffTest(  "", "",0);
        diffTest(  "A", "A",0);
        diffTest(  "A B", "A B",0);

        diffTest(  "      ", "      ",0);
        diffTest(  "A B  C", "A B  C",0);
        diffTest(  "A B   C", "A B   C",0);
    }

    @Test
    public void testSimpleInsertions() throws IOException, WikiException {
        // Ah, the white space trailing an insertion is tacked onto the insertion, this is fair, the
        // alternative would be to greedily take the leading whitespace before the insertion as part
        // of it instead, and that doesn't make any more or less sense. just remember this behaviour
        // when writing tests.

        // Simple inserts...
        diffTest(  "A C", "A B C", 1 );
        diffTest(  "A D", "A B C D", 1 );

        // Simple inserts with spaces...
        diffTest(  "A C", "A B  C", 1);
        diffTest(  "A C", "A B   C", 1 );
        diffTest(  "A C", "A B    C", 1 );

        // Just inserted spaces...
        diffTest(  "A B", "A  B", 1 );
        diffTest(  "A B", "A   B",1 );
        diffTest(  "A B", "A    B", 1 );
        diffTest(  "A B", "A     B", 1 );
    }

    @Test
    public void testSimpleDeletions() throws IOException, WikiException {
        // Simple deletes...
        diffTest(  "A B C", "A C", 1 );
        diffTest(  "A B C D", "A D", 1 );

        // Simple deletes with spaces...
        diffTest(  "A B  C", "A C", 1 );
        diffTest(  "A B   C", "A C", 1 );

        // Just deleted spaces...
        diffTest(  "A  B", "A B", 1 );
        diffTest(  "A   B", "A B", 1 );
        diffTest(  "A    B", "A B", 1 );
    }

    @Test
    public void testContextLimits() throws IOException, WikiException {
        // No change
        diffTest(  "A B C D E F G H I", "A B C D E F G H I", 0 );
        //TODO Hmm, should the diff provider instead return the string, "No Changes"?

        // Bad property value, should default to huge context limit and return entire string.
        diffTest(  "A B C D E F G H I", "A B C D F G H I", 1 );

        // One simple deletion, limit context to 2...
        diffTest(  "A B C D E F G H I", "A B C D F G H I", 1);

        // Deletion of first element, limit context to 2...
        diffTest(  "A B C D E", "B C D E", 1);

        // Deletion of last element, limit context to 2...
        diffTest( "A B C D E", "A B C D ", 1 );

        // Two simple deletions, limit context to 2...
        diffTest( "A B C D E F G H I J K L M N O P", "A B C E F G H I J K M N O P", 1 );
    }

    @Test
    public void testMultiples() throws IOException, WikiException {
        diffTest(  "A F", "A B C D E F", 1 );
        diffTest(  "A B C D E F", "A F", 1 );
    }

    @Test
    public void testSimpleChanges() throws IOException, WikiException {
        // *changes* are actually an insert and a delete in the output...

        //single change
        diffTest(  "A B C", "A b C", 1 );

        //non-consequtive changes...
        diffTest(  "A B C D E", "A b C d E", 1 );
    }

    // FIXME: This test Assertions.fails; must be enabled again asap.
    
    @Test
    public void testKnownProblemCases() throws Exception
    {
        //These all Assertions.fail...

        //make two consequtive changes
        diffTest( "A B C D", "A b c D", 1);
        //acually returns ->                 "A |^b^-B-| |^c^-C-| D"

        //collapse adjacent elements...
        diffTest( "A B C D", "A BC D", 1);
        //acually returns ->                "A |^BC^-B-| |-C -|D"


        //These Assertions.failures are all due to how we process the diff results, we need to collapse
        //adjacent edits into one...

    }
     

    private void diffTest( final String oldText, final String newText, int expected )
            throws IOException, WikiException {
        final SvnStyleDiffProvider diff = new SvnStyleDiffProvider();

        final Properties props = TestEngine.getTestProperties();
        
        diff.initialize( null, props );

        final TestEngine engine = new TestEngine( props );
        final Context ctx = Wiki.context().create( engine, Wiki.contents().page( engine, "Dummy" ) );
        final String actualDiff = diff.makeDiffHtml( ctx, oldText, newText );
        int actuals = StringUtils.countMatches(actualDiff, SvnStyleDiffProvider.CSS_DIFF_ADDED ) +
                StringUtils.countMatches(actualDiff, SvnStyleDiffProvider.CSS_DIFF_REMOVED );
        
        Assertions.assertEquals( expected*2, actuals , actualDiff);
    }

}
