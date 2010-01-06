package org.apache.wiki.content.inspect;

/**
 * Thrown when an Inspection is interrupted by a {@link InspectionListener}.
 */
public class InspectionInterruptedException extends Exception
{
    private static final long serialVersionUID = -2989289836320240924L;

    private final InspectionListener m_source;

    /**
     * Constructs a new InspectionInterruptedException with a String message.
     * @param source the source of the interruption
     * @param message the message
     */
    public InspectionInterruptedException( InspectionListener source, String message )
    {
        super( message );
        m_source = source;
    }

    public InspectionListener getSource()
    {
        return m_source;
    }
}
