package com.ecyrd.jspwiki.auth.modules;

import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.auth.*;
import org.apache.log4j.Category;

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

    static Category log = Category.getInstance( FileAuthenticator.class );

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
