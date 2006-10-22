package com.ecyrd.jspwiki.auth.user;

/**
 * Exception indicating that an identical user already exists in the user
 * database.
 * @author Andrew Jaquith
 * @since 2.3
 */
public final class DuplicateUserException extends Exception
{
    private static final long serialVersionUID = 3258125851953674032L;

    public DuplicateUserException( String message )
    {
        super( message );
    }
}