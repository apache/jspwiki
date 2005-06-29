package com.ecyrd.jspwiki.auth.user;

/**
 * Exception indicating that an identical user already exists in the user
 * database.
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public final class DuplicateUserException extends Exception
{
    public DuplicateUserException( String message )
    {
        super( message );
    }
}