package org.apache.wiki.content.inspect;

/**
 * Thrown when an Inspection is interrupted by a {@link InspectionListener}.
 */
public class InspectionInterruptedException extends Exception
{
    private static final long serialVersionUID = -2989289836320240924L;

    /**
     * Constructs a new InspectionInterruptedException with a String message.
     * @param message the message
     */
    public InspectionInterruptedException( String message )
    {
        super( message );
    }
}
