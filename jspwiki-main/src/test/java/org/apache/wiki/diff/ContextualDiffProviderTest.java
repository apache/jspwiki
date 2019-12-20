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

import org.apache.log4j.PropertyConfigurator;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.WikiException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

public class ContextualDiffProviderTest
{
    /**
     * Sets up some shorthand notation for writing test cases.
     * <p>
     * The quick |^Brown Fox^-Blue Monster-| jumped over |^the^| moon.
     * <p>
     * Get it?
     */
    private void specializedNotation(ContextualDiffProvider diff)
    {
        diff.CHANGE_END_HTML = "|";
        diff.CHANGE_START_HTML = "|";

        diff.DELETION_END_HTML = "-";
        diff.DELETION_START_HTML = "-";

        diff.DIFF_END = "";
        diff.DIFF_START = "";

        diff.ELIDED_HEAD_INDICATOR_HTML = "...";
        diff.ELIDED_TAIL_INDICATOR_HTML = "...";

        diff.m_emitChangeNextPreviousHyperlinks = false;

        diff.INSERTION_END_HTML = "^";
        diff.INSERTION_START_HTML = "^";

        diff.LINE_BREAK_HTML = "";
        diff.ALTERNATING_SPACE_HTML = "_";
    }



    @Test
    public void testNoChanges() throws IOException, WikiException
    {
        diffTest(null, "", "", "");
        diffTest(null, "A", "A", "A");
        diffTest(null, "A B", "A B", "A B");

        diffTest(null, "      ", "      ", " _ _ _");
        diffTest(null, "A B  C", "A B  C", "A B _C");
        diffTest(null, "A B   C", "A B   C", "A B _ C");
    }



    @Test
    public void testSimpleInsertions() throws IOException, WikiException
    {
        // Ah, the white space trailing an insertion is tacked onto the insertion, this is fair, the
        // alternative would be to greedily take the leading whitespace before the insertion as part
        // of it instead, and that doesn't make any more or less sense. just remember this behaviour
        // when writing tests.

        // Simple inserts...
        diffTest(null, "A C", "A B C", "A |^B ^|C");
        diffTest(null, "A D", "A B C D", "A |^B C ^|D");

        // Simple inserts with spaces...
        diffTest(null, "A C", "A B  C", "A |^B _^|C");
        diffTest(null, "A C", "A B   C", "A |^B _ ^|C");
        diffTest(null, "A C", "A B    C", "A |^B _ _^|C");

        // Just inserted spaces...
        diffTest(null, "A B", "A  B", "A |^_^|B");
        diffTest(null, "A B", "A   B", "A |^_ ^|B");
        diffTest(null, "A B", "A    B", "A |^_ _^|B");
        diffTest(null, "A B", "A     B", "A |^_ _ ^|B");
    }



    @Test
    public void testSimpleDeletions() throws IOException, WikiException
    {
        // Simple deletes...
        diffTest(null, "A B C", "A C", "A |-B -|C");
        diffTest(null, "A B C D", "A D", "A |-B C -|D");

        // Simple deletes with spaces...
        diffTest(null, "A B  C", "A C", "A |-B _-|C");
        diffTest(null, "A B   C", "A C", "A |-B _ -|C");

        // Just deleted spaces...
        diffTest(null, "A  B", "A B", "A |-_-|B");
        diffTest(null, "A   B", "A B", "A |-_ -|B");
        diffTest(null, "A    B", "A B", "A |-_ _-|B");
    }



    @Test
    public void testContextLimits() throws IOException, WikiException
    {
        // No change
        diffTest("1", "A B C D E F G H I", "A B C D E F G H I", "A...");
        //TODO Hmm, should the diff provider instead return the string, "No Changes"?

        // Bad property value, should default to huge context limit and return entire string.
        diffTest("foobar", "A B C D E F G H I", "A B C D F G H I", "A B C D |-E -|F G H I");

        // One simple deletion, limit context to 2...
        diffTest("2", "A B C D E F G H I", "A B C D F G H I", "...D |-E -|F ...");

        // Deletion of first element, limit context to 2...
        diffTest("2", "A B C D E", "B C D E", "|-A -|B ...");

        // Deletion of last element, limit context to 2...
        diffTest("2", "A B C D E", "A B C D ", "...D |-E-|");

        // Two simple deletions, limit context to 2...
        diffTest("2", "A B C D E F G H I J K L M N O P", "A B C E F G H I J K M N O P",
            "...C |-D -|E ......K |-L -|M ...");

    }

    @Test
    public void testMultiples() throws IOException, WikiException
    {
        diffTest(null, "A F", "A B C D E F", "A |^B C D E ^|F");
        diffTest(null, "A B C D E F", "A F", "A |-B C D E -|F");

    }

    @Test
    public void testSimpleChanges() throws IOException, WikiException
    {
        // *changes* are actually an insert and a delete in the output...

        //single change
        diffTest(null, "A B C", "A b C", "A |^b^-B-| C");

        //non-consequtive changes...
        diffTest(null, "A B C D E", "A b C d E", "A |^b^-B-| C |^d^-D-| E");

    }

    // FIXME: This test Assertions.fails; must be enabled again asap.
    /*
    @Test
    public void testKnownProblemCases() throws NoRequiredPropertyException, IOException
    {
        //These all Assertions.fail...

        //make two consequtive changes
        diffTest(null, "A B C D", "A b c D", "A |^b c^-B C-| D");
        //acually returns ->                 "A |^b^-B-| |^c^-C-| D"

        //collapse adjacent elements...
        diffTest(null, "A B C D", "A BC D", "A |^BC^-B C-| D");
        //acually returns ->                "A |^BC^-B-| |-C -|D"


        //These Assertions.failures are all due to how we process the diff results, we need to collapse
        //adjacent edits into one...

    }
     */

    private void diffTest(String contextLimit, String oldText, String newText, String expectedDiff)
        throws IOException, WikiException
    {
        ContextualDiffProvider diff = new ContextualDiffProvider();

        specializedNotation(diff);

        Properties props = TestEngine.getTestProperties();
        if (null != contextLimit)
            props.put(ContextualDiffProvider.PROP_UNCHANGED_CONTEXT_LIMIT, contextLimit);

        diff.initialize(null, props);

        PropertyConfigurator.configure(props);
        TestEngine engine = new TestEngine(props);

        WikiContext ctx = new WikiContext( engine, new WikiPage(engine,"Dummy") );
        String actualDiff = diff.makeDiffHtml( ctx, oldText, newText);

        Assertions.assertEquals(expectedDiff, actualDiff);
    }

}
