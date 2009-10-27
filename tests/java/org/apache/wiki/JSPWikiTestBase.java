package org.apache.wiki;

import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 *  Provides a base class for all JSPWiki tests.  Any tests which want to throw a NotExecutableException
 *  should extend from this class.
 *  <p>
 *  Technique for ignoring non-executable tests picked from Apache Jackrabbit - thanks heaps guys!
 */
public class JSPWikiTestBase extends TestCase
{
    public void run(TestResult testResult) 
    {
        super.run(new JSPWikiTestResult(testResult));
    }
    
    
}
