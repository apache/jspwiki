package org.apache.wiki.content;

import org.apache.wiki.api.WikiException;

/**
 * Exception indicating that a required request parameter was not supplied, for example
 * to SpamFilter.
 */
public class MissingParameterException extends WikiException
{
    private static final long serialVersionUID = 8665543487480429651L;

    /**
     * Constructs a new MissingParameterException
     * @param message the exception message
     * @param cause the cause of the exception, which may be <code>null</code>
     */
    public MissingParameterException( String message, Throwable cause )
    {
        super( message, cause );
    }
    
}
