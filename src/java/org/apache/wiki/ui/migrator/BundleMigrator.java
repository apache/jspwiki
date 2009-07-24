/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.ui.migrator;

import java.io.*;
import java.util.*;

import org.apache.wiki.util.CommentedProperties;


/**
 * Utility class for copying, moving, deleting and renaming message keys between
 * two message bundles.
 */
public class BundleMigrator
{
    /**
     *  Describes a message bundle.
     */
    public static class Bundle
    {
        private final Map<Locale, File> m_bundleFiles = new HashMap<Locale, File>();

        private final Map<Locale, CommentedProperties> m_bundleProps = new HashMap<Locale, CommentedProperties>();

        private final File m_baseFile;

        private CommentedProperties m_baseProps;

        /**
         * Constructs a new Bundle whose base file name is supplied. During
         * construction, the base bundle file is verified by appending
         * <code>.properties</code> to the base name and testing that the
         * resulting file exists. For example, if the base file
         * <code>src/Default</code> is passed, the file
         * <code>src/Default.properties</code> will be checked. If the file is
         * found, all possible combinations of bundle files are checked next,
         * for example <code>src/Default_en.properties</code>,
         * <code>src/Default_fr.properties</code>, and so on. If the base
         * bundle file does not exist, this method throws an
         * IllegalArgumentException.
         * 
         * @param baseFile the path to the base bundle file, minus the trailing
         *            locale suffix and <code>.properties</code> extension
         * @throws FileNotFoundException if the base bundle file does not exist
         */
        public Bundle( String baseFile ) throws FileNotFoundException
        {
            super();
            m_baseFile = new File( baseFile + ".properties" );
            m_baseProps = new CommentedProperties();
            findBundleFiles( baseFile );
        }

        /**
         * Returns the path to the bundle file for a supplied Locale.
         * 
         * @param locale if <code>null</code>, the base file will be returned
         * @return the file
         */
        public File getFile( Locale locale )
        {
            return locale == null ? m_baseFile : m_bundleFiles.get( locale );
        }

        /**
         * Returns the Locales associated with this Bundle.
         * 
         * @return the collection of Locale objects
         */
        public Collection<Locale> getLocales()
        {
            return Collections.unmodifiableSet( m_bundleFiles.keySet() );
        }

        /**
         * Returns the Properties object for a supplied Locale.
         * 
         * @param locale if <code>null</code>, the base Properties object
         *            will be returned
         * @return the file
         */
        public CommentedProperties getProperties( Locale locale )
        {
            return locale == null ? m_baseProps : m_bundleProps.get( locale );
        }

        /**
         *  Loads the set of bundle files from disk.
         *  @throws IOException If the loading fails.
         */
        public void load() throws IOException
        {
            // Load the default properties file first
            m_baseProps.load( new FileInputStream( m_baseFile ) );

            // Now load one for each Locale
            for( Map.Entry<Locale, File> entry : m_bundleFiles.entrySet() )
            {
                CommentedProperties props = new CommentedProperties();
                props.load( new FileInputStream( entry.getValue() ) );
                m_bundleProps.put( entry.getKey(), props );
            }
        }

        /**
         *  Saves the set of bundle files to disk.
         * 
         *  @throws IOException If saving fails.
         */
        public void save() throws IOException
        {
            // Save the default properties file firs
            m_baseProps.store( new FileOutputStream( m_baseFile ), null );

            // Now store each Locale's file
            for( Map.Entry<Locale, File> entry : m_bundleFiles.entrySet() )
            {
                CommentedProperties props = m_bundleProps.get( entry.getKey() );
                props.store( new FileOutputStream( entry.getValue() ), null );
            }
        }

        /**
         * Populates a Bundle, starting with a supplied base file and iterating
         * through every possible Locale combination.
         * 
         * @param baseFile the path to the base bundle file, minus the trailing
         *            locale and <code>.properties</code>
         * @throws FileNotFoundException If the Bundle file cannot be located. 
         */
        protected void findBundleFiles( String baseFile ) throws FileNotFoundException
        {
            File baseBundle = new File( baseFile + ".properties" );
            if( !baseBundle.exists() )
            {
                throw new FileNotFoundException( "Bundle file " + baseBundle.toString() + " not found." );
            }

            // Add all bundle files that exist in the file system
            Locale[] locales = Locale.getAvailableLocales();
            for( Locale locale : locales )
            {
                File file = new File( baseFile + "_" + locale.toString() + ".properties" );
                if( file.exists() )
                {
                    m_bundleFiles.put( locale, file );
                    m_bundleProps.put( locale, new CommentedProperties() );
                }
            }
        }
    }

    private Bundle m_source;

    /**
     * Constructs a new BundleMigrator.
     */
    public BundleMigrator()
    {
        super();
    }

    /**
     * Copies a message key from the source Bundle to a target Bundle.
     * 
     * @param key the name of the key to copy
     * @param target the target Bundle
     * @throws IOException If the copying fails.
     */
    public void copy( String key, Bundle target ) throws IOException
    {
        // Look for the property in the base file
        String value = m_source.getProperties( null ).getProperty( key );
        if( value == null )
        {
            throw new IllegalArgumentException( "Key " + key + " not found in bundle." );
        }

        // Load the source and target bundles
        m_source.load();
        target.load();
        String msg = "Copied from " + m_source.m_baseFile.getPath() + ".";

        // Copy the base property file's key first
        CommentedProperties props = target.getProperties( null );
        String comment = props.getComment( key );
        comment = comment == null ? msg : comment + ". " + msg;
        props.setProperty( key, value, comment );

        // Copy the key for each locale file
        Collection<Locale> locales = m_source.getLocales();
        for( Locale locale : locales )
        {
            props = m_source.getProperties( locale );
            value = props.getProperty( key );
            if( value != null )
            {
                props = target.getProperties( locale );
                if( props != null )
                {
                    comment = props.getComment( key );
                    comment = comment == null ? msg : comment + ". " + msg;
                    props.setProperty( key, value, comment );
                }
            }
        }

        // Save the target bundle
        target.save();
    }

    /**
     * Returns the source {@link Bundle} for the BundleMigrator to operate on.
     * 
     * @return the source bundle
     */
    public Bundle getSource()
    {
        return m_source;
    }

    /**
     * Moves a message key from the source Bundle to a target Bundle.
     * 
     * @param key the name of the key to move
     * @param target the target Bundle
     * @throws IOException If the moving fails.
     */
    public void move( String key, Bundle target ) throws IOException
    {
        copy( key, target );
        remove( key );
    }

    /**
     * Deletes a message key from the source Bundle.
     * 
     * @param key the name of the key to remove
     * @throws IOException If removal fails.
     */
    public void remove( String key ) throws IOException
    {
        // Look for the property in the base file
        String value = m_source.getProperties( null ).getProperty( key );
        if( value == null )
        {
            throw new IllegalArgumentException( "Key " + key + " not found in bundle." );
        }

        // Load the source bundle
        m_source.load();

        // Rename the base property file's key first
        CommentedProperties props = m_source.getProperties( null );
        props.remove( key );

        // Remove the key from each locale file
        Collection<Locale> locales = m_source.getLocales();
        for( Locale locale : locales )
        {
            props = m_source.getProperties( locale );
            value = props.getProperty( key );
            if( value != null )
            {
                props.remove( key );
            }
        }

        // Save the bundle
        m_source.save();
    }

    /**
     * Renames a message key contained in the source Bundle.
     * 
     * @param key the name of the key to change
     * @param newKey the new name for the key
     * @throws IOException If the renaming fails for some reason.
     */
    public void rename( String key, String newKey ) throws IOException
    {
        // Look for the property in the base file
        String value = m_source.getProperties( null ).getProperty( key );
        if( value == null )
        {
            throw new IllegalArgumentException( "Key " + key + " not found in bundle." );
        }
        if( newKey == null )
        {
            throw new IllegalArgumentException( "New key name must not be null." );
        }

        // Load the source bundle
        m_source.load();
        String msg = "Formerly named " + key + ".";

        // Rename the base property file's key first
        CommentedProperties props = m_source.getProperties( null );
        String comment = props.getComment( key );
        comment = comment == null ? msg : comment + ". " + msg;
        props.remove( key );
        props.setProperty( newKey, value, comment );

        // Rename the key in each locale file
        Collection<Locale> locales = m_source.getLocales();
        for( Locale locale : locales )
        {
            props = m_source.getProperties( locale );
            value = props.getProperty( key );
            if( value != null )
            {
                comment = props.getComment( key );
                comment = comment == null ? msg : comment + ". " + msg;
                props.remove( key );
                props.setProperty( newKey, value, comment );
            }
        }

        // Save the bundle
        m_source.save();

    }

    /**
     * Sets the source {@link Bundle} to operate on, and loads it.
     * 
     * @param source the Bundle to operate on
     * @throws FileNotFoundException if the base bundle file does not exist
     * @throws IOException If something else fails.
     */
    public void setBundle( Bundle source ) throws FileNotFoundException, IOException
    {
        m_source = source;
        source.load();
    }

    /**
     * <p>
     * Command-line interface to BundleMigrator. The general syntax for running
     * BundleMigrator is:
     * </p>
     * <blockquote>
     * <code>BundleMigrator <var>action source keyname [destination | newkeyname]</var></code>
     * </blockquote>
     * <p>
     * ...where <var>action</var> is an action verb: <code>copy</code>,
     * <code>delete</code>, <code>move</code> or <code>rename</code>.
     * </p>
     * <p>
     * The <code>source</code> parameter indicates the path to the base bundle
     * file, minus the trailing locale suffix and <code>.properties</code>
     * extension; for example, <code>etc/i18n/CoreResources</code>. The
     * <code>destination</code> parameter indicates the destination bundle for
     * move and rename actions.
     * </p>
     * <p>
     * The <code>keyname</code> denotes the key to copy, move, delete or
     * rename. When renaming a key, <code>newkeyname</code> denotes the new
     * key name.
     * </p>
     * <p>
     * Here are examples for the entire set of valid commands:
     * </p>
     * <blockquote>
     * <code>BundleMigrator rename src/i18n/CoreResources error.oldname error.newname<br/>
     * BundleMigrator delete src/i18n/CoreResources error.oldname<br/>
     * BundleMigrator copy src/i18n/CoreResources error.oldname src/i18n/templates/default<br/>
     * BundleMigrator move src/i18n/CoreResources error.oldname src/i18n/templates/default</code></blockquote>
     * 
     * @param args the command line arguments
     */
    public static final void main( String[] args )
    {
        String validSyntax = "BundleMigrator action source keyname [newkeyname | destination]";
        if( args.length == 0 )
        {
            System.err.println( "Too few arguments. Valid syntax: " + validSyntax );
            System.err.println( "Valid actions are: copy, delete, move, and rename." );
            return;
        }

        try
        {
            // Get the verb and source bundle
            String action = args[0].trim();
            Bundle source = new Bundle( args[1].trim() );
            String key = args[2].trim();
            BundleMigrator migrator = new BundleMigrator();
            migrator.setBundle( source );

            // Execute the action
            if( "delete".equals( action ) )
            {
                if  ( wrongNumberArgs( 3, args, "BundleMigrator delete source keyname" ) ) 
                    return;
                
                migrator.remove( key );
            }
            else if( "rename".equals( action ) )
            {
                if ( wrongNumberArgs( 4, args, "BundleMigrator rename source keyname newkeyname" ) )
                    return;
                
                String newKey = args[3].trim();
                migrator.rename( key, newKey );
            }
            else if( "copy".equals( action ) )
            {
                if ( wrongNumberArgs( 4, args, "BundleMigrator copy source keyname destination" ) ) 
                    return;
                
                Bundle destination = new Bundle( args[3].trim() );
                migrator.copy( key, destination );
            }
            else if( "move".equals( action ) )
            {
                if ( wrongNumberArgs( 4, args, "BundleMigrator move source keyname destination" ) ) 
                    return;
                
                Bundle destination = new Bundle( args[3].trim() );
                migrator.move( key, destination );
            }
            else
            {
                System.err.println( "Invalid syntax. Valid syntax is: " + validSyntax );
            }
        }
        catch ( IOException e )
        {
            System.err.println( "Error: " + e.getMessage() );
        }
    }

    private static boolean wrongNumberArgs( int requiredArgs, String[] args, String validSyntax )
    {
        if( args.length != requiredArgs )
        {
            System.out.println( "Wrong number of arguments. Valid syntax: " + validSyntax );
            return true;
        }
        return false;
    }
}
