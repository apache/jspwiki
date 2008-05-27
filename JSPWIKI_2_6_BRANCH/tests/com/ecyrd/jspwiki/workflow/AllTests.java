package com.ecyrd.jspwiki.workflow;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 */
public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Workflow tests" );
        suite.addTestSuite( ApprovalWorkflowTest.class );
        suite.addTestSuite( DecisionQueueTest.class );
        suite.addTestSuite( FactTest.class );
        suite.addTestSuite( OutcomeTest.class );
        suite.addTestSuite( SimpleDecisionTest.class );
        suite.addTestSuite( TaskTest.class );
        suite.addTestSuite( WorkflowManagerTest.class );
        suite.addTestSuite( WorkflowTest.class );
        return suite;
    }
}