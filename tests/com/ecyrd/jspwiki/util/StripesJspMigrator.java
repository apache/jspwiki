package com.ecyrd.jspwiki.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class StripesJspMigrator
{

    /**
     * @param args
     */
    public static void main( String[] args )
    {
        if( args.length != 2 )
        {
            throw new IllegalArgumentException( "Must supply source and destination directories." );
        }
        File src = new File( args[0] );
        File dest = new File( args[1] );
        if( !src.exists() )
        {
            throw new IllegalArgumentException( "Source directory " + src.getAbsolutePath() + " does not exist." );
        }
        if( !src.isDirectory() )
        {
            throw new IllegalArgumentException( "Source " + src.getAbsolutePath() + " is not a directory." );
        }
        if( !dest.exists() )
        {
            throw new IllegalArgumentException( "Destination directory " + dest.getAbsolutePath() + " does not exist!" );
        }
        if( !src.isDirectory() )
        {
            throw new IllegalArgumentException( "Destination " + dest.getAbsolutePath() + " is not a directory." );
        }
        if( src.equals( dest ) )
        {
            throw new IllegalArgumentException( "Source and destination cannot be the same." );
        }
        StripesJspMigrator migrator = new StripesJspMigrator();
        try
        {
            migrator.migrate( src, dest );
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Migrates the contents of an entire directory from one location to
     * another.
     * 
     * @param srcDir the source directory
     * @param destDir the destination directory
     */
    public void migrate( File srcDir, File destDir ) throws IOException
    {
        // Create the destination directory if it does not already exist
        if( !destDir.exists() )
        {
            destDir.mkdir();
        }

        // Assemble list of files
        for( File src : srcDir.listFiles() )
        {
            // If directory, migrate everything in it recursively
            File dest = new File( destDir, src.getName() );
            if( src.isDirectory() )
            {
                migrate( src, dest );
            }

            // Otherwise it's a file, so migrate it if it's a JSP
            else
            {
                if( src.getName().endsWith( ".jsp" ) )
                {
                    migrateFile( src, dest );
                }
            }
        }
    }

    /**
     * Migrates a single file.
     * 
     * @param src the source file
     * @param dest the destination file
     */
    protected void migrateFile( File src, File dest ) throws IOException
    {
        // Read in the file
        System.out.println( "Migrating " + src.getPath() + " ----> " + dest.getPath() );
        String s = readSource( src );

        // Parse the contents of the file
        JspDocument doc = new JspDocument();
        doc.parse( s.toString() );
        for ( JspDocument.Node node : doc.getNodes() )
        {
            System.out.println( node.toString() );
        }

        // Write the migrated contents to disk
        writeDestination( dest, doc.toString() );
        System.out.println( "    done [" + s.length() + " chars]." );
    }

    /**
     * Writes a String to a file.
     * 
     * @param dest the destination file
     * @param contents the String to write to the file
     * @throws IOException
     */
    private void writeDestination( File dest, String contents ) throws IOException
    {
        FileWriter writer = new FileWriter( dest );
        writer.append( contents );
        writer.close();
    }
    
    protected String readSource( File src ) throws IOException
    {
        // Read in the file
        FileReader reader = new FileReader( src );
        StringBuffer s = new StringBuffer();
        int ch = 0;
        while ( (ch = reader.read()) != -1 )
        {
            s.append( (char) ch );
        }
        reader.close();
        return s.toString();
    }

}
