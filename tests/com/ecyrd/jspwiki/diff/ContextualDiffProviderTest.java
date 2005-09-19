package com.ecyrd.jspwiki.diff;

import java.io.IOException;
import java.util.Properties;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.dav.DavPathTest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ContextualDiffProviderTest extends TestCase
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
        diff.m_changeEndHtml = "|";
        diff.m_changeStartHtml = "|";

        diff.m_deletionEndHtml = "-";
        diff.m_deletionStartHtml = "-";

        diff.m_diffEnd = "";
        diff.m_diffStart = "";

        diff.m_elidedHeadIndicatorHtml = "...";
        diff.m_elidedTailIndicatorHtml = "...";

        diff.m_emitChangeNextPreviousHyperlinks = false;

        diff.m_insertionEndHtml = "^";
        diff.m_insertionStartHtml = "^";

        diff.m_lineBreakHtml = "";
        diff.m_alternatingSpaceHtml = "_";
    }



    public void testNoChanges() throws NoRequiredPropertyException, IOException
    {
        diffTest(null, "", "", "");
        diffTest(null, "A", "A", "A");
        diffTest(null, "A B", "A B", "A B");

        diffTest(null, "      ", "      ", " _ _ _");
        diffTest(null, "A B  C", "A B  C", "A B _C");
        diffTest(null, "A B   C", "A B   C", "A B _ C");
    }



    public void testSimpleInsertions() throws NoRequiredPropertyException, IOException
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



    public void testSimpleDeletions() throws NoRequiredPropertyException, IOException
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



    public void testContextLimits() throws NoRequiredPropertyException, IOException
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

    public void testMultiples() throws NoRequiredPropertyException, IOException
    {
        diffTest(null, "A F", "A B C D E F", "A |^B C D E ^|F");
        diffTest(null, "A B C D E F", "A F", "A |-B C D E -|F");
        
    }

    public void testSimpleChanges() throws NoRequiredPropertyException, IOException
    {
        // *changes* are actually an insert and a delete in the output...

        //single change
        diffTest(null, "A B C", "A b C", "A |^b^-B-| C");

        //non-consequtive changes...
        diffTest(null, "A B C D E", "A b C d E", "A |^b^-B-| C |^d^-D-| E");

    }

    public void testKnownProblemCases() throws NoRequiredPropertyException, IOException
    {
        //These all fail...
        
        //make two consequtive changes
        diffTest(null, "A B C D", "A b c D", "A |^b c^-B C-| D");
        //acually returns ->                 "A |^b^-B-| |^c^-C-| D"

        //collapse adjacent elements...
        diffTest(null, "A B C D", "A BC D", "A |^BC^-B C-| D");
        //acually returns ->                "A |^BC^-B-| |-C -|D"
        
        
        //These failures are all due to how we process the diff results, we need to collapse 
        //adjacent edits into one...
        
    }

    private void diffTest(String contextLimit, String oldText, String newText, String expectedDiff)
        throws NoRequiredPropertyException, IOException
    {
        ContextualDiffProvider diff = new ContextualDiffProvider();

        specializedNotation(diff);

        Properties props = new Properties();
        if (null != contextLimit)
            props.put(ContextualDiffProvider.PROP_UNCHANGED_CONTEXT_LIMIT, contextLimit);

        diff.initialize(null, props);

        String actualDiff = diff.makeDiffHtml(oldText, newText);

        assertEquals(expectedDiff, actualDiff);
    }

    public static Test suite()
    {
        return new TestSuite( ContextualDiffProviderTest.class );
    }

}
