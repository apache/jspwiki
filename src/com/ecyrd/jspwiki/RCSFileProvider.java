package com.ecyrd.jspwiki;

import java.util.Properties;
import org.apache.log4j.Category;
import java.io.File;

public class RCSFileProvider
    extends FileSystemProvider
{
    private String m_checkinCommand  = "ci -q -mx -l -t-none %s";
    private String m_checkoutCommand = "co -l %s";

    private static final Category   log = Category.getInstance(RCSFileProvider.class);

    public static final String    PROP_CHECKIN  = "jspwiki.rcsFileProvider.checkinCommand";
    public static final String    PROP_CHECKOUT = "jspwiki.rcsFileProvider.checkoutCommand";

    public void initialize( Properties props )
        throws NoRequiredPropertyException
    {
        log.debug("Initing RCS");
        super.initialize( props );

        m_checkinCommand = props.getProperty( PROP_CHECKIN, m_checkinCommand );
        m_checkoutCommand = props.getProperty( PROP_CHECKOUT, m_checkoutCommand );

        File rcsdir = new File( getPageDirectory(), "RCS" );

        if( !rcsdir.exists() )
            rcsdir.mkdirs();

        log.info("checkin="+m_checkinCommand);
        log.info("checkout="+m_checkoutCommand);
    }

    public void putPageText( String page, String text )
    {
        super.putPageText( page, text );

        log.debug( "Checking in text..." );

        try
        {
            String cmd = m_checkinCommand;
            String[] env = new String[0];

            cmd = TranslatorReader.replaceString( cmd, "%s", page );

            log.debug("Command = '"+cmd+"'");

            Process process = Runtime.getRuntime().exec( cmd, env, new File(getPageDirectory()) );

            process.waitFor();

            log.debug("Done, returned = "+process.exitValue());
        }
        catch( Exception e )
        {
            log.error("RCS checkin failed",e);
        }
    }
}
