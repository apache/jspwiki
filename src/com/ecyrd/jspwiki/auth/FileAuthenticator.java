/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth;


import java.util.*;
import java.io.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.*;

/**
 * FileAuthenticator reads usernames and passwords from a file
 * named in the init properties by variable "jspwiki.authenticator.file".
 * Auth information is not cached; this authenticator is not suited for
 * a large user base. 
 *
 * <P>If the file name does not start with a slash, it is assumed
 * to be in WikiRootPath/WEB-INF.
 *
 * <P>Since the file is read every time an authentication is requested, 
 * chages are immediately visible.
 * 
 * <P>FileAuthenticator implementations should be able to parse only
 * the correct lines and discard any extraneous information in the
 * data file. This lets us combine user/passwd/role/permission 
 * information in one file.
 *
 * <P>This class may be expanded to read an XML access definition file,
 * such as that used by Tomcat, in the future. Currently, however, the
 * format is line-based, with each line consisting of the user name, 
 * password, and optional information, like this:
 *
 * <PRE>
 * # comments start with a hash
 * user name password other,information,may,follow
 * </PRE>
 *
 * <P>The optional information will, quite probably, be the users, roles, but
 * this class does not handle them. 
 * Any line NOT containing colons will be skipped when parsing auth information.
 */
public class FileAuthenticator
    implements WikiAuthenticator
{
    
    public static final String PROP_AUTHFILE = Authenticator.PROP_AUTHENTICATOR + ".file";
    public static final String ENTRY_USER = "user";
    public static final String ENTRY_COMMENT = "#";

    static Category log = Category.getInstance( FileAuthenticator.class );

    private File m_authFile;

    public FileAuthenticator()
    {
    }

    /**
     * Determines the file to use for authentication.
     */
    public void initialize( Properties props )
        throws NoRequiredPropertyException
    {

        String fileName = props.getProperty( PROP_AUTHFILE );
        if( fileName == null )
        {
            log.error( "No '" + PROP_AUTHFILE + "' defined in jspwiki.properties. " +
                       "No-one will be able to authenticate." );
            return;
        }
        
        fileName = fileName.trim();

        File testFile = new File( fileName );
        if( testFile.isFile() && testFile.canRead() )
        {
            m_authFile = testFile;
            log.info( "FileAuthenticator initialized, using " + fileName );
        }
    }
    
    /**
     * Attempts to authenticate the given WikiUserPrincipal.
     * Expects to find the name and password fields set, and uses them
     * to compare to the stored password entry. Reads the auth file, 
     * locates a line matching the uid, and compares the password. 
     *
     * <p>If the password matches, calls setValidated( true ) on the 
     * UserPrincipal and returns true. If not, or if a null object is
     * offered as parameter, returns false.
     */
    public boolean authenticate( UserProfile prospect )
    {
        if( prospect == null )
            return( false );

        String uid = prospect.getName();
        String passwd = prospect.getPassword();

        if( m_authFile != null )
        {
            String line = findUidEntry( uid );
            if( line == null )
            {
                log.debug( "No entry for " + uid + " in " + m_authFile.getName() );
                prospect.setInfo( Authenticator.MSG_AUTH, "Unknown user." );
                return( false );
            }
            String hashedPasswd = parseHashedPassword( line );
            if( hashedPasswd == null )
            {
                log.debug( "No password defined for " + uid + " in " + 
                           m_authFile.getName() + ". Access denied." );
                prospect.setInfo( Authenticator.MSG_AUTH, "Invalid user." );
                return( false );
            }

            if( Crypt.match( passwd, hashedPasswd ) )
            {
                prospect.setStatus( UserProfile.VALIDATED );
                prospect.removeInfo( Authenticator.MSG_AUTH );
                return( true );
            }
            else
            {
                prospect.setInfo( Authenticator.MSG_AUTH, "Invalid password." );
                log.debug( "Invalid password entered by " + uid + ". Access denied." );
            }

            return( false );
        }

        log.error( "No auth file initialized!" );
        prospect.setInfo( Authenticator.MSG_AUTH, "Access closed." );
        return( false );
    }


    /**
     * Parses a password entry out of a string "uid passwd ..."
     * Returns the password portion, or null if it is not defined.
     */
    public String parseHashedPassword( String line )
    {
        if( line == null )
            return( null );

        try
        {
            StringTokenizer tok = new StringTokenizer( line );
            String name = tok.nextToken();
            String passwd = tok.nextToken().trim();
            log.debug( "Parsed password " + passwd + " from line " + line );
            return( passwd.trim() );
        }
        catch( NoSuchElementException nsee )
        {
            log.error( "Invalid user entry '" + line + "'" );
        }

        return( null );
    }


    /**
     * Searches the auth file for an entry starting with uid 
     * and containing at least one colon. Returns a string of
     * format "uid passwd other_information", where the spaces
     * may be whitespace.
     */
    protected String findUidEntry( String uid )
    {
        String line = null;
        try
        {
            BufferedReader in = new BufferedReader( new FileReader( m_authFile ) );
            while( (line = in.readLine()) != null )
            {
                line = line.trim();

                if( line.startsWith( ENTRY_COMMENT ) )
                    continue;

                if( line.startsWith( ENTRY_USER ) )
                {
                    line = line.substring( ENTRY_USER.length() ).trim();
                    if( line.startsWith( uid ) && Character.isWhitespace( line.charAt(uid.length()) ) )
                    {
                        in.close();
                        return( line );
                    }
                }
            }
            in.close();
            return( null );
        }
        catch( IOException ioe )
        {
            log.error( "Unable to scan auth file " + m_authFile.getName(), ioe );
        }
        catch( NullPointerException npe )
        {
            log.error( "Error parsing authentication file " + m_authFile.getName() +
                       "line \"" + line + "\"", npe );
        }
        return( null );
    }

} //EOF



