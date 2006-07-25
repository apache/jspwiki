package com.ecyrd.jspwiki.auth;

import java.io.File;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.commons.lang.ArrayUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.auth.acl.Acl;
import com.ecyrd.jspwiki.auth.acl.AclEntry;
import com.ecyrd.jspwiki.auth.acl.UnresolvedPrincipal;
import com.ecyrd.jspwiki.auth.authorize.DefaultGroup;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.auth.user.DefaultUserProfile;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 * Tests the AuthorizationManager class.
 * @author Janne Jalkanen
 */
public class AuthorizationManagerTest extends TestCase
{
    private AuthorizationManager m_auth;

    private TestEngine           m_engine;

    private WikiContext          m_context;

    private WikiSession          m_session;

    public AuthorizationManagerTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine( props );
        m_auth = m_engine.getAuthorizationManager();
    }

    /**
     * Set up a simple wiki page without ACL. 
     * Create a WikiContext for this page, with an associated
     * servlet request with userPrincipal Alice that has roles IT and Engineering.
     * Create a sample user Alice who possesses built-in
     * roles ALL and AUTHENTICATED.
     *
     */
    public void testHasRoleOrPrincipal()
    {
        String src = "Sample wiki page without ACL";
        try
        {
            m_engine.saveText( "Test", src );
            WikiPage p = m_engine.getPage( "Test" );
            TestHttpServletRequest request = new TestHttpServletRequest();
            request.setRoles( new String[0] );
            m_context = new WikiContext( m_engine, request, p );
        }
        catch( WikiException e )
        {
            assertTrue( "Setup failed", false );
        }
        
        String wiki = m_engine.getApplicationName();

        Principal alice = new WikiPrincipal( "Alice" );
        Principal[] principals = new Principal[]
        { alice, Role.AUTHENTICATED, Role.ALL, new Role( "IT" ), new Role ( "Engineering" ), new GroupPrincipal( wiki, "Admin" ) };
        GroupManager groupMgr = m_engine.getGroupManager();
        WikiSession session = buildSession( m_context, principals );
        groupMgr.addWikiEventListener( session );

        // Test build-in role membership
        assertTrue( "Alice ALL", m_auth.hasRoleOrPrincipal( session, Role.ALL ) );
        assertFalse( "Alice ANONYMOUS", m_auth.hasRoleOrPrincipal( session, Role.ANONYMOUS ) );
        assertFalse( "Alice ASSERTED", m_auth.hasRoleOrPrincipal( session, Role.ASSERTED ) );
        assertTrue( "Alice AUTHENTICATED", m_auth.hasRoleOrPrincipal( session, Role.AUTHENTICATED ) );
        assertTrue( "Alice IT", m_auth.hasRoleOrPrincipal( session, new Role( "IT" ) ) );
        assertTrue( "Alice Engineering", m_auth.hasRoleOrPrincipal( session, new Role( "Engineering" ) ) );
        assertFalse( "Alice Finance", m_auth.hasRoleOrPrincipal( session, new Role( "Finance" ) ) );

        // Test group membership: Alice should be part of group Bar, but not Foo
        Group fooGroup = new DefaultGroup( "Foo" );
        groupMgr.add( fooGroup );
        Group barGroup = new DefaultGroup( "Bar" );
        barGroup.add( alice );
        groupMgr.add( barGroup );
        assertFalse( "Authenticated Alice not in Foo", m_auth.hasRoleOrPrincipal( session, fooGroup ) );
        assertTrue( "Authenticated Alice in Bar", m_auth.hasRoleOrPrincipal( session, barGroup ) );
        
        // Test user principal posession
        assertTrue("Alice has Alice", m_auth.hasRoleOrPrincipal( session, new WikiPrincipal("Alice")));
        assertFalse("Alice has Bob", m_auth.hasRoleOrPrincipal( session, new WikiPrincipal("Bob")));
        
        // Test user principal (non-WikiPrincipal) posession
        assertTrue("Alice has Alice", m_auth.hasRoleOrPrincipal( session, new TestPrincipal("Alice")));
        assertFalse("Alice has Bob", m_auth.hasRoleOrPrincipal( session, new TestPrincipal("Bob")));
        
        // Create a new session for Alice as an Asserted user. This time,
        // she should NOT be considered part of either group Bar or Foo, since we prohibit
        // Asserted users from being members of any role that isn't built-in.
        principals = new Principal[]
        { alice, Role.ASSERTED, Role.ALL };
        session = buildSession( m_context, principals );
        groupMgr.addWikiEventListener( session );
        assertFalse( "Asserted Alice not in Foo", m_auth.hasRoleOrPrincipal( session, fooGroup ) );
        assertFalse( "Asserted Alice not in Bar", m_auth.hasRoleOrPrincipal( session, barGroup ) );
        
        // Cleanup
        try
        {
            m_engine.deletePage( "Test" );
        }
        catch ( ProviderException e ) 
        {
            assertTrue(false);
        }
    }
    
    public void testStaticPermission()
    {
        String wiki = m_engine.getApplicationName();
        
        Subject s = new Subject();
        s.getPrincipals().add( Role.ANONYMOUS );
        assertTrue( "Anonymous view",     m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Anonymous edit",     m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Anonymous comment",  m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertFalse( "Anonymous modify",  m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertFalse( "Anonymous upload",  m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertFalse( "Anonymous rename",  m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Anonymous delete",  m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertTrue( "Anonymous prefs",    m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertTrue( "Anonymous profile",  m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertTrue( "Anonymous pages",    m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertFalse( "Anonymous groups",  m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );
        
        s = new Subject();
        s.getPrincipals().add( Role.ASSERTED );
        assertTrue( "Asserted view",     m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Asserted edit",     m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Asserted comment",  m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertFalse( "Asserted modify",  m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertFalse( "Asserted upload",  m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertFalse( "Asserted rename",  m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Asserted delete",  m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertTrue( "Asserted prefs",    m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertTrue( "Asserted profile",  m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertTrue( "Asserted pages",    m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertFalse( "Asserted groups",  m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );
        
        s = new Subject();
        s.getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "Authenticated view",      m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Authenticated edit",      m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Authenticated comment",   m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertTrue( "Authenticated modify",    m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertTrue( "Authenticated upload",    m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertTrue( "Authenticated rename",    m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Authenticated delete",   m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertTrue( "Authenticated prefs",     m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertTrue( "Authenticated profile",   m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertTrue( "Authenticated pages",     m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertTrue( "Authenticated groups",    m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );
        
        s = new Subject();
        s.getPrincipals().add( new GroupPrincipal( wiki, "Admin" ) );
        assertTrue( "Admin view",     m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Admin edit",     m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Admin comment",  m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertTrue( "Admin modify",   m_auth.checkStaticPermission( s, PagePermission.MODIFY ) );
        assertTrue( "Admin upload",   m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertTrue( "Admin rename",   m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertTrue( "Admin delete",   m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertTrue( "Admin prefs",    m_auth.checkStaticPermission( s, WikiPermission.EDIT_PREFERENCES ) );
        assertTrue( "Admin profile",  m_auth.checkStaticPermission( s, WikiPermission.EDIT_PROFILE ) );
        assertTrue( "Admin pages",    m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertTrue( "Admin groups",   m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );
    }
    
    private static class TestPrincipal implements Principal
    {
        private final String m_name;
        public TestPrincipal( String name)
        {
            m_name = name;
        }
        
        public String getName() {
            return m_name;
        }
    }
    
    private WikiSession buildSession( WikiContext context, Principal[] principals )
    {
        WikiSession session = context.getWikiSession();
        Set subjectPrincipals = session.getSubject().getPrincipals();
        subjectPrincipals.clear();
        for( int i = 0; i < principals.length; i++ )
        {
            subjectPrincipals.add( principals[i] );
        }
        return session;
    }

    /**
     * Any principal strings that have same names as built-in
     * roles should resolve as built-in roles!
     */
    public void testResolveBuiltInRoles()
    {
        Principal principal = Role.AUTHENTICATED;
        assertEquals( principal, m_auth.resolvePrincipal( "Authenticated" ) );
        principal = Role.ASSERTED;
        assertEquals( principal, m_auth.resolvePrincipal( "Asserted" ) );
        principal = Role.ALL;
        assertEquals( principal, m_auth.resolvePrincipal( "All" ) );
        principal = Role.ANONYMOUS;
        assertEquals( principal, m_auth.resolvePrincipal( "Anonymous" ) );
        
        // This should resolve because there's no built-in role Admin
        principal = new WikiPrincipal("Admin");
        assertFalse( principal.equals( m_auth.resolvePrincipal( "Admin" ) ) );
    }

    public void testResolveGroups()
    {
        Group group1 = new DefaultGroup("SampleGroup");
        m_engine.getGroupManager().add( group1 );
        assertEquals( group1, m_auth.resolvePrincipal( "SampleGroup" ) );
        
        // We shouldn't be able to spoof a built-in role
        Group group2 = new DefaultGroup("Authenticated");
        m_engine.getGroupManager().add( group2 );
        super.assertNotSame( group2, m_auth.resolvePrincipal( "Authenticated" ) );
        super.assertEquals( Role.AUTHENTICATED, m_auth.resolvePrincipal( "Authenticated" ) );
        
        // We shouldn't resolve a group if the manager doesn't know about it
        Group group3 = new DefaultGroup("NonExistentGroup");
        assertFalse( group3.equals( m_auth.resolvePrincipal( "NonExistentGroup" ) ) );
    }

    
    public void testResolveUsers()
    {
        // We should be able to resolve a user by login, user, or wiki name
        UserProfile profile = new DefaultUserProfile();
        profile.setEmail("janne@jalkanen.net");
        profile.setFullname("Janne Jalkanen");
        profile.setLoginName("janne");
        profile.setWikiName("JanneJalkanen");
        try {
            m_engine.getUserDatabase().save( profile );
        }
        catch (WikiSecurityException e)
        {
            assertFalse("Failed save: " + e.getLocalizedMessage(), true);
        }
        assertEquals( new WikiPrincipal( "janne" ), m_auth.resolvePrincipal( "janne" ) );
        assertEquals( new WikiPrincipal( "Janne Jalkanen" ), m_auth.resolvePrincipal( "Janne Jalkanen" ) );
        assertEquals( new WikiPrincipal( "JanneJalkanen" ), m_auth.resolvePrincipal( "JanneJalkanen" ) );
        
        // We shouldn't be able to spoof a built-in group
        Group group1 = new DefaultGroup("SampleGroup");
        m_engine.getGroupManager().add( group1 );
        super.assertNotSame( new WikiPrincipal("SampleGroup"), m_auth.resolvePrincipal( "Authenticated" ) );
        super.assertEquals( group1, m_auth.resolvePrincipal( "SampleGroup" ) );

        // We shouldn't be able to spoof a built-in role
        super.assertNotSame( new WikiPrincipal("Authenticated"), m_auth.resolvePrincipal( "Authenticated" ) );
        super.assertEquals( Role.AUTHENTICATED, m_auth.resolvePrincipal( "Authenticated" ) );

        // An unknown user should resolve to a generic UnresolvedPrincipal
        Principal principal = new UnresolvedPrincipal("Bart Simpson");
        assertEquals( principal, m_auth.resolvePrincipal("Bart Simpson"));
    }
    
    /**
     * Tests the default policy. Anonymous users can read,
     * Authenticated can edit, etc. Uses the default 
     * tests/etc/jspwiki.policy file installed by the JRE at startup.
     * @throws Exception
     */
    public void testDefaultPermissions() throws Exception
    {
        String text = "Foo";
        m_engine.saveText( "TestDefaultPage", text );
        
        WikiPage p = m_engine.getPage("TestDefaultPage");
        m_context = new WikiContext( m_engine, p );
        m_session = m_context.getWikiSession();

        AuthorizationManager mgr = m_engine.getAuthorizationManager();
        
        // Charlie is anonymous
        Principal principal = new WikiPrincipal( "Charlie");
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.ANONYMOUS );
        assertTrue( "Charlie view", 
                    mgr.checkPermission( m_session,
                                     new PagePermission( p, "view" ) ) );
        assertTrue( "Charlie edit", 
                    mgr.checkPermission( m_session,
                                      new PagePermission( p, "edit" ) ) );

        // Bob is logged in
        principal = new WikiPrincipal( "Bob");
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "Bob view", mgr.checkPermission( m_session,
                new PagePermission( p, "view" ) ) );
        assertTrue( "Bob edit", mgr.checkPermission( m_session,
                new PagePermission( p, "edit" ) ) );

        // Cleanup
        try
        {
            m_engine.deletePage( "TestDefaultPage" );
        }
        catch ( ProviderException e ) 
        {
            assertTrue(false);
        }
    }

    public void testGetRoles() throws Exception
    {
        String wiki = m_engine.getApplicationName();
        
        // Set up a group without Bob in it
        String text = "Foobar.\n\n[{SET members=Alice, Charlie}]\n\nTest group.";
        m_engine.saveText( "GroupTest", text );
        
        // Pretend Bob has asserted his identity and has role "admin"
        TestHttpServletRequest request = new TestHttpServletRequest();
        Principal[] principals = new Principal[]{ new WikiPrincipal( "Bob" ), Role.ALL, Role.ASSERTED, new GroupPrincipal( wiki, "Admin" ) };
        WikiPage p = m_engine.getPage( "GroupTest" );
        m_context = new WikiContext( m_engine, request, p );
        m_session = buildSession( m_context, principals );
        m_engine.getGroupManager().addWikiEventListener( m_session );
        
        // Bob should have two roles
        principals = m_auth.getRoles( m_session );
        assertTrue( "Bob member of ALL", ArrayUtils.contains( principals, Role.ALL ) );
        assertTrue( "Bob member of ASSERTED", ArrayUtils.contains( principals, Role.ASSERTED ) );
        assertFalse( "Bob member of ANONYMOUS", ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        assertFalse( "Bob member of Test", ArrayUtils.contains( principals, new GroupPrincipal( wiki, "Test" ) ) );
        
        // Re-save group "Test" with Bob as a member
        text = "Foobar.\n\n[{SET members=Alice, Bob, Charlie}]\n\nTest group.";
        m_engine.saveText( "GroupTest", text );

        // Bob should have three roles
        principals = m_auth.getRoles( m_session );
        assertTrue( "Bob member of ALL", ArrayUtils.contains( principals, Role.ALL ) );
        assertTrue( "Bob member of ASSERTED", ArrayUtils.contains( principals, Role.ASSERTED ) );
        assertFalse( "Bob member of ANONYMOUS", ArrayUtils.contains( principals, Role.ANONYMOUS ) );
        assertTrue( "Bob member of Test", ArrayUtils.contains( principals, new GroupPrincipal( wiki, "Test" ) ) );
    }
    
    public void testPrincipalAclPermissions() throws Exception
    {
        String src = "[{ALLOW edit Foo}] ";
        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage( "Test" );
        m_context = new WikiContext( m_engine, p );
        m_session = m_context.getWikiSession();

        // Foo is in the ACL and can read
        Principal principal = new WikiPrincipal( "Foo" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "Foo view Test", m_auth.checkPermission( m_session, new PagePermission( p, "view" ) ) );
        assertTrue( "Foo edit Test", m_auth.checkPermission( m_session, new PagePermission( p, "edit" ) ) );
        assertTrue( "Foo view all", m_auth.checkPermission( m_session, PagePermission.VIEW ) );
        assertTrue( "Foo edit all", m_auth.checkPermission( m_session, PagePermission.EDIT ) );

        // Bar is not in the ACL, so he can't read or edit
        principal = new WikiPrincipal( "Bar" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.ANONYMOUS );
        assertFalse( "Bar view Test", m_auth.checkPermission( m_session, new PagePermission( p, "view" ) ) );
        assertFalse( "Bar !view Test", m_auth.checkPermission( m_session, new PagePermission( p, "edit" ) ) );
        assertTrue( "Bar view all", m_auth.checkPermission( m_session, PagePermission.VIEW ) );
        assertTrue( "Bar !edit all", m_auth.checkPermission( m_session, PagePermission.EDIT ) );
        
        // Cleanup
        try
        {
            m_engine.deletePage( "Test" );
        }
        catch ( ProviderException e ) 
        {
            fail("Could not delete page");
        }
    }

    public void testInheritedPermissions() throws Exception
    {
        String src = "[{ALLOW edit Foo}] ";
        m_engine.saveText( "Test", src );

        File f = m_engine.makeAttachmentFile();
        
        Attachment att = new Attachment( m_engine, "Test", "test1.txt" );

        att.setAuthor( "FirstPost" );

        m_engine.getAttachmentManager().storeAttachment( att, f );
        
        Attachment p = (Attachment)m_engine.getPage( "Test/test1.txt" );
        m_context = new WikiContext( m_engine, p );
        m_session = m_context.getWikiSession();

        // Foo is in the ACL and can read
        Principal principal = new WikiPrincipal( "Foo" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "Foo view Test", m_auth.checkPermission( m_session, new PagePermission( p, "view" ) ) );
        assertTrue( "Foo edit Test", m_auth.checkPermission( m_session, new PagePermission( p, "edit" ) ) );
        assertTrue( "Foo view all", m_auth.checkPermission( m_session, PagePermission.VIEW ) );
        assertTrue( "Foo edit all", m_auth.checkPermission( m_session, PagePermission.EDIT ) );

        // Bar is not in the ACL, so he can't read or edit
        principal = new WikiPrincipal( "Bar" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.ANONYMOUS );
        assertFalse( "Bar view Test", m_auth.checkPermission( m_session, new PagePermission( p, "view" ) ) );
        assertFalse( "Bar !view Test", m_auth.checkPermission( m_session, new PagePermission( p, "edit" ) ) );
        assertTrue( "Bar view all", m_auth.checkPermission( m_session, PagePermission.VIEW ) );
        assertTrue( "Bar !edit all", m_auth.checkPermission( m_session, PagePermission.EDIT ) );
        

        m_engine.deletePage( "Test" );
    }

    public void testInheritedPermissions2() throws Exception
    {
        String src = "[{ALLOW view Foo}] [[ALLOW edit AdminGroup}]";
        m_engine.saveText( "Test", src );

        File f = m_engine.makeAttachmentFile();
        
        Attachment att = new Attachment( m_engine, "Test", "test1.txt" );

        att.setAuthor( "FirstPost" );

        m_engine.getAttachmentManager().storeAttachment( att, f );
        
        Attachment p = (Attachment)m_engine.getPage( "Test/test1.txt" );
        m_context = new WikiContext( m_engine, p );
        m_session = m_context.getWikiSession();

        // Foo is in the ACL and can read
        Principal principal = new WikiPrincipal( "Foo" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "Foo view Test", m_auth.checkPermission( m_session, new PagePermission( p, "view" ) ) );
        assertFalse( "Foo !edit Test", m_auth.checkPermission( m_session, new PagePermission( p, "edit" ) ) );
        assertTrue( "Foo view all", m_auth.checkPermission( m_session, PagePermission.VIEW ) );
        assertTrue( "Foo edit all", m_auth.checkPermission( m_session, PagePermission.EDIT ) );

        // Bar is not in the ACL, so he can't read or edit
        principal = new WikiPrincipal( "Guest" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.ANONYMOUS );
        assertFalse( "Guest !view Test", m_auth.checkPermission( m_session, new PagePermission( p, "view" ) ) );
        assertFalse( "Guest !edit Test", m_auth.checkPermission( m_session, new PagePermission( p, "edit" ) ) );
        assertTrue( "Guest view all", m_auth.checkPermission( m_session, PagePermission.VIEW ) );
        assertTrue( "Guest edit all", m_auth.checkPermission( m_session, PagePermission.EDIT ) );
        
        m_engine.getAttachmentManager().deleteAttachment( att );
        m_engine.deletePage( "Test" );
    }

    public void testRoleAclPermissions() throws Exception
    {
        String src = "[{ALLOW edit Authenticated}] ";
        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage( "Test" );
        m_context = new WikiContext( m_engine, p );
        m_session = m_context.getWikiSession();

        // Authenticated is in the ACL and can view and edit
        Principal principal = new WikiPrincipal( "Foo" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "Foo view Test", m_auth.checkPermission( m_session, new PagePermission( p, "view" ) ) );
        assertTrue( "Foo edit Test", m_auth.checkPermission( m_session, new PagePermission( p, "edit" ) ) );
        assertTrue( "Foo view all", m_auth.checkPermission( m_session, PagePermission.VIEW ) );
        assertTrue( "Foo edit all", m_auth.checkPermission( m_session, PagePermission.EDIT ) );

        // Bar is not authenticated, so he can't read or edit
        principal = new WikiPrincipal( "Bar" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.ANONYMOUS );
        assertFalse( "Bar view Test", m_auth.checkPermission( m_session, new PagePermission( p, "view" ) ) );
        assertFalse( "Bar edit Test", m_auth.checkPermission( m_session, new PagePermission( p, "edit" ) ) );
        
        // Cleanup
        try
        {
            m_engine.deletePage( "Test" );
        }
        catch ( ProviderException e ) 
        {
            assertTrue(false);
        }
    }

    /**
     * Returns a string representation of the permissions of the page.
     */
    public static String printPermissions( WikiPage p ) throws Exception
    {
        StringBuffer sb = new StringBuffer();

        Acl acl = p.getAcl();

        sb.append( "page = " + p.getName() + "\n" );

        if ( acl != null )
        {
            for( Enumeration e = acl.entries(); e.hasMoreElements(); )
            {
                AclEntry entry = (AclEntry) e.nextElement();

                sb.append( "  user = " + entry.getPrincipal().getName() + ": " );

                for( Enumeration perms = entry.permissions(); perms.hasMoreElements(); )
                {
                    sb.append( perms.nextElement().toString() );
                }
                sb.append( ")\n" );
            }
        }
        else
        {
            sb.append( "  no permissions\n" );
        }

        return sb.toString();
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("Authorization Manager test");
        suite.addTestSuite( AuthorizationManagerTest.class );
        return suite;
    }
}