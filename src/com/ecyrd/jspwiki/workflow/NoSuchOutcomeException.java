package com.ecyrd.jspwiki.workflow;

/**
 * Exception thrown when an attempt is made to find an Outcome that does not
 * exist.
 *
 * @author Andrew Jaquith
 */
public class NoSuchOutcomeException extends Exception
{

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception.
     * @param message the message
     */
    public NoSuchOutcomeException(String message)
    {
        super(message);
    }
}
