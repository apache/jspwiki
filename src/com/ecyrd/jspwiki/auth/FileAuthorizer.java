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
 * FileAuthorizer reads user-role-permission mappings from 
 * a file named in the properties by variable "jspwiki.authorizer.file".
 * Authorizer information is loaded upon initialization and updated
 * every minute. 
 *
 * <P>If the file name does not start with a slash, it is assumed
 * to be in WikiRootPath/WEB-INF.
 *
 * <P>FileAuthorizer implementations should be able to parse only
 * the correct lines and discard any extraneous information in the
 * data file. This lets us combine user/passwd/role/permission 
 * information in one file.
 *
 * <P>This class may be expanded to read an XML access definition file,
 * such as that used by Tomcat, in the future. Currently, however, the
 * format consists of freely ordered lines, thus:
 *
 * <PRE>
 * # comments start with a hash
 * user username password rolename1, rolename2, ...
 * role rolename  permissionname1, permissionname2, ...
 * </PRE>
 *
 * <P>The format is compatible with the FileAuthenticator.
 *
 * <P>Strings are trimmed. Permission names are free-form strings, but
 * whitespace should not be used in them. 
 * 
 * <P>The optional information will, quite probably, be the users, roles, but
 * this class does not handle them. 
 * Any line NOT containing colons will be skipped when parsing auth information.
 */
public class FileAuthorizer
    implements WikiAuthorizer
{
    
    public static final String PROP_AUTHFILE = Authorizer.PROP_AUTHORIZER + ".file";
    public static final String ENTRY_DEFAULT = "default";
    public static final String ENTRY_USER = "user";
    public static final String ENTRY_ROLE = "role";
    public static final String ENTRY_COMMENT = "#";

    private long m_lastLoadTime;
    private long m_lastAccessTime;
    private long m_refreshTime = 60L * 1000L;

    static Category log = Category.getInstance( FileAuthorizer.class );

    private File m_authFile;
    private HashMap m_userRoles;
    private HashMap m_rolePerms;
    private AccessRuleSet m_defaultRules;

    public FileAuthorizer()
    {
        m_defaultRules = new AccessRuleSet();
        m_userRoles = new HashMap();
        m_rolePerms = new HashMap();
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
            log.info( "FileAuthorizer initialized, using " + fileName );
            loadAuthFile();
        }
        else
        {
            log.error( "Unable to read authority file specified in jspwiki.properties (" +
                       fileName + "). Access disabled." );
        }

    }
    
    /**
     * Reads the authorization specification file and stores locally.
     */
    public void loadAuthFile()
    {
        m_userRoles.clear();
        m_rolePerms.clear();

        if( m_authFile != null )
        {
            try
            {
                BufferedReader in = new BufferedReader( new FileReader( m_authFile ) );
                String line = null;
                while( (line = in.readLine()) != null )
                {
                    line = line.trim();
                    if( line.startsWith( ENTRY_COMMENT ) )
                        continue;

                    if( line.startsWith( ENTRY_DEFAULT ) )
                    {
                        addDefaultRule( line );

                    }
                    else if( line.startsWith( ENTRY_USER ) )
                    {
                        addUserRoles( line );
                    }
                    else if( line.startsWith( ENTRY_ROLE ) )
                    {
                        addRolePermissions( line );
                    }

                    // Discard others silently.
                }
                in.close();
                m_lastLoadTime = System.currentTimeMillis();
                m_lastAccessTime = m_authFile.lastModified();

                return;
            }
            catch( IOException ioe )
            {
                log.error( "Unable to scan authorization file " + m_authFile.getName(), ioe );
            }
        }

        log.error( "No auth file initialized!" );
    }


    /**
     * If m_amountTime has passed since the last refresh, and the authorization
     * file has changed, reloads it.
     */
    private void refresh()
    {
        if( System.currentTimeMillis() - m_lastLoadTime > m_refreshTime )
        {
            if( m_authFile.lastModified() > m_lastAccessTime )
            {
                loadAuthFile();
            }
        }
    }



    /**
     * Loads the user's roles and permissions from the Authorizer's
     * storage and sets them in the WikiUserPrincipal.
     */
    public void loadPermissions( UserProfile wup )
    {
        // In case something has changed in the file:
        refresh();

        ArrayList userRoles = (ArrayList)m_userRoles.get( wup.getName() );
        if( userRoles != null && userRoles.size() > 0 )
        {
            Iterator roleIt = userRoles.iterator();
            while( roleIt.hasNext() )
            {
                String role = (String)roleIt.next();
                addRole( wup, role );
            }
        }
    }


    /**
     * Adds an access rule to the default rules that will be applied if
     * no rules are encountered on a WikiPage.
     */
    private void addDefaultRule( String ruleLine )
    {
        if( ruleLine == null || ruleLine.length() < ENTRY_DEFAULT.length() )
            return;

        String rule = ruleLine.substring( ENTRY_DEFAULT.length() );
        m_defaultRules.addRule( rule );
    }

    
    /** 
     * Returns a copy of the default rule set.
     * The returned object may be safelu modified by the caller
     * without affecting the defaults. 
     */
    public AccessRuleSet getDefaultPermissions()
    {
        // We can't allow access to the default set, since it is usual
        // to append page-specific rules to defaults.
        return( m_defaultRules.copy() );
    }




    /**
     * Explicitly adds a given role to a UserProfile.
     * Fetches the corresponding permissions and adds them, as well.
     */
    public void addRole( UserProfile wup, String roleName )
    {
        if( wup == null || roleName == null )
            return;

        wup.addRole( roleName );
        ArrayList rolePerms = (ArrayList)m_rolePerms.get( roleName );
        if( rolePerms != null && rolePerms.size() > 0 )
        {
            Iterator permIt = rolePerms.iterator();
            while( permIt.hasNext() )
            {
                String perm = (String)permIt.next();
                wup.addPermission( perm );
            }
        }
    }

    /**
     * Explicitly adds a given permission to a UserProfile.
     * (Provided here just for uniformity; calls 
     * UserProfile.addPermission().)
     */
    public void addPermission( UserProfile wup, String permName )
    {
        if( wup == null || permName == null )
            return;

        wup.addPermission( permName );
    }


    /**
     * Takes a <i>user</i> line and parses any comma separated role names from it.
     * The format of the line is described in the class description.
     */
    private void addUserRoles( String line )
    {
        UserEntry ue = new UserEntry( line );
        if( ue.isValid() && ue.extra != null )
        {
            m_userRoles.put( ue.uid, ue.extra );
        }
    }

    /**
     * Takes a <i>role</i> line, parses any permission names from it, and adds the
     * information to the role-permission table.
     */
    private void addRolePermissions( String line )
    {
        RoleEntry re = new RoleEntry( line );
        if( re.isValid() )
        {
            // A null extras value is OK.
            m_rolePerms.put( re.name, re.extra );
        }
    }


    private class UserEntry
    {
        public String id;
        public String uid;
        public String passwd;
        public ArrayList extra;

        public UserEntry( String entry )
        {
            StringTokenizer tok = new StringTokenizer( entry );
            if( tok.countTokens() < 3 )
                return;

            id = tok.nextToken();
            uid = tok.nextToken();
            passwd = tok.nextToken();
            if( tok.hasMoreTokens() )
            {
                extra = new ArrayList( 5 );
                while( tok.hasMoreTokens() )
                {
                    String extraValue = tok.nextToken( "," );
                    extra.add( extraValue.trim() );
                }
            }
        }

        public boolean isValid()
        {
            return( "user".equals( id ) && uid != null && passwd != null );
        }
    }

    private class RoleEntry
    {
        public String id;
        public String name;
        public ArrayList extra;

        public RoleEntry( String entry )
        {
            StringTokenizer tok = new StringTokenizer( entry );
            if( tok.countTokens() < 2 )
                return;

            id = tok.nextToken();
            name = tok.nextToken();
            if( tok.hasMoreTokens() )
            {
                extra = new ArrayList( 5 );
                while( tok.hasMoreTokens() )
                {
                    String extraValue = tok.nextToken( "," );
                    extra.add( extraValue.trim() );
                }
            }
        }

        public boolean isValid()
        {
            return( "role".equals( id ) && name != null );
        }
    }

} //EOF



