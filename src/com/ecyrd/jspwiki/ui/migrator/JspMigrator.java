package com.ecyrd.jspwiki.ui.migrator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.util.ResolverUtil;

import com.ecyrd.jspwiki.action.WikiContextFactory;

public class JspMigrator
{
    /**
     * Key for a list of File objects stored in the shared-state array.
     */
    protected static final String ALL_JSPS = "allJSPs";

    /**
     * Returns a list of files that match a specified extension, starting in a
     * supplied directory and scanning each child directory recursively..
     * 
     * @param dir the directory to start in
     * @param extension the extension to check, for example <code>.jsp</code>.
     *            If the extension is "*", all files are returned
     * @return the list of files
     * @throws IOException
     * @throws IllegalArgumentException if <code>dir</code> does not already
     *             exist, or if <code>extension</code>
     */
    public static List<File> getFiles( File dir, String extension ) throws IOException
    {
        // Verify parameters
        if( dir == null || extension == null )
        {
            throw new IllegalArgumentException( "Dir and extension must not be null." );
        }

        // Verify that the directory exists
        if( !dir.exists() )
        {
            throw new IllegalArgumentException( "Dir " + dir.getPath() + " does not exist!" );
        }

        List<File> allFiles = new ArrayList<File>();

        // Assemble list of files
        for( File file : dir.listFiles() )
        {
            // If directory, migrate everything in it recursively
            if( file.isDirectory() )
            {
                allFiles.addAll( getFiles( file, extension ) );
            }

            // Otherwise it's a file, so add it to the list
            else
            {
                if( "*".equals( extension ) || file.getName().endsWith( extension ) )
                {
                    allFiles.add( file );
                }
            }
        }
        return allFiles;
    }

    /**
     * Migrates a directory containing JSPs, and all of its subdirectories, to a
     * destination directory.
     * 
     * @param args two Strings specifying the source and destination
     *            directories, respectively. Both source and destination must
     *            already exist, and may not be the same.
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
        JspMigrator migrator = new JspMigrator();
        migrator.addTransformer( new StripesJspTransformer() );
        migrator.addTransformer( new JSPWikiJspTransformer() );
        migrator.initialize( new HashMap<String, Object>() );
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
     * Returns the ActionBean implementations found on the classpath.
     * 
     * @return the bean classes
     */
    protected static Set<Class<? extends ActionBean>> findBeanClasses()
    {
        // Find all ActionBean implementations on the classpath
        String beanPackagesProp = System.getProperty( WikiContextFactory.PROPS_ACTIONBEAN_PACKAGES,
                                                      WikiContextFactory.DEFAULT_ACTIONBEAN_PACKAGES ).trim();
        String[] beanPackages = beanPackagesProp.split( "," );
        ResolverUtil<ActionBean> resolver = new ResolverUtil<ActionBean>();
        resolver.findImplementations( ActionBean.class, beanPackages );
        Set<Class<? extends ActionBean>> beanClasses = resolver.getClasses();

        return beanClasses;
    }

    protected static String readSource( File src ) throws IOException
    {
        // Read in the file
        FileReader reader = new FileReader( src );
        StringBuilder s = new StringBuilder();
        int ch = 0;
        while ( (ch = reader.read()) != -1 )
        {
            s.append( (char) ch );
        }
        reader.close();
        return s.toString();
    }

    /**
     * Whether the JspMigrator should migrate HTML form elements to the Stripes
     * format. Defaults to <code>true</code>.
     */
    public static final String MIGRATE_FORMS = "stripes.migrator.migrateForms";

    private Map<String, Boolean> m_features = new HashMap<String, Boolean>();

    private List<JspTransformer> m_transformers = new ArrayList<JspTransformer>();

    private Map<String, Object> m_sharedState = new HashMap<String, Object>();

    /**
     * Adds a {@link JspTransformer} to the chain of transformers that will be
     * applied to each file as it is migrated.
     * 
     * @param transformer the transformer to add to the chain
     */
    public void addTransformer( JspTransformer transformer )
    {
        m_transformers.add( transformer );
    }

    /**
     * Returns <code>true</code> if a feature for the JspMigrator has been
     * set.
     * 
     * @param feature the feature to check; for example, {@link #MIGRATE_FORMS}
     * @return the result
     */
    public boolean getFeature( String feature )
    {
        if( feature == null )
        {
            throw new IllegalArgumentException( "Feature cannot be null." );
        }
        Boolean featureValue = m_features.get( feature );
        return featureValue == null ? false : featureValue.booleanValue();
    }

    /**
     * Initializes the JspMigrator with a shared-state Map containing key/value
     * pairs. Each {@link JspTransformer} added to the transformer via
     * {@link #addTransformer(JspTransformer)} is initialized in sequence by
     * calling its respective {@link JspTransformer#initialize(JspMigrator, Set, Map)} method.
     * Each JspTransformer is passed a Set of discovered
     * {@link net.sourceforge.stripes.action.ActionBean} classes, plus the
     * shared-state Map.
     * 
     * @param sharedState the shared-state Map passed to all JspTransformers at
     *            time of initialization
     */
    public void initialize( Map<String, Object> sharedState )
    {
        // Figure out the properties to use.
        boolean migrateForms = Boolean.getBoolean( System.getProperty( MIGRATE_FORMS, "true" ) );
        setFeature( MIGRATE_FORMS, migrateForms );

        m_sharedState = sharedState;

        // Initialize the transformers
        for( JspTransformer transformer : m_transformers )
        {
            transformer.initialize( this, findBeanClasses(), m_sharedState );
        }
    }

    /**
     * Migrates the contents of an entire directory from one location to
     * another.
     * 
     * @param sourceDir the source directory
     * @param destDir the destination directory
     */
    public void migrate( File sourceDir, File destDir ) throws IOException
    {
        // Find the files we need to migrate
        String sourcePath = sourceDir.getPath();
        List<File> allFiles = Collections.unmodifiableList( getFiles( sourceDir, ".jsp" ) );
        m_sharedState.put( ALL_JSPS, allFiles );

        for( File src : allFiles )
        {
            // Figure out the new file name
            String destPath = src.getPath().substring( sourcePath.length() );
            File dest = new File( destDir, destPath );

            // Create any directories we need
            dest.getParentFile().mkdirs();

            migrateFile( src, dest );
        }
    }

    /**
     * Sets a feature for the JspMigrator.
     * 
     * @param feature the feature to set; for example, {@link #MIGRATE_FORMS}
     * @param value whether the feature should be activated (<code>true</code>) or
     *            not (<code>false</code>).
     */
    public void setFeature( String feature, boolean value )
    {
        if( feature == null )
        {
            throw new IllegalArgumentException( "Feature cannot be null." );
        }
        m_features.put( feature, value );
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
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s.toString() );

        // Apply any transformations
        for( JspTransformer transformer : m_transformers )
        {
            transformer.transform( m_sharedState, doc );
        }

        // Write the transformed contents to disk
        writeDestination( dest, doc.toString() );
        System.out.println( "    done [" + s.length() + " chars]." );
    }

}
