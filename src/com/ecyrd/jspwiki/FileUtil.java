package com.ecyrd.jspwiki;

import java.io.*;
import org.apache.log4j.Category;

public class FileUtil
{
    private static final Category   log = Category.getInstance(FileUtil.class);

    public static File newTmpFile( String content )
        throws IOException
    {
        File f = File.createTempFile( "jspwiki", null );

        StringReader in = new StringReader( content );

        FileWriter out = new FileWriter( f );

        int c;

        while( (c = in.read()) != -1 )
        {
            out.write( c );
        }

        out.close();

        return f;
    }

    public static String runSimpleCommand( String command, String directory )
        throws IOException,
               InterruptedException
    {
        StringBuffer result = new StringBuffer();        

        log.debug("Running simple command "+command+" in "+directory);
        String[] env = new String[0];

        Process process = Runtime.getRuntime().exec( command, env, new File(directory) );

        BufferedReader stdout = new BufferedReader( new InputStreamReader(process.getInputStream()) );

        String line;

        while( (line = stdout.readLine()) != null )
        { 
            result.append( line+"\n");
        }            

        process.waitFor();
        
        return result.toString();
    }
}
