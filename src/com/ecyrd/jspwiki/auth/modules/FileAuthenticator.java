/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth.modules;

import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.auth.*;
import org.apache.log4j.Logger;

/**
 *  Provides a simple file-based authenticator.  This is really simple,
 *  as it does not even provide encryption for the passwords.
 *
 *  @author Janne Jalkanen
 *  @since  2.1.29.
 */
public class FileAuthenticator
    implements WikiAuthenticator
{
    public static final String PROP_FILENAME = "jspwiki.fileAuthenticator.fileName";

    private String m_fileName;

    static Logger log = Logger.getLogger( FileAuthenticator.class );

    public void initialize( Properties props )
        throws NoRequiredPropertyException
    {
        m_fileName = WikiEngine.getRequiredProperty( props, PROP_FILENAME );
    }

    private Properties readPasswords( String filename )
        throws IOException
    {
        Properties  props = new Properties();
        InputStream in    = null;

        try
        {
            File file = new File( filename );

            if( file != null && file.exists() )
            {
                in = new FileInputStream( file );

                props.load(in);

                log.debug("Loaded "+props.size()+" usernames.");
            }            
        }
        finally
        {
            if( in != null ) in.close();
        }

        return props;
    }

    public boolean authenticate( UserProfile wup )
    {
        if( wup == null || wup.getName() == null )
            return( false );

        try
        {
            Properties props = readPasswords( m_fileName );

            String userName = wup.getName();
            String password = wup.getPassword();

            String storedPassword = props.getProperty( userName );

            if( storedPassword != null && storedPassword.equals( password ) )
            {
                return true;
            }
        }
        catch( IOException e )
        {
            log.error("Unable to read passwords, disallowing login.",e);
        }

        return false;
    }

    public boolean canChangePasswords()
    {
        return false;
    }

    public void setPassword( UserProfile wup, String password )
    {
    }
}
