package com.ecyrd.jspwiki.auth.authorize;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.Principal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 * <p>
 * GroupDatabase implementation for loading, persisting and storing wiki groups,
 * using an XML file for persistence. Group entries are simple
 * <code>&lt;group&gt;</code> elements under the root. Each group member is
 * representated by a <code>&lt;member&gt;</code> element. For example:
 * </p>
 * <blockquote><code>
 * &lt;groups&gt;<br/>
 * &nbsp;&nbsp;&lt;group name="TV" created="Jun 20, 2006 2:50:54 PM" lastModified="Jan 21, 2006 2:50:54 PM"&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;member principal="Archie Bunker" /&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;member principal="BullwinkleMoose" /&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;member principal="Fred Friendly" /&gt;<br/>
 * &nbsp;&nbsp;&lt;/group&gt;<br/>
 * &nbsp;&nbsp;&lt;group name="Literature" created="Jun 22, 2006 2:50:54 PM" lastModified="Jan 23, 2006 2:50:54 PM"&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;member principal="Charles Dickens" /&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;member principal="Homer" /&gt;<br/>
 * &nbsp;&nbsp;&lt;/group&gt;<br/>
 * &lt;/groups&gt;
 * </code></blockquote>
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-07-29 19:18:01 $
 * @since 2.4.17
 */
public class XMLGroupDatabase implements GroupDatabase
{
    protected static final Logger log              = Logger.getLogger( XMLGroupDatabase.class );

    /**
     * The jspwiki.properties property specifying the file system location of
     * the group database.
     */
    public static final String    PROP_DATABASE    = "jspwiki.xmlGroupDatabaseFile";

    private static final String   DEFAULT_DATABASE = "groupdatabase.xml";

    private static final String   CREATED          = "created";

    private static final String   CREATOR          = "creator";
    
    private static final String   GROUP_TAG        = "group";

    private static final String   GROUP_NAME       = "name";

    private static final String   LAST_MODIFIED    = "lastModified";

    private static final String   MODIFIER         = "modifier";
    
    private static final String   MEMBER_TAG       = "member";

    private static final String   PRINCIPAL        = "principal";

    private Document              c_dom            = null;

    private DateFormat            c_format         = DateFormat.getDateTimeInstance();

    private File                  c_file           = null;
    
    private WikiEngine            m_engine         = null;
    
    private Map                   m_groups         = new HashMap();
    
    /**
     * Writes out the current contents of the groups database.
     * @see com.ecyrd.jspwiki.auth.authorize.GroupDatabase#commit()
     */
    public void commit() throws WikiSecurityException
    {
        if ( c_dom == null )
        {
            log.fatal( "Group database doesn't exist in memory." );
        }

        File newFile = new File( c_file.getAbsolutePath() + ".new" );
        try
        {
            BufferedWriter io = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( newFile ), "UTF-8" ) );

            // Write the file header and document root
            io.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" );
            io.write( "<groups>\n" );

            // Write each profile as a <group> node
            Collection groups = m_groups.values();
            for( Iterator it = groups.iterator(); it.hasNext(); )
            {
                Group group = (Group) it.next();
                io.write( "  <" + GROUP_TAG + " " );
                io.write( GROUP_NAME );
                io.write( "=\"" + group.getName() + "\" " );
                io.write( CREATOR );
                io.write( "=\"" + group.getCreator() + "\" " );
                io.write( CREATED );
                io.write( "=\"" + c_format.format( group.getCreated() ) + "\" " );
                io.write( MODIFIER );
                io.write( "=\"" + group.getModifier() + "\" " );
                io.write( LAST_MODIFIED );
                io.write( "=\"" + c_format.format( group.getLastModified() ) + "\"" );
                io.write( ">\n" );

                // Write each member as a <member> node
                Principal[] members = group.members();
                for( int j = 0; j < members.length; j++ )
                {
                    Principal member = members[j];
                    io.write( "    <" + MEMBER_TAG + " " );
                    io.write( PRINCIPAL );
                    io.write( "=\"" + member.getName() + "\" " );
                    io.write( "/>\n" );
                }

                // Close tag
                io.write( "  </" + GROUP_TAG + ">\n" );
            }
            io.write( "</groups>" );
            io.close();
        }
        catch( IOException e )
        {
            throw new WikiSecurityException( e.getLocalizedMessage() );
        }

        // Copy new file over old version
        File backup = new File( c_file.getAbsolutePath() + ".old" );
        if ( backup.exists() )
        {
            if ( !backup.delete() )
            {
                log.error( "Could not delete old group database backup: " + backup );
            }
        }
        if ( !c_file.renameTo( backup ) )
        {
            log.error( "Could not create group database backup: " + backup );
        }
        if ( !newFile.renameTo( c_file ) )
        {
            log.error( "Could not save database: " + backup + " restoring backup." );
            if ( !backup.renameTo( c_file ) )
            {
                log.error( "Restore failed. Check the file permissions." );
            }
            log.error( "Could not save database: " + c_file + ". Check the file permissions" );
        }
    }

    /**
     * @see com.ecyrd.jspwiki.auth.authorize.GroupDatabase#delete(com.ecyrd.jspwiki.auth.authorize.Group)
     */
    public void delete( Group group ) throws NoSuchPrincipalException, WikiSecurityException
    {
        String index = group.getName();
        boolean exists = m_groups.containsKey( index );
        
        if ( !exists )
        {
            throw new NoSuchPrincipalException( "Not in database: " + group.getName() );
        }
        
        m_groups.remove( index );
    }

    /**
     * @see com.ecyrd.jspwiki.auth.authorize.GroupDatabase#groups()
     */
    public Group[] groups() throws WikiSecurityException
    {
        buildDOM();
        Collection groups = m_groups.values();
        return (Group[])groups.toArray( new Group[groups.size()] );
    }

    /**
     * Initializes the group database based on values from a Properties object.
     * The properties object must contain a file path to the XML database file
     * whose key is {@link #PROP_DATABASE}.
     * @see com.ecyrd.jspwiki.auth.authorize.GroupDatabase#initialize(com.ecyrd.jspwiki.WikiEngine,
     *      java.util.Properties)
     * @throws NoRequiredPropertyException if the user database cannot be
     *             located, parsed, or opened
     */
    public void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException, WikiSecurityException
    {
        m_engine = engine;
        
        File defaultFile = null;
        if ( engine.getRootPath() == null )
        {
            log.warn( "Cannot identify JSPWiki root path" );
            defaultFile = new File( "WEB-INF/" + DEFAULT_DATABASE ).getAbsoluteFile();
        }
        else
        {
            defaultFile = new File( engine.getRootPath() + "/WEB-INF/" + DEFAULT_DATABASE );
        }
        
        // Get database file location
        String file = props.getProperty( PROP_DATABASE );
        if ( file == null )
        {
            log.error( "XML group database property " + PROP_DATABASE + " not found; trying " + defaultFile );
            c_file = defaultFile;
        }
        else
        {
            c_file = new File( file );
        }

        log.info( "XML group database at " + c_file.getAbsolutePath() );

        // Read DOM
        buildDOM();
    }

    /**
     * Saves the group by adding it to an internal cache and setting its created/modified dates.
     * @see com.ecyrd.jspwiki.auth.authorize.GroupDatabase#save(Group, Principal)
     */
    public void save( Group group, Principal modifier ) throws WikiSecurityException
    {
        if ( group == null || modifier == null )
        {
            throw new IllegalArgumentException( "Group or modifier cannot be null." );
        }
        
        String index = group.getName();
        boolean isNew = !( m_groups.containsKey( index ) );
        Date modDate = new Date( System.currentTimeMillis() );
        if ( isNew )
        {
            // If new, set created info
            group.setCreated( modDate );
            group.setCreator( modifier.getName() );
        }
        group.setModifier( modifier.getName() );
        group.setLastModified( modDate );

        // Add the group to the 'saved' list
        m_groups.put( index, group );
    }

    private void buildDOM() throws WikiSecurityException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating( false );
        factory.setExpandEntityReferences( false );
        factory.setIgnoringComments( true );
        factory.setNamespaceAware( false );
        try
        {
            c_dom = factory.newDocumentBuilder().parse( c_file );
            log.debug( "Database successfully initialized" );
        }
        catch( ParserConfigurationException e )
        {
            log.error( "Configuration error: " + e.getMessage() );
        }
        catch( SAXException e )
        {
            log.error( "SAX error: " + e.getMessage() );
        }
        catch( FileNotFoundException e )
        {
            log.info( "Group database not found; creating from scratch..." );
        }
        catch( IOException e )
        {
            log.error( "IO error: " + e.getMessage() );
        }
        if ( c_dom == null )
        {
            try
            {
                //
                // Create the DOM from scratch
                //
                c_dom = factory.newDocumentBuilder().newDocument();
                c_dom.appendChild( c_dom.createElement( "groups" ) );
            }
            catch( ParserConfigurationException e )
            {
                log.fatal( "Could not create in-memory DOM" );
            }
        }
        
        // Ok, now go and read this sucker in
        if ( c_dom != null )
        {
            NodeList groupNodes = c_dom.getElementsByTagName( GROUP_TAG );
            for( int i = 0; i < groupNodes.getLength(); i++ )
            {
                Element groupNode = (Element) groupNodes.item( i );
                String groupName = groupNode.getAttribute( GROUP_NAME );
                if ( groupName == null )
                {
                    log.warn( "Detected null group name in XMLGroupDataBase. Check your group database." );
                }
                else
                {
                    Group group = buildGroup( groupNode, groupName );
                    m_groups.put( groupName, group );
                }
            }
        }
    }
    
    /**
     * Constructs a Group based on a DOM group node.
     * @param groupNode the node in the DOM containing the node
     * @param name the name of the group
     * @throws NoSuchPrincipalException
     * @throws WikiSecurityException
     */
    private Group buildGroup( Element groupNode, String name ) throws NoSuchPrincipalException, WikiSecurityException
    {
        // It's an error if either param is null (very odd)
        if ( groupNode == null || name == null )
        {
            throw new IllegalArgumentException( "DOM element or name cannot be null." );
        }
        
        // Construct a new group
        Group group = new Group( name, m_engine.getApplicationName() );

        // Get the users for this group, and add them
        NodeList members = groupNode.getElementsByTagName( MEMBER_TAG );
        for( int i = 0; i < members.getLength(); i++ )
        {
            Element memberNode = (Element) members.item( i );
            String principalName = memberNode.getAttribute( PRINCIPAL );
            Principal member = new WikiPrincipal( principalName );
            group.add( member );
        }

        // Add the created/last-modified info
        String creator = groupNode.getAttribute( CREATOR );
        String created = groupNode.getAttribute( CREATED );
        String modifier = groupNode.getAttribute( MODIFIER );
        String modified = groupNode.getAttribute( LAST_MODIFIED );
        try
        {
            group.setCreated( c_format.parse( created ) );
            group.setLastModified( c_format.parse( modified ) );
        }
        catch( ParseException e )
        {
            log.warn( "Could not parse 'created' or 'lastModified' " + "attribute for " + " group'"
                    + group.getName() + "'." + " It may have been tampered with." );
        }
        group.setCreator( creator );
        group.setModifier( modifier );
        return group;
    }
}
