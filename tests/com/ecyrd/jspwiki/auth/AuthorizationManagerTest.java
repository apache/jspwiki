package com.ecyrd.jspwiki.auth;

import java.security.Principal;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.TestHttpServletRequest;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiSession;
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
            request.setRoles( new String[]
            { "IT", "Engineering" } );
            m_context = new WikiContext( m_engine, request, p );
        }
        catch( WikiException e )
        {
            assertTrue( "Setup failed", false );
        }

        Principal principal = new WikiPrincipal( "Alice" );
        Role[] roles = new Role[]
        { Role.AUTHENTICATED, Role.ALL };
        WikiSession session = buildSession( m_context, principal, roles );

        // Test build-in role membership
        assertFalse( "Alice ADMIN", m_auth.hasRoleOrPrincipal( m_context, Role.ADMIN ) );
        assertTrue( "Alice ALL", m_auth.hasRoleOrPrincipal( m_context, Role.ALL ) );
        assertFalse( "Alice ANONYMOUS", m_auth.hasRoleOrPrincipal( m_context, Role.ANONYMOUS ) );
        assertFalse( "Alice ASSERTED", m_auth.hasRoleOrPrincipal( m_context, Role.ASSERTED ) );
        assertTrue( "Alice AUTHENTICATED", m_auth.hasRoleOrPrincipal( m_context, Role.AUTHENTICATED ) );
        assertTrue( "Alice IT", m_auth.hasRoleOrPrincipal( m_context, new Role( "IT" ) ) );
        assertTrue( "Alice Engineering", m_auth.hasRoleOrPrincipal( m_context, new Role( "Engineering" ) ) );
        assertFalse( "Alice Finance", m_auth.hasRoleOrPrincipal( m_context, new Role( "Finance" ) ) );

        // Test group membership
        GroupManager groupMgr = m_engine.getGroupManager();
        Group fooGroup = new DefaultGroup( "Foo" );
        groupMgr.add( fooGroup );
        Group barGroup = new DefaultGroup( "Bar" );
        barGroup.add( principal );
        groupMgr.add( barGroup );
        assertFalse( "Alice in Foo", m_auth.hasRoleOrPrincipal( m_context, fooGroup ) );
        assertTrue( "Alice in Bar", m_auth.hasRoleOrPrincipal( m_context, barGroup ) );
        
        // Test user principal posession
        assertTrue("Alice has Alice", m_auth.hasRoleOrPrincipal( m_context, new WikiPrincipal("Alice")));
        assertFalse("Alice has Bob", m_auth.hasRoleOrPrincipal( m_context, new WikiPrincipal("Bob")));
        
        // Test user principal (non-WikiPrincipal) posession
        assertTrue("Alice has Alice", m_auth.hasRoleOrPrincipal( m_context, new TestPrincipal("Alice")));
        assertFalse("Alice has Bob", m_auth.hasRoleOrPrincipal( m_context, new TestPrincipal("Bob")));
        
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
        Subject s = new Subject();
        s.getPrincipals().add( Role.ANONYMOUS );
        assertTrue( "Anonymous view",     m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertFalse( "Anonymous edit",    m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Anonymous comment",  m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertFalse( "Anonymous upload",  m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertFalse( "Anonymous rename",  m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Anonymous delete",  m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertTrue( "Anonymous register", m_auth.checkStaticPermission( s, WikiPermission.REGISTER ) );
        assertFalse( "Anonymous pages",   m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertFalse( "Anonymous groups",  m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );
        
        s = new Subject();
        s.getPrincipals().add( Role.ASSERTED );
        assertTrue( "Asserted view",     m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Asserted edit",     m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Asserted comment",  m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertTrue( "Asserted upload",   m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertFalse( "Asserted rename",  m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Asserted delete",  m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertFalse( "Asserted register",m_auth.checkStaticPermission( s, WikiPermission.REGISTER ) );
        assertTrue( "Asserted pages",    m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertFalse( "Asserted groups",  m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );
        
        s = new Subject();
        s.getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "Authenticated view",     m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Authenticated edit",     m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Authenticated comment",  m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertTrue( "Authenticated upload",   m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertTrue( "Authenticated rename",   m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertFalse( "Authenticated delete",  m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertFalse( "Authenticated register",m_auth.checkStaticPermission( s, WikiPermission.REGISTER ) );
        assertTrue( "Authenticated pages",    m_auth.checkStaticPermission( s, WikiPermission.CREATE_PAGES ) );
        assertTrue( "Authenticated groups",   m_auth.checkStaticPermission( s, WikiPermission.CREATE_GROUPS ) );
        
        s = new Subject();
        s.getPrincipals().add( Role.ADMIN );
        assertTrue( "Admin view",     m_auth.checkStaticPermission( s, PagePermission.VIEW ) );
        assertTrue( "Admin edit",     m_auth.checkStaticPermission( s, PagePermission.EDIT ) );
        assertTrue( "Admin comment",  m_auth.checkStaticPermission( s, PagePermission.COMMENT ) );
        assertTrue( "Admin upload",   m_auth.checkStaticPermission( s, PagePermission.UPLOAD ) );
        assertTrue( "Admin rename",   m_auth.checkStaticPermission( s, PagePermission.RENAME ) );
        assertTrue( "Admin delete",   m_auth.checkStaticPermission( s, PagePermission.DELETE ) );
        assertFalse( "Admin register",m_auth.checkStaticPermission( s, WikiPermission.REGISTER ) );
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
    
    private WikiSession buildSession( WikiContext context, Principal user, Role[] roles )
    {
        WikiSession session = WikiSession.GUEST_SESSION;
        Set principals = session.getSubject().getPrincipals();
        principals.clear();
        principals.add( user );
        for( int i = 0; i < roles.length; i++ )
        {
            principals.add( roles[i] );
        }
        context.setWikiSession( session );
        return session;
    }

    /**
     * Any principal strings that have same names as built-in
     * roles should resolve as built-in roles!
     */
    public void testResolveBuiltInRoles()
    {
        Principal principal = Role.ADMIN;
        assertEquals( principal, m_auth.resolvePrincipal( "Admin" ) );
        principal = Role.AUTHENTICATED;
        assertEquals( principal, m_auth.resolvePrincipal( "Authenticated" ) );
        principal = Role.ASSERTED;
        assertEquals( principal, m_auth.resolvePrincipal( "Asserted" ) );
        principal = Role.ALL;
        assertEquals( principal, m_auth.resolvePrincipal( "All" ) );
        principal = Role.ADMIN;
        assertEquals( principal, m_auth.resolvePrincipal( "Admin" ) );
        
        // This should NOT resolve
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
        WikiContext context = new WikiContext( m_engine, p );
        WikiSession session = WikiSession.GUEST_SESSION;
        context.setWikiSession( session );

        AuthorizationManager mgr = m_engine.getAuthorizationManager();
        
        // Charlie is anonymous
        Principal principal = new WikiPrincipal( "Charlie");
        session.getSubject().getPrincipals().clear();
        session.getSubject().getPrincipals().add( principal );
        session.getSubject().getPrincipals().add( Role.ANONYMOUS );
        assertTrue( "Charlie view", 
                    mgr.checkPermission( context,
                                         new PagePermission( "TestPage", "view" ) ) );
        assertFalse( "Charlie edit", 
                     mgr.checkPermission( context,
                                          new PagePermission( "TestPage", "edit" ) ) );

        // Bob is logged in
        principal = new WikiPrincipal( "Bob");
        session.getSubject().getPrincipals().clear();
        session.getSubject().getPrincipals().add( principal );
        session.getSubject().getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "Bob view", mgr.checkPermission( context,
                new PagePermission( "TestPage", "view" ) ) );
        assertTrue( "Bob edit", mgr.checkPermission( context,
                new PagePermission( "TestPage", "edit" ) ) );

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

    public void testPrincipalAclPermissions() throws Exception
    {
        String src = "[{ALLOW edit Foo}] ";
        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage( "Test" );
        m_context = new WikiContext( m_engine, p );
        m_session = WikiSession.GUEST_SESSION;
        m_context.setWikiSession( m_session );
        System.out.println( printPermissions( p ) );

        // Foo is in the ACL and can read
        Principal principal = new WikiPrincipal( "Foo" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "Foo view Test", m_auth.checkPermission( m_context, new PagePermission( "Test", "view" ) ) );
        assertFalse( "Foo !view all", m_auth.checkPermission( m_context, PagePermission.VIEW ) );
        assertTrue( "Foo edit Test", m_auth.checkPermission( m_context, new PagePermission( "Test", "edit" ) ) );
        assertFalse( "Foo !edit all", m_auth.checkPermission( m_context, PagePermission.EDIT ) );

        // Bar is not in the ACL, so he can't read or edit
        principal = new WikiPrincipal( "Bar" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.ANONYMOUS );
        assertFalse( "Bar view Test", m_auth.checkPermission( m_context, new PagePermission( "Test", "view" ) ) );
        assertFalse( "Bar !view all", m_auth.checkPermission( m_context, new PagePermission( "Test", "edit" ) ) );
        
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

    public void testRoleAclPermissions() throws Exception
    {
        String src = "[{ALLOW edit Authenticated}] ";
        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage( "Test" );
        m_context = new WikiContext( m_engine, p );
        m_session = WikiSession.GUEST_SESSION;
        m_context.setWikiSession( m_session );
        System.out.println( printPermissions( p ) );

        // Authenticated is in the ACL and can view and edit
        Principal principal = new WikiPrincipal( "Foo" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "Foo view Test", m_auth.checkPermission( m_context, new PagePermission( "Test", "view" ) ) );
        assertFalse( "Foo !view all", m_auth.checkPermission( m_context, PagePermission.VIEW ) );
        assertTrue( "Foo edit Test", m_auth.checkPermission( m_context, new PagePermission( "Test", "edit" ) ) );
        assertFalse( "Foo !edit all", m_auth.checkPermission( m_context, PagePermission.EDIT ) );

        // Bar is not authenticated, so he can't read or edit
        principal = new WikiPrincipal( "Bar" );
        m_session.getSubject().getPrincipals().clear();
        m_session.getSubject().getPrincipals().add( principal );
        m_session.getSubject().getPrincipals().add( Role.ANONYMOUS );
        assertFalse( "Bar view Test", m_auth.checkPermission( m_context, new PagePermission( "Test", "view" ) ) );
        assertFalse( "Bar !view all", m_auth.checkPermission( m_context, new PagePermission( "Test", "edit" ) ) );
        
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