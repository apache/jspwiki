package org.apache.wiki;

import java.util.Enumeration;

import junit.framework.*;

/**
 *  Provides a facade for TestResult which ignores all NotExecutableExceptions.
 */
public class JSPWikiTestResult extends TestResult
{
    private TestResult m_result;
    
    public JSPWikiTestResult( TestResult testResult )
    {
        m_result = testResult;
    }

    @Override
    public synchronized void addError( Test arg0, Throwable arg1 )
    {
        if( arg1 instanceof NotExecutableException )
            return;
        
        m_result.addError( arg0, arg1 );
    }

    @Override
    public synchronized void addFailure( Test arg0, AssertionFailedError arg1 )
    {
        m_result.addFailure( arg0, arg1 );
    }

    @Override
    public synchronized void addListener( TestListener listener )
    {
        m_result.addListener( listener );
    }

    @Override
    public void endTest( Test arg0 )
    {
        m_result.endTest( arg0 );
    }

    @Override
    public synchronized int errorCount()
    {
        return m_result.errorCount();
    }

    @Override
    public synchronized Enumeration errors()
    {
        return m_result.errors();
    }

    @Override
    public synchronized int failureCount()
    {
        return m_result.failureCount();
    }

    @Override
    public synchronized Enumeration failures()
    {
        return m_result.failures();
    }

    @Override
    public synchronized void removeListener( TestListener listener )
    {
        m_result.removeListener( listener );
    }

    @Override
    public synchronized int runCount()
    {
        return m_result.runCount();
    }

    @Override
    public synchronized boolean shouldStop()
    {
        return m_result.shouldStop();
    }

    @Override
    public void startTest( Test arg0 )
    {
        m_result.startTest( arg0 );
    }

    @Override
    public synchronized void stop()
    {
        m_result.stop();
    }

    @Override
    public synchronized boolean wasSuccessful()
    {
        return m_result.wasSuccessful();
    }

}
