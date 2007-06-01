/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
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

    private Document              m_dom            = null;

    private DateFormat            m_defaultFormat  = DateFormat.getDateTimeInstance();

    private DateFormat            m_format         = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss:SSS z");

    private File                  m_file           = null;

    private WikiEngine            m_engine         = null;

    private Map                   m_groups         = new HashMap();

    /**
     * No-op method that in previous versions of JSPWiki was intended to
     * atomically commit changes to the user database. Now, the
     * {@link #save(Group, Principal)} and {@link #delete(Group)} methods
     * are atomic themselves.
     * @throws WikiSecurityException never...
     * @deprecated there is no need to call this method because the save and
     * delete methods contain their own commit logic
     */
    public void commit() throws WikiSecurityException
    { }

    /**
      * Looks up and deletes a {@link Group} from the group database. If the
     * group database does not contain the supplied Group. this method throws a
     * {@link NoSuchPrincipalException}. The method commits the results
     * of the delete to persistent storage.
     * @param group the group to remove
    * @throws WikiSecurityException if the database does not contain the
     * supplied group (thrown as {@link NoSuchPrincipalException}) or if
     * the commit did not succeed
     */
    public void delete( Group group ) throws WikiSecurityException
    {
        String index = group.getName();
        boolean exists = m_groups.containsKey( index );

        if ( !exists )
        {
            throw new NoSuchPrincipalException( "Not in database: " + group.getName() );
        }

        m_groups.remove( index );

        // Commit to disk
        saveDOM();
    }

    /**
     * Returns all wiki groups that are stored in the GroupDatabase as an array
     * of Group objects. If the database does not contain any groups, this
     * method will return a zero-length array. This method causes back-end
     * storage to load the entire set of group; thus, it should be called
     * infrequently (e.g., at initialization time).
     * @return the wiki groups
     * @throws WikiSecurityException if the groups cannot be returned by the back-end
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
     * @param engine the wiki engine
     * @param props the properties used to initialize the group database
     * @throws NoRequiredPropertyException if the user database cannot be
     *             located, parsed, or opened
     * @throws WikiSecurityException if the database could not be initialized successfully
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
            m_file = defaultFile;
        }
        else
        {
            m_file = new File( file );
        }

        log.info( "XML group database at " + m_file.getAbsolutePath() );

        // Read DOM
        buildDOM();
    }

    /**
     * Saves a Group to the group database. Note that this method <em>must</em>
     * fail, and throw an <code>IllegalArgumentException</code>, if the
     * proposed group is the same name as one of the built-in Roles: e.g.,
     * Admin, Authenticated, etc. The database is responsible for setting
     * create/modify timestamps, upon a successful save, to the Group.
     * The method commits the results of the delete to persistent storage.
     * @param group the Group to save
     * @param modifier the user who saved the Group
     * @throws WikiSecurityException if the Group could not be saved successfully
     */
    public void save( Group group, Principal modifier ) throws WikiSecurityException
    {
        if ( group == null || modifier == null )
        {
            throw new IllegalArgumentException( "Group or modifier cannot be null." );
        }

        checkForRefresh();

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

        // Commit to disk
        saveDOM();
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
            m_dom = factory.newDocumentBuilder().parse( m_file );
            log.debug( "Database successfully initialized" );
            m_lastModified = m_file.lastModified();
            m_lastCheck    = System.currentTimeMillis();
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
        if ( m_dom == null )
        {
            try
            {
                //
                // Create the DOM from scratch
                //
                m_dom = factory.newDocumentBuilder().newDocument();
                m_dom.appendChild( m_dom.createElement( "groups" ) );
            }
            catch( ParserConfigurationException e )
            {
                log.fatal( "Could not create in-memory DOM" );
            }
        }

        // Ok, now go and read this sucker in
        if ( m_dom != null )
        {
            NodeList groupNodes = m_dom.getElementsByTagName( GROUP_TAG );
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

    private long m_lastCheck    = 0;
    private long m_lastModified = 0;

    private void checkForRefresh()
    {
        long time = System.currentTimeMillis();

        if( time - m_lastCheck > 60*1000L )
        {
            long lastModified = m_file.lastModified();

            if( lastModified > m_lastModified )
            {
                try
                {
                    buildDOM();
                }
                catch( WikiSecurityException e )
                {
                    log.error("Could not refresh DOM",e);
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
    private Group buildGroup( Element groupNode, String name )
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
            group.setCreated( m_format.parse( created ) );
            group.setLastModified( m_format.parse( modified ) );
        }
        catch ( ParseException e )
        {
            // If parsing failed, use the platform default
            try
            {
                group.setCreated( m_defaultFormat.parse( created ) );
                group.setLastModified( m_defaultFormat.parse( modified ) );
            }
            catch ( ParseException e2)
            {
                log.warn( "Could not parse 'created' or 'lastModified' " + "attribute for " + " group'"
                          + group.getName() + "'." + " It may have been tampered with." );
            }
        }
        group.setCreator( creator );
        group.setModifier( modifier );
        return group;
    }

    private void saveDOM() throws WikiSecurityException
    {
        if ( m_dom == null )
        {
            log.fatal( "Group database doesn't exist in memory." );
        }

        File newFile = new File( m_file.getAbsolutePath() + ".new" );
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
                io.write( "=\"" + StringEscapeUtils.escapeXml( group.getName() )+ "\" " );
                io.write( CREATOR );
                io.write( "=\"" + StringEscapeUtils.escapeXml( group.getCreator() ) + "\" " );
                io.write( CREATED );
                io.write( "=\"" + m_format.format( group.getCreated() ) + "\" " );
                io.write( MODIFIER );
                io.write( "=\"" + group.getModifier() + "\" " );
                io.write( LAST_MODIFIED );
                io.write( "=\"" + m_format.format( group.getLastModified() ) + "\"" );
                io.write( ">\n" );

                // Write each member as a <member> node
                Principal[] members = group.members();
                for( int j = 0; j < members.length; j++ )
                {
                    Principal member = members[j];
                    io.write( "    <" + MEMBER_TAG + " " );
                    io.write( PRINCIPAL );
                    io.write( "=\"" + StringEscapeUtils.escapeXml(member.getName()) + "\" " );
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
        File backup = new File( m_file.getAbsolutePath() + ".old" );
        if ( backup.exists() )
        {
            if ( !backup.delete() )
            {
                log.error( "Could not delete old group database backup: " + backup );
            }
        }
        if ( !m_file.renameTo( backup ) )
        {
            log.error( "Could not create group database backup: " + backup );
        }
        if ( !newFile.renameTo( m_file ) )
        {
            log.error( "Could not save database: " + backup + " restoring backup." );
            if ( !backup.renameTo( m_file ) )
            {
                log.error( "Restore failed. Check the file permissions." );
            }
            log.error( "Could not save database: " + m_file + ". Check the file permissions" );
        }
    }

}
