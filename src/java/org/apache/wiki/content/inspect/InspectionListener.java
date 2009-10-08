package org.apache.wiki.content.inspect;

/**
 * Listener that fires whenever a Finding is changed.
 */
public interface InspectionListener
{
    public void changedScore( Inspection inspection, Finding score ) throws InspectionInterruptedException;
}
